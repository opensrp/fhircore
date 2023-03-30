/*
 * Copyright 2021-2023 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.data.local.register

import ca.uhn.fhir.rest.gclient.DateClientParam
import ca.uhn.fhir.rest.gclient.NumberClientParam
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.has
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.NestedSearchConfig
import org.smartregister.fhircore.engine.domain.model.RelatedResourceCount
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.SortConfig
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.engine.util.pmap
import timber.log.Timber

class RegisterRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DefaultDispatcherProvider,
  override val sharedPreferencesHelper: SharedPreferencesHelper,
  override val configurationRegistry: ConfigurationRegistry,
  override val configService: ConfigService,
  val fhirPathDataExtractor: FhirPathDataExtractor
) :
  Repository,
  DefaultRepository(
    fhirEngine = fhirEngine,
    dispatcherProvider = dispatcherProvider,
    sharedPreferencesHelper = sharedPreferencesHelper,
    configurationRegistry = configurationRegistry,
    configService = configService
  ) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    registerId: String,
    paramsMap: Map<String, String>?
  ): List<RepositoryResourceData> {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val baseResourceConfig = registerConfiguration.fhirResource.baseResource
    val relatedResourcesConfig = registerConfiguration.fhirResource.relatedResources
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val baseResourceType = baseResourceClass.newInstance().resourceType
    val secondaryResourceConfig = registerConfiguration.secondaryResources
    val secondaryResourceData = LinkedList<RepositoryResourceData>()

    // retrieve secondary ResourceData if secondaryResources are configured
    if (!secondaryResourceConfig.isNullOrEmpty()) {
      secondaryResourceData.addAll(retrieveSecondaryResources(secondaryResourceConfig))
    }

    val baseResources: List<Resource> =
      withContext(dispatcherProvider.io()) {
        searchResource(
          baseResourceClass = baseResourceClass,
          nestedSearchResources = baseResourceConfig.nestedSearchResources,
          dataQueries = baseResourceConfig.dataQueries,
          sortConfigs = baseResourceConfig.sortConfigs,
          currentPage = currentPage,
          pageSize = registerConfiguration.pageSize
        )
      }

    // Retrieve data for each of the configured related resources
    // Also retrieve data for nested related resources for each of the related resource
    return baseResources.map { baseResource: Resource ->
      val currentRelatedResources = LinkedList<RepositoryResourceData>()

      relatedResourcesConfig.forEach { resourceConfig: ResourceConfig ->
        val relatedResources =
          withContext(dispatcherProvider.io()) {
            searchRelatedResources(
              resourceConfig = resourceConfig,
              baseResourceType = baseResourceType,
              baseResource = baseResource,
              fhirPathExpression = resourceConfig.fhirPathExpression
            )
          }
        currentRelatedResources.addAll(relatedResources)
      }

      // Include secondary resourceData in each row if secondaryResources are configured
      RepositoryResourceData(
        id = baseResourceConfig.id ?: baseResourceType.name,
        queryResult =
          RepositoryResourceData.QueryResult.Search(
            resource = baseResource,
            relatedResources = currentRelatedResources.apply { addAll(secondaryResourceData) }
          )
      )
    }
  }

  private suspend fun searchRelatedResources(
    resourceConfig: ResourceConfig,
    baseResourceType: ResourceType,
    baseResource: Resource,
    fhirPathExpression: String?
  ): LinkedList<RepositoryResourceData> {

    val relatedResourceClass = resourceConfig.resource.resourceClassType()
    val relatedResourceType = relatedResourceClass.newInstance().resourceType
    val relatedResourcesData = LinkedList<RepositoryResourceData>()
    if (fhirPathExpression.isNullOrEmpty()) {
      val search =
        Search(type = relatedResourceType).apply {
          filterByResourceTypeId(
            ReferenceClientParam(resourceConfig.searchParameter),
            baseResourceType,
            baseResource.logicalId
          )
          resourceConfig.dataQueries?.forEach { filterBy(it) }
          applyNestedSearchFilters(resourceConfig.nestedSearchResources)
        }
      if (resourceConfig.resultAsCount) {
        val count = fhirEngine.count(search)
        relatedResourcesData.addLast(
          RepositoryResourceData(
            id = resourceConfig.id ?: relatedResourceType.name,
            queryResult =
              RepositoryResourceData.QueryResult.Count(
                resourceType = relatedResourceType,
                relatedResourceCount =
                  RelatedResourceCount(parentResourceId = baseResource.logicalId, count = count)
              )
          )
        )
      } else {
        val relatedResourceSearch = search.apply { sort(resourceConfig.sortConfigs) }
        fhirEngine.search<Resource>(relatedResourceSearch).forEach { resource ->
          relatedResourcesData.addLast(
            RepositoryResourceData(
              id = resourceConfig.id ?: resource.resourceType.name,
              queryResult = RepositoryResourceData.QueryResult.Search(resource = resource)
            )
          )
        }
        postProcessRelatedResourcesData(resourceConfig.relatedResources, relatedResourcesData)
      }
    } else {
      fhirPathDataExtractor
        .extractData(baseResource, fhirPathExpression)
        .takeWhile { it is Reference }
        .pmap {
          try {
            fhirEngine.get(
              resourceConfig.resource.resourceClassType().newInstance().resourceType,
              (it as Reference).extractId()
            )
          } catch (exception: ResourceNotFoundException) {
            Timber.e(exception)
            null
          }
        }
        .forEach { resource ->
          resource?.let {
            relatedResourcesData.addLast(
              RepositoryResourceData(
                id = resourceConfig.id ?: resource.resourceType.name,
                queryResult = RepositoryResourceData.QueryResult.Search(resource = resource)
              )
            )
          }
          postProcessRelatedResourcesData(resourceConfig.relatedResources, relatedResourcesData)
        }
    }
    return relatedResourcesData
  }

  private suspend fun postProcessRelatedResourcesData(
    relatedResources: List<ResourceConfig>,
    relatedResourcesData: LinkedList<RepositoryResourceData>
  ) {

    if (relatedResourcesData.isEmpty()) return

    relatedResources.forEach {
      val repositoryResourceData =
        relatedResourcesData.last.queryResult as RepositoryResourceData.QueryResult.Search
      val searchRelatedResources =
        searchRelatedResources(
          resourceConfig = it,
          baseResourceType = repositoryResourceData.resource.resourceType,
          baseResource = repositoryResourceData.resource,
          fhirPathExpression = it.fhirPathExpression
        )

      repositoryResourceData.relatedResources.addAll(searchRelatedResources)
    }
  }

  private suspend fun searchResource(
    baseResourceClass: Class<out Resource>,
    nestedSearchResources: List<NestedSearchConfig>?,
    dataQueries: List<DataQuery>?,
    sortConfigs: List<SortConfig>,
    currentPage: Int? = null,
    pageSize: Int? = null
  ): List<Resource> {
    val resourceType = baseResourceClass.newInstance().resourceType
    val search =
      Search(type = resourceType).apply {
        dataQueries?.forEach { filterBy(it) }
        // For patient return only active members
        if (resourceType == ResourceType.Patient) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
        applyNestedSearchFilters(nestedSearchResources)
        sort(sortConfigs)
        if (currentPage != null && pageSize != null) {
          count = pageSize
          from = currentPage * pageSize
        }
      }
    return fhirEngine.search(search)
  }

  private fun Search.applyNestedSearchFilters(nestedSearchResources: List<NestedSearchConfig>?) {
    nestedSearchResources?.forEach {
      has(it.resourceType, ReferenceClientParam((it.referenceParam))) {
        it.dataQueries?.forEach { dataQuery -> filterBy(dataQuery) }
      }
    }
  }

  private fun Search.sort(sortConfigs: List<SortConfig>) {
    sortConfigs.forEach { sortConfig ->
      when (sortConfig.dataType) {
        DataType.INTEGER -> sort(NumberClientParam(sortConfig.paramName), sortConfig.order)
        DataType.DATE -> sort(DateClientParam(sortConfig.paramName), sortConfig.order)
        DataType.STRING -> sort(StringClientParam(sortConfig.paramName), sortConfig.order)
        else -> {
          /*Unsupported data type*/
        }
      }
    }
  }

  /** Count register data for the provided [registerId]. Use the configured base resource filters */
  override suspend fun countRegisterData(
    registerId: String,
    paramsMap: Map<String, String>?
  ): Long {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val baseResourceConfig = registerConfiguration.fhirResource.baseResource
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val resourceType = baseResourceClass.newInstance().resourceType

    val search =
      Search(resourceType).apply {
        baseResourceConfig.dataQueries?.forEach { filterBy(it) }
        // For patient return only active members count
        if (resourceType == ResourceType.Patient) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
        applyNestedSearchFilters(baseResourceConfig.nestedSearchResources)
      }

    return fhirEngine.count(search)
  }

  override suspend fun loadProfileData(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsList: Array<ActionParameter>?
  ): RepositoryResourceData {
    val paramsMap: Map<String, String> =
      paramsList
        ?.filter {
          (it.paramType == ActionParameterType.PARAMDATA ||
            it.paramType == ActionParameterType.UPDATE_DATE_ON_EDIT) && it.value.isNotEmpty()
        }
        ?.associate { it.key to it.value }
        ?: emptyMap()
    val profileConfiguration =
      configurationRegistry.retrieveConfiguration<ProfileConfiguration>(
        ConfigType.Profile,
        profileId,
        paramsMap
      )
    val resourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
    val baseResourceConfig = resourceConfig.baseResource
    val relatedResourcesConfig = resourceConfig.relatedResources
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val baseResourceType = baseResourceClass.newInstance().resourceType
    val secondaryResourceConfig = profileConfiguration.secondaryResources

    val baseResource: Resource =
      withContext(dispatcherProvider.io()) {
        fhirEngine.get(baseResourceType, resourceId.extractLogicalIdUuid())
      }

    val relatedResources = LinkedList<RepositoryResourceData>()

    relatedResourcesConfig.forEach { config: ResourceConfig ->
      val resources =
        withContext(dispatcherProvider.io()) {
          searchRelatedResources(
            resourceConfig = config,
            baseResourceType = baseResourceType,
            baseResource = baseResource,
            fhirPathExpression = config.fhirPathExpression
          )
        }
      relatedResources.addAll(resources)
    }

    if (!secondaryResourceConfig.isNullOrEmpty()) {
      relatedResources.addAll(retrieveSecondaryResources(secondaryResourceConfig))
    }

    return RepositoryResourceData(
      id = baseResourceConfig.id ?: baseResourceType.name,
      queryResult =
        RepositoryResourceData.QueryResult.Search(
          resource = baseResource,
          relatedResources = relatedResources
        )
    )
  }

  private suspend fun retrieveSecondaryResources(
    resourceConfigList: List<FhirResourceConfig>
  ): LinkedList<RepositoryResourceData> {
    val repositoryResourceData = LinkedList<RepositoryResourceData>()

    resourceConfigList.map { fhirResourceConfig: FhirResourceConfig ->
      val baseResources: List<Resource> =
        withContext(dispatcherProvider.io()) {
          searchResource(
            baseResourceClass = fhirResourceConfig.baseResource.resource.resourceClassType(),
            dataQueries = fhirResourceConfig.baseResource.dataQueries,
            sortConfigs = fhirResourceConfig.baseResource.sortConfigs,
            nestedSearchResources = fhirResourceConfig.baseResource.nestedSearchResources,
          )
        }

      baseResources.map { baseResource: Resource ->
        val baseRelatedResourceList = LinkedList<RepositoryResourceData>()
        fhirResourceConfig.relatedResources.forEach { resourceConfig: ResourceConfig ->
          val currentRelatedResources =
            withContext(dispatcherProvider.io()) {
              searchRelatedResources(
                resourceConfig = resourceConfig,
                baseResourceType = baseResource.resourceType,
                baseResource = baseResource,
                fhirPathExpression = resourceConfig.fhirPathExpression
              )
            }
          baseRelatedResourceList.addAll(currentRelatedResources)
        }
        repositoryResourceData.add(
          RepositoryResourceData(
            id = fhirResourceConfig.baseResource.id,
            queryResult =
              RepositoryResourceData.QueryResult.Search(
                resource = baseResource,
                relatedResources = baseRelatedResourceList
              )
          )
        )
      }
    }

    return repositoryResourceData
  }

  fun retrieveRegisterConfiguration(
    registerId: String,
    paramsMap: Map<String, String>?
  ): RegisterConfiguration =
    configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId, paramsMap)

  companion object {
    const val ACTIVE = "active"
  }
}
