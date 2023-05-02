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
    return searchResourcesRecursively(
      fhirResourceConfig = registerConfiguration.fhirResource,
      currentPage = currentPage,
      pageSize = registerConfiguration.pageSize
    )
  }

  private suspend fun searchResourcesRecursively(
    fhirResourceConfig: FhirResourceConfig,
    currentPage: Int? = null,
    pageSize: Int? = null
  ): List<RepositoryResourceData> {
    val baseResourceConfig = fhirResourceConfig.baseResource
    val relatedResourcesConfig = fhirResourceConfig.relatedResources
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val baseResourceType = baseResourceClass.newInstance().resourceType

    val search =
      Search(type = baseResourceType).apply {
        baseResourceConfig.dataQueries?.forEach { filterBy(it) }
        applyNestedSearchFilters(baseResourceConfig.nestedSearchResources)
        // Filter only active resources
        if (baseResourceType in filterActiveResources) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
        sort(baseResourceConfig.sortConfigs)
        if (currentPage != null && pageSize != null) {
          count = pageSize
          from = currentPage * pageSize
        }
      }

    val baseFhirResources =
      withContext(dispatcherProvider.io()) { fhirEngine.search<Resource>(search) }

    return baseFhirResources.map { baseFhirResource ->
      // TODO add secondary resources as related resources
      RepositoryResourceData.Search(
        rulesFactsMapId = baseResourceConfig.id ?: baseResourceType.name,
        resource = baseFhirResource,
        relatedResources =
          withContext(dispatcherProvider.io()) {
            retrieveRelatedResources(baseFhirResource, relatedResourcesConfig)
          }
      )
    }
  }

  private suspend fun retrieveRelatedResources(
    baseResource: Resource,
    relatedResourcesConfigs: List<ResourceConfig>?
  ): Map<String, LinkedList<RepositoryResourceData>> {

    // The key for this map is the same as the one used by the Rules Fact map
    // Can be provided via ResourceConfig.id property otherwise defaults to the ResourceType
    val finalResultMap = mutableMapOf<String, LinkedList<RepositoryResourceData>>()

    val nonRevIncludedRelatedResourcesConfigs =
      relatedResourcesConfigs?.revIncludeRelatedResourceConfigs(true)

    nonRevIncludedRelatedResourcesConfigs?.forEach { resourceConfig ->
      searchWithForwardInclude(resourceConfig, baseResource, finalResultMap)

      // Return counts for the related resources using the count API
      if (resourceConfig.resultAsCount && !resourceConfig.searchParameter.isNullOrEmpty()) {
        val relatedResourceClass = resourceConfig.resource.resourceClassType()
        val relatedResourceType = relatedResourceClass.newInstance().resourceType
        val search =
          Search(type = relatedResourceType).apply {
            filterByResourceTypeId(
              ReferenceClientParam(resourceConfig.searchParameter),
              baseResource.resourceType,
              baseResource.logicalId
            )
            resourceConfig.dataQueries?.forEach { filterBy(it) }
            applyNestedSearchFilters(resourceConfig.nestedSearchResources)
          }
        val count = fhirEngine.count(search)
        finalResultMap[resourceConfig.id ?: resourceConfig.resource] =
          LinkedList<RepositoryResourceData>().apply {
            addLast(
              RepositoryResourceData.Count(
                resourceType = relatedResourceType,
                relatedResourceCount =
                  RelatedResourceCount(parentResourceId = baseResource.logicalId, count = count)
              )
            )
          }
      }
    }

    searchWithRevInclude(relatedResourcesConfigs, baseResource, finalResultMap)
    return finalResultMap
  }

  private suspend fun searchWithForwardInclude(
    resourceConfig: ResourceConfig,
    baseResource: Resource,
    finalResultMap: MutableMap<String, LinkedList<RepositoryResourceData>>
  ) {
    if (!resourceConfig.fhirPathExpression.isNullOrEmpty()) {
      val queryResultLinkedList = LinkedList<RepositoryResourceData>()
      fhirPathDataExtractor
        .extractData(baseResource, resourceConfig.fhirPathExpression)
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
        .forEach { thisResource ->
          thisResource?.let {
            queryResultLinkedList.addLast(
              RepositoryResourceData.Search(
                resource = thisResource,
                relatedResources =
                  retrieveRelatedResources(
                    baseResource = thisResource,
                    relatedResourcesConfigs = resourceConfig.relatedResources
                  )
              )
            )
          }
        }
      finalResultMap[resourceConfig.id ?: resourceConfig.resource] = queryResultLinkedList
    }
  }

  private suspend fun searchWithRevInclude(
    relatedResourcesConfigs: List<ResourceConfig>?,
    baseResource: Resource,
    finalResultMap: MutableMap<String, LinkedList<RepositoryResourceData>>
  ) {
    // Get configurations of related resources to be reverse included
    val revIncludeRelatedResourcesConfigsMap: Map<String, List<ResourceConfig>>? =
      relatedResourcesConfigs?.revIncludeRelatedResourceConfigs(false)?.groupBy { it.resource }

    if (!revIncludeRelatedResourcesConfigsMap.isNullOrEmpty()) {
      val revIncludeSearch =
        Search(baseResource.resourceType).apply {
          filter(Resource.RES_ID, { value = of(baseResource.logicalId) })
        }
      revIncludeRelatedResourcesConfigsMap.values.flatten().forEach { resourceConfig ->
        val resourceType = resourceConfig.resource.resourceClassType().newInstance().resourceType
        revIncludeSearch.apply {
          revInclude(resourceType, ReferenceClientParam(resourceConfig.searchParameter))
        }
      }

      val revIncludeSearchResult: Map<Resource, Map<ResourceType, List<Resource>>> =
        fhirEngine.searchWithRevInclude(revIncludeSearch)

      revIncludeSearchResult.values.forEach { revIncludedResource ->
        val revIncludedResourcesMap =
          revIncludedResource
            .mapValues { entry ->
              entry.value.mapTo(LinkedList<RepositoryResourceData>()) { resource ->
                RepositoryResourceData.Search(
                  resource = resource,
                  relatedResources =
                    retrieveRelatedResources(
                      baseResource = resource,
                      relatedResourcesConfigs =
                        revIncludeRelatedResourcesConfigsMap[entry.key.name]
                          .nestedRelatedResourceConfigs()
                    )
                )
              }
            }
            .mapKeys {
              // Use the unique resourceConfig.id otherwise default to resourceType
              if (revIncludeRelatedResourcesConfigsMap.containsKey(it.key.name)) {
                revIncludeRelatedResourcesConfigsMap[it.key.name]?.firstOrNull()?.id ?: it.key.name
              } else it.key.name
            }

        finalResultMap.putAll(revIncludedResourcesMap)
      }
    }
  }

  private fun List<ResourceConfig>?.nestedRelatedResourceConfigs(): List<ResourceConfig>? =
    this
      ?.filter { thisResourceConfig -> thisResourceConfig.relatedResources.isNotEmpty() }
      ?.flatMap { it.relatedResources }

  private fun List<ResourceConfig>.revIncludeRelatedResourceConfigs(inverse: Boolean) =
    if (inverse)
      this.filter {
        !it.fhirPathExpression.isNullOrEmpty() ||
          (!it.searchParameter.isNullOrEmpty() && it.resultAsCount)
      }
    else this.filter { !it.searchParameter.isNullOrEmpty() && !it.resultAsCount }

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
        // Filter only active resources
        if (resourceType in filterActiveResources) {
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
        ?.asSequence()
        ?.filter {
          (it.paramType == ActionParameterType.PARAMDATA ||
            it.paramType == ActionParameterType.UPDATE_DATE_ON_EDIT) && it.value.isNotEmpty()
        }
        ?.associate { it.key to it.value }
        ?: emptyMap()

    val profileConfiguration =
      configurationRegistry.retrieveConfiguration<ProfileConfiguration>(
        configType = ConfigType.Profile,
        configId = profileId,
        paramsMap = paramsMap
      )
    val resourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
    val baseResourceConfig = resourceConfig.baseResource
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val baseResourceType = baseResourceClass.newInstance().resourceType

    val baseResource: Resource =
      withContext(dispatcherProvider.io()) {
        fhirEngine.get(baseResourceType, resourceId.extractLogicalIdUuid())
      }

    return RepositoryResourceData.Search(
      rulesFactsMapId = baseResourceConfig.id ?: baseResourceType.name,
      resource = baseResource,
      // TODO Add secondary resources as related resources
      relatedResources =
        withContext(dispatcherProvider.io()) {
          retrieveRelatedResources(baseResource, resourceConfig.relatedResources)
        }
    )
  }

  fun retrieveRegisterConfiguration(
    registerId: String,
    paramsMap: Map<String, String>?
  ): RegisterConfiguration =
    configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId, paramsMap)

  companion object {
    private val filterActiveResources = listOf(ResourceType.Patient, ResourceType.Group)
    const val ACTIVE = "active"
  }
}
