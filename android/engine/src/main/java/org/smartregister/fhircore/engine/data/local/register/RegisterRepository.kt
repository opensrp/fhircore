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
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.search.has
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Enumerations.DataType
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
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import timber.log.Timber

class RegisterRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DispatcherProvider,
  override val sharedPreferencesHelper: SharedPreferencesHelper,
  override val configurationRegistry: ConfigurationRegistry,
  override val configService: ConfigService
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
      registerConfiguration.secondaryResources,
      currentPage = currentPage,
      pageSize = registerConfiguration.pageSize
    )
  }

  private suspend fun searchResourcesRecursively(
    fhirResourceConfig: FhirResourceConfig,
    secondaryResourceConfigs: List<FhirResourceConfig>?,
    currentPage: Int? = null,
    pageSize: Int? = null
  ): List<RepositoryResourceData> {
    val baseResourceConfig = fhirResourceConfig.baseResource
    val relatedResourcesConfig = fhirResourceConfig.relatedResources

    val search =
      Search(type = baseResourceConfig.resource).apply {
        baseResourceConfig.dataQueries?.forEach { filterBy(it) }
        applyNestedSearchFilters(baseResourceConfig.nestedSearchResources)
        // Filter only active resources
        if (baseResourceConfig.resource in filterActiveResources) {
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
      val retrievedRelatedResources =
        withContext(dispatcherProvider.io()) {
          retrieveRelatedResources(
            resources = listOf(baseFhirResource),
            relatedResourcesConfigs = relatedResourcesConfig,
            relatedResourceWrapper = RelatedResourceWrapper()
          )
        }
      RepositoryResourceData(
        resourceRulesEngineFactId = baseResourceConfig.id ?: baseResourceConfig.resource.name,
        resource = baseFhirResource,
        relatedResourcesMap = retrievedRelatedResources.relatedResourceMap,
        relatedResourcesCountMap = retrievedRelatedResources.relatedResourceCountMap,
        secondaryRepositoryResourceData =
          withContext(dispatcherProvider.io()) {
            secondaryResourceConfigs.retrieveSecondaryRepositoryResourceData()
          }
      )
    }
  }

  /** This function fetches other resources that are not linked to the base/primary resource. */
  private suspend fun List<FhirResourceConfig>?.retrieveSecondaryRepositoryResourceData():
    LinkedList<RepositoryResourceData> {
    val secondaryRepositoryResourceDataLinkedList = LinkedList<RepositoryResourceData>()
    this?.forEach {
      secondaryRepositoryResourceDataLinkedList.addAll(
        searchResourcesRecursively(fhirResourceConfig = it, secondaryResourceConfigs = null)
      )
    }
    return secondaryRepositoryResourceDataLinkedList
  }

  private suspend fun retrieveRelatedResources(
    resources: List<Resource>,
    relatedResourcesConfigs: List<ResourceConfig>?,
    relatedResourceWrapper: RelatedResourceWrapper,
  ): RelatedResourceWrapper {

    // Search with forward include
    searchWithRevInclude(
      isRevInclude = false,
      relatedResourcesConfigs = relatedResourcesConfigs,
      resources = resources,
      relatedResourceWrapper = relatedResourceWrapper
    )

    val countResourceConfigs = relatedResourcesConfigs?.filter { it.resultAsCount }
    countResourceConfigs?.forEach { resourceConfig ->
      if (resourceConfig.searchParameter.isNullOrEmpty()) {
        Timber.e("Search parameter require to perform count query. Current config: $resourceConfig")
      }

      // Return counts for the related resources using the count API
      if (resourceConfig.resultAsCount && !resourceConfig.searchParameter.isNullOrEmpty()) {
        val relatedResourceCountLinkedList = LinkedList<RelatedResourceCount>()
        resources.forEach { baseResource ->
          val search =
            Search(type = resourceConfig.resource).apply {
              filterByResourceTypeId(
                ReferenceClientParam(resourceConfig.searchParameter),
                baseResource.resourceType,
                baseResource.logicalId
              )
              resourceConfig.dataQueries?.forEach { filterBy(it) }
              applyNestedSearchFilters(resourceConfig.nestedSearchResources)
            }
          val count = fhirEngine.count(search)
          relatedResourceCountLinkedList.add(
            RelatedResourceCount(
              relatedResourceType = resourceConfig.resource,
              parentResourceId = baseResource.logicalId,
              count = count
            )
          )
        }

        // Add to the count query result to map
        relatedResourceWrapper.relatedResourceCountMap[
          resourceConfig.id ?: resourceConfig.resource.name] = relatedResourceCountLinkedList
      }
    }

    searchWithRevInclude(
      isRevInclude = true,
      relatedResourcesConfigs = relatedResourcesConfigs,
      resources = resources,
      relatedResourceWrapper = relatedResourceWrapper
    )

    return relatedResourceWrapper
  }

  /**
   * If [isRevInclude] is set to false, the forward include search API will be used; otherwise
   * reverse include is used to retrieve related resources. [relatedResourceWrapper] is a data class
   * that wraps the maps used to store Search Query results. The [relatedResourcesConfigs]
   * configures which resources to load.
   */
  private suspend fun searchWithRevInclude(
    isRevInclude: Boolean,
    relatedResourcesConfigs: List<ResourceConfig>?,
    resources: List<Resource>,
    relatedResourceWrapper: RelatedResourceWrapper
  ) {
    val revIncludeRelatedResourceConfigs =
      relatedResourcesConfigs?.revIncludeRelatedResourceConfigs(isRevInclude)

    val revIncludeRelatedResourcesConfigsMap: Map<String, List<ResourceConfig>>? =
      revIncludeRelatedResourceConfigs?.groupBy { it.resource.name }

    if (!revIncludeRelatedResourcesConfigsMap.isNullOrEmpty()) {
      if (resources.isEmpty()) return

      val firstResourceType = resources.first().resourceType
      val search =
        Search(firstResourceType).apply {
          val filters =
            resources.map {
              val apply: TokenParamFilterCriterion.() -> Unit = { value = of(it.logicalId) }
              apply
            }
          filter(Resource.RES_ID, *filters.toTypedArray())
        }

      revIncludeRelatedResourceConfigs.forEach { resourceConfig ->
        search.apply {
          if (isRevInclude) {
            revInclude(
              resourceConfig.resource,
              ReferenceClientParam(resourceConfig.searchParameter)
            )
          } else {
            include(ReferenceClientParam(resourceConfig.searchParameter), resourceConfig.resource)
          }
        }
      }

      val searchResult: Map<Resource, Map<ResourceType, List<Resource>>> =
        fhirEngine.searchWithRevInclude(isRevInclude, search)

      searchResult.values.forEach { theRelatedResourcesMap: Map<ResourceType, List<Resource>> ->
        theRelatedResourcesMap.forEach { entry ->
          val key =
            if (revIncludeRelatedResourcesConfigsMap.containsKey(entry.key.name)) {
              revIncludeRelatedResourcesConfigsMap[entry.key.name]?.firstOrNull()?.id
                ?: entry.key.name
            } else entry.key.name

          // Append to existing list if key exists
          relatedResourceWrapper.relatedResourceMap[key] =
            relatedResourceWrapper
              .relatedResourceMap
              .getOrPut(key) { LinkedList() }
              .plus(entry.value)

          relatedResourcesConfigs.forEach { resourceConfig ->
            if (resourceConfig.relatedResources.isNotEmpty())
              retrieveRelatedResources(
                resources = entry.value,
                relatedResourcesConfigs = resourceConfig.relatedResources,
                relatedResourceWrapper = relatedResourceWrapper
              )
          }
        }
      }
    }
  }

  private fun List<ResourceConfig>.revIncludeRelatedResourceConfigs(isRevInclude: Boolean) =
    if (isRevInclude) this.filter { it.isRevInclude && !it.resultAsCount }
    else this.filter { !it.isRevInclude && !it.resultAsCount }

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

    val search =
      Search(baseResourceConfig.resource).apply {
        baseResourceConfig.dataQueries?.forEach { filterBy(it) }
        // Filter only active resources
        if (baseResourceConfig.resource in filterActiveResources) {
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

    val profileConfiguration = retrieveProfileConfiguration(profileId, paramsMap)
    val resourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
    val baseResourceConfig = resourceConfig.baseResource

    val baseResource: Resource =
      withContext(dispatcherProvider.io()) {
        fhirEngine.get(baseResourceConfig.resource, resourceId.extractLogicalIdUuid())
      }

    val retrievedRelatedResources =
      withContext(dispatcherProvider.io()) {
        retrieveRelatedResources(
          resources = listOf(baseResource),
          relatedResourcesConfigs = resourceConfig.relatedResources,
          relatedResourceWrapper = RelatedResourceWrapper()
        )
      }
    return RepositoryResourceData(
      resourceRulesEngineFactId = baseResourceConfig.id ?: baseResourceConfig.resource.name,
      resource = baseResource,
      relatedResourcesMap = retrievedRelatedResources.relatedResourceMap,
      relatedResourcesCountMap = retrievedRelatedResources.relatedResourceCountMap,
      secondaryRepositoryResourceData =
        withContext(dispatcherProvider.io()) {
          profileConfiguration.secondaryResources.retrieveSecondaryRepositoryResourceData()
        }
    )
  }

  fun retrieveProfileConfiguration(profileId: String, paramsMap: Map<String, String>) =
    configurationRegistry.retrieveConfiguration<ProfileConfiguration>(
      configType = ConfigType.Profile,
      configId = profileId,
      paramsMap = paramsMap
    )

  fun retrieveRegisterConfiguration(
    registerId: String,
    paramsMap: Map<String, String>?
  ): RegisterConfiguration =
    configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId, paramsMap)

  /**
   * A wrapper class to hold search results. All related resources are flattened into one Map
   * including the nested related resources as required by the Rules Engine facts.
   */
  private data class RelatedResourceWrapper(
    val relatedResourceMap: MutableMap<String, List<Resource>> = mutableMapOf(),
    val relatedResourceCountMap: MutableMap<String, List<RelatedResourceCount>> = mutableMapOf()
  )

  companion object {
    private val filterActiveResources = listOf(ResourceType.Patient, ResourceType.Group)
    const val ACTIVE = "active"
  }
}
