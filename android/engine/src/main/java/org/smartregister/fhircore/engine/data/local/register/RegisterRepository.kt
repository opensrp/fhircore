/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import com.google.android.fhir.search.include
import com.google.android.fhir.search.revInclude
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.ActiveResourceFilterConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.view.retrieveListProperties
import org.smartregister.fhircore.engine.data.local.ContentCache
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RelatedResourceCount
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.batchedSearch
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

typealias SearchQueryResultQueue =
  ArrayDeque<Triple<List<String>, ResourceConfig, Map<String, String>>>

typealias RelatedResourcesQueue = ArrayDeque<Triple<List<String>, ResourceConfig, String>>

@Singleton
class RegisterRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DispatcherProvider,
  override val sharedPreferencesHelper: SharedPreferencesHelper,
  override val configurationRegistry: ConfigurationRegistry,
  override val configService: ConfigService,
  override val rulesExecutor: RulesExecutor,
  override val fhirPathDataExtractor: FhirPathDataExtractor,
  override val parser: IParser,
  @ApplicationContext override val context: Context,
  override val contentCache: ContentCache,
) :
  Repository,
  DefaultRepository(
    fhirEngine = fhirEngine,
    dispatcherProvider = dispatcherProvider,
    sharedPreferencesHelper = sharedPreferencesHelper,
    configurationRegistry = configurationRegistry,
    configService = configService,
    rulesExecutor = rulesExecutor,
    fhirPathDataExtractor = fhirPathDataExtractor,
    parser = parser,
    context = context,
    contentCache = contentCache,
  ) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    registerId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsMap: Map<String, String>?,
  ): List<ResourceData> {
    val registerDataMap = mutableMapOf<String, RepositoryResourceData>()
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val requiredFhirResourceConfig = fhirResourceConfig ?: registerConfiguration.fhirResource
    val configComputedRuleValues = registerConfiguration.configRules.configRulesComputedValues()

    searchNestedResources(
      baseResourceIds = null,
      fhirResourceConfig = requiredFhirResourceConfig,
      resultsDataMap = registerDataMap,
      configComputedRuleValues = configComputedRuleValues,
      activeResourceFilters = registerConfiguration.activeResourceFilters,
      currentPage = currentPage,
      pageSize = registerConfiguration.pageSize,
    )

    populateSecondaryResources(
      secondaryResources = registerConfiguration.secondaryResources,
      configComputedRuleValues = configComputedRuleValues,
      resultsDataMap = registerDataMap,
    )

    val rules = rulesExecutor.rulesFactory.generateRules(registerConfiguration.registerCard.rules)
    return registerDataMap.values
      .asSequence()
      .map { repositoryResourceData ->
        rulesExecutor.processResourceData(
          repositoryResourceData = repositoryResourceData,
          params = paramsMap,
          rules = rules,
        )
      }
      .toList()
  }

  /** Count register data for the provided [registerId]. Use the configured base resource filters */
  override suspend fun countRegisterData(
    registerId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsMap: Map<String, String>?,
  ): Long {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val fhirResource = fhirResourceConfig ?: registerConfiguration.fhirResource
    val baseResourceConfig = fhirResource.baseResource
    val configComputedRuleValues = registerConfiguration.configRules.configRulesComputedValues()
    val filterByRelatedEntityLocation = registerConfiguration.filterDataByRelatedEntityLocation
    val filterActiveResources = registerConfiguration.activeResourceFilters
    return countResources(
      filterByRelatedEntityLocation = filterByRelatedEntityLocation,
      baseResourceConfig = baseResourceConfig,
      filterActiveResources = filterActiveResources,
      configComputedRuleValues = configComputedRuleValues,
    )
  }

  override suspend fun loadProfileData(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsMap: Map<String, String>?,
  ): ResourceData {
    val profileDataMap = mutableMapOf<String, RepositoryResourceData>()
    val profileConfiguration = retrieveProfileConfiguration(profileId, paramsMap)
    val requiredFhirResourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
    val configComputedRuleValues = profileConfiguration.configRules.configRulesComputedValues()

    searchNestedResources(
      baseResourceIds = listOf(resourceId),
      fhirResourceConfig = requiredFhirResourceConfig,
      resultsDataMap = profileDataMap,
      configComputedRuleValues = configComputedRuleValues,
      activeResourceFilters = null,
      currentPage = null,
      pageSize = null,
    )

    populateSecondaryResources(
      secondaryResources = profileConfiguration.secondaryResources,
      configComputedRuleValues = configComputedRuleValues,
      resultsDataMap = profileDataMap,
    )

    if (profileDataMap.values.isNotEmpty()) { // Expectation is that this is never empty
      val repositoryResourceData = profileDataMap.values.first()
      val listResourceDataMap = mutableStateMapOf<String, SnapshotStateList<ResourceData>>()

      val rules = rulesExecutor.rulesFactory.generateRules(profileConfiguration.rules)
      val resourceData =
        rulesExecutor
          .processResourceData(
            repositoryResourceData = repositoryResourceData,
            params = paramsMap,
            rules = rules,
          )
          .copy(listResourceDataMap = listResourceDataMap)

      profileConfiguration.views.retrieveListProperties().forEach { listProperties ->
        rulesExecutor.processListResourceData(
          listProperties = listProperties,
          relatedResourcesMap = repositoryResourceData.relatedResourcesMap,
          computedValuesMap =
            if (!paramsMap.isNullOrEmpty()) {
              resourceData.computedValuesMap.plus(
                paramsMap.toList(),
              )
            } else resourceData.computedValuesMap,
          listResourceDataStateMap = listResourceDataMap,
        )
      }
      return resourceData
    }
    return ResourceData("", ResourceType.Basic, emptyMap())
  }

  fun retrieveProfileConfiguration(profileId: String, paramsMap: Map<String, String>?) =
    configurationRegistry.retrieveConfiguration<ProfileConfiguration>(
      configType = ConfigType.Profile,
      configId = profileId,
      paramsMap = paramsMap,
    )

  fun retrieveRegisterConfiguration(
    registerId: String,
    paramsMap: Map<String, String>?,
  ): RegisterConfiguration =
    configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId, paramsMap)

  private suspend fun searchNestedResources(
    baseResourceIds: List<String>?,
    fhirResourceConfig: FhirResourceConfig,
    resultsDataMap: MutableMap<String, RepositoryResourceData>,
    configComputedRuleValues: Map<String, Any>,
    activeResourceFilters: List<ActiveResourceFilterConfig>?,
    currentPage: Int?,
    pageSize: Int?,
  ): MutableMap<String, RepositoryResourceData> {
    val searchResults =
      searchResources(
        baseResourceIds = baseResourceIds,
        baseResourceConfig = fhirResourceConfig.baseResource,
        relatedResourcesConfigs = fhirResourceConfig.relatedResources,
        activeResourceFilters = activeResourceFilters,
        configComputedRuleValues = configComputedRuleValues,
        currentPage = currentPage,
        pageSize = pageSize,
      )

    val processedSearchResults =
      handleSearchResults(
        searchResults = searchResults,
        repositoryResourceDataResultMap = resultsDataMap,
        repositoryResourceDataMap = null,
        relatedResourceConfigs = fhirResourceConfig.relatedResources,
        baseResourceConfigId = fhirResourceConfig.baseResource.id,
        configComputedRuleValues = configComputedRuleValues,
      )

    while (processedSearchResults.isNotEmpty()) {
      val (newBaseResourceIds, newResourceConfig, repositoryResourceDataMap) =
        processedSearchResults.removeFirst()
      val newSearchResults =
        searchResources(
          baseResourceIds = newBaseResourceIds,
          baseResourceConfig = newResourceConfig,
          relatedResourcesConfigs = newResourceConfig.relatedResources,
          activeResourceFilters = activeResourceFilters,
          configComputedRuleValues = configComputedRuleValues,
          currentPage = null,
          pageSize = null,
        )

      val newProcessedSearchResults =
        handleSearchResults(
          searchResults = newSearchResults,
          repositoryResourceDataResultMap = resultsDataMap,
          repositoryResourceDataMap = repositoryResourceDataMap,
          relatedResourceConfigs = newResourceConfig.relatedResources,
          baseResourceConfigId = fhirResourceConfig.baseResource.id,
          configComputedRuleValues = configComputedRuleValues,
        )
      processedSearchResults.addAll(newProcessedSearchResults)
    }
    return resultsDataMap
  }

  suspend fun searchResources(
    baseResourceIds: List<String>?,
    baseResourceConfig: ResourceConfig,
    relatedResourcesConfigs: List<ResourceConfig>,
    activeResourceFilters: List<ActiveResourceFilterConfig>?,
    configComputedRuleValues: Map<String, Any>,
    currentPage: Int?,
    pageSize: Int?,
  ): List<SearchResult<Resource>> {
    val search =
      createSearch(
        baseResourceIds = baseResourceIds,
        baseResourceConfig = baseResourceConfig,
        filterActiveResources = activeResourceFilters,
        configComputedRuleValues = configComputedRuleValues,
        currentPage = currentPage,
        count = pageSize,
      )

    val (forwardIncludes, reverseIncludes) =
      relatedResourcesConfigs.filter { !it.resultAsCount }.partition { !it.isRevInclude }

    search.apply {
      reverseIncludes.forEach { resourceConfig ->
        revInclude(
          resourceConfig.resource,
          ReferenceClientParam(resourceConfig.searchParameter),
        ) {
          (this as Search).applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = true,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }
      forwardIncludes.forEach { resourceConfig ->
        include(
          resourceConfig.resource,
          ReferenceClientParam(resourceConfig.searchParameter),
        ) {
          (this as Search).applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = true,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }
    }
    return fhirEngine.batchedSearch(search)
  }

  private suspend fun handleSearchResults(
    searchResults: List<SearchResult<Resource>>,
    repositoryResourceDataResultMap: MutableMap<String, RepositoryResourceData>,
    repositoryResourceDataMap: Map<String, String>?,
    relatedResourceConfigs: List<ResourceConfig>,
    baseResourceConfigId: String?,
    configComputedRuleValues: Map<String, Any>,
  ): SearchQueryResultQueue {
    val relatedResourcesQueue = RelatedResourcesQueue()
    val (forwardIncludes, reverseIncludes) =
      relatedResourceConfigs
        .asSequence()
        .filter { !it.resultAsCount }
        .partition { !it.isRevInclude }
    val forwardIncludesMap =
      forwardIncludes.groupBy { it.searchParameter!! }.mapValues { it.value.first() }
    val reverseIncludesMap =
      reverseIncludes
        .groupBy { "${it.resource.name}_${it.searchParameter}".lowercase() }
        .mapValues { it.value.first() }
    val includedResourcesCountConfigs =
      relatedResourceConfigs
        .asSequence()
        .flatMap { it.relatedResources }
        .filter { it.resultAsCount && !it.searchParameter.isNullOrEmpty() }
        .toList()

    searchResults.forEach { searchResult: SearchResult<Resource> ->
      // Create new repository data if none exist (subsequent queries will have repository data)
      // First get the key for the repository data, then proceed to retrieve it from the result map
      val repositoryResourceDataMapId =
        repositoryResourceDataMap?.get(searchResult.resource.logicalId)
      val repositoryResourceData =
        repositoryResourceDataResultMap[repositoryResourceDataMapId]
          ?: RepositoryResourceData(
            resource = searchResult.resource,
            resourceConfigId = baseResourceConfigId,
          )

      searchResult.included?.forEach { entry ->
        // Add the forward included resources to the relatedResourcesMap
        val relatedResourceConfig = forwardIncludesMap[entry.key]
        updateRepositoryResourceData(
          resources = entry.value,
          relatedResourceConfig = relatedResourceConfig,
          repositoryResourceData = repositoryResourceData,
          relatedResourcesQueue = relatedResourcesQueue,
        )
        if (entry.value.isNotEmpty()) {
          handleCountResults(
            resources = entry.value,
            repositoryResourceData = repositoryResourceData,
            countConfigs = includedResourcesCountConfigs,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }
      searchResult.revIncluded?.forEach { entry ->
        val (resourceType, searchParam) = entry.key
        val name = "${resourceType.name}_$searchParam".lowercase()
        val relatedResourceConfig = reverseIncludesMap[name]
        // Add the reverse included resources to the relatedResourcesMap
        updateRepositoryResourceData(
          resources = entry.value,
          relatedResourceConfig = relatedResourceConfig,
          repositoryResourceData = repositoryResourceData,
          relatedResourcesQueue = relatedResourcesQueue,
        )
        if (entry.value.isNotEmpty()) {
          handleCountResults(
            resources = entry.value,
            repositoryResourceData = repositoryResourceData,
            countConfigs = includedResourcesCountConfigs,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }
      if (repositoryResourceDataMap == null) {
        repositoryResourceDataResultMap[searchResult.resource.logicalId] = repositoryResourceData
      }
    }
    return groupAndBatchQueriedResources(relatedResourcesQueue, RESOURCE_BATCH_SIZE)
  }

  private fun updateRepositoryResourceData(
    resources: List<Resource>,
    relatedResourceConfig: ResourceConfig?,
    repositoryResourceData: RepositoryResourceData?,
    relatedResourcesQueue: RelatedResourcesQueue,
  ) {
    if (resources.isNotEmpty() && repositoryResourceData != null) {
      val key = relatedResourceConfig?.id ?: relatedResourceConfig?.resource?.name
      if (!key.isNullOrBlank()) {
        repositoryResourceData.apply {
          relatedResourcesMap
            .getOrPut(key = key) { mutableListOf() }
            .apply { (this as MutableList).addAll(resources.distinctBy { it.logicalId }) }
        }
      }

      if (!relatedResourceConfig?.relatedResources.isNullOrEmpty()) {
        // Track the next nested resource to be fetched. ID for base resources is the unique key
        relatedResourcesQueue.addLast(
          Triple(
            first = resources.map { it.logicalId }.distinct(),
            second = relatedResourceConfig!!,
            third = repositoryResourceData.resource.logicalId,
          ),
        )
      }
    }
  }

  /**
   * Count the related resources that references the provided [resources]. The count is updated in
   * the [repositoryResourceData].
   */
  suspend fun handleCountResults(
    resources: List<Resource>,
    repositoryResourceData: RepositoryResourceData,
    countConfigs: List<ResourceConfig>,
    configComputedRuleValues: Map<String, Any>,
  ) {
    if (countConfigs.isEmpty()) return
    resources.chunked(RESOURCE_BATCH_SIZE).forEach { theResources ->
      countRelatedResources(
        resources = theResources,
        repositoryResourceData = repositoryResourceData,
        countConfigs = countConfigs,
        configComputedRuleValues = configComputedRuleValues,
      )
    }
  }

  private suspend fun countRelatedResources(
    resources: List<Resource>,
    repositoryResourceData: RepositoryResourceData,
    countConfigs: List<ResourceConfig>,
    configComputedRuleValues: Map<String, Any>,
  ) {
    countConfigs.forEach { resourceConfig ->
      if (resourceConfig.countResultConfig?.sumCounts == true) {
        // Count all the related resources. E.g. count all members (Patient) of household (Group)
        val countSearch =
          Search(resourceConfig.resource).apply {
            val filters =
              resources.map {
                val apply: ReferenceParamFilterCriterion.() -> Unit = {
                  value = it.logicalId.asReference(it.resourceType).reference
                }
                apply
              }
            filter(
              ReferenceClientParam(resourceConfig.searchParameter),
              *filters.toTypedArray(),
            )
            applyConfiguredSortAndFilters(
              resourceConfig = resourceConfig,
              sortData = false,
              configComputedRuleValues = configComputedRuleValues,
            )
          }
        val key = resourceConfig.id ?: resourceConfig.resource.name
        countSearch.count(
          onSuccess = {
            repositoryResourceData.apply {
              relatedResourcesCountMap
                .getOrPut(key) { mutableListOf() }
                .apply {
                  (this as MutableList).add(
                    RelatedResourceCount(
                      count = it,
                      relatedResourceType = resourceConfig.resource,
                    ),
                  )
                }
            }
          },
          onFailure = {
            Timber.e(
              it,
              "Error retrieving total count for all related resources identified by $key",
            )
          },
        )
      } else {
        // Count each related resources, e.g. number of visits (Encounter) for every Patient
        computeCountForEachRelatedResource(
          resources = resources,
          resourceConfig = resourceConfig,
          configComputedRuleValues = configComputedRuleValues,
          repositoryResourceData = repositoryResourceData,
        )
      }
    }
  }

  /**
   * Retrieve and populate secondary resources in [resultsDataMap]. Every [RepositoryResourceData]
   * in [resultsDataMap] must have a copy of the secondary resources. Secondary resources
   * independent resources that needs to be loaded and have no relationship with the primary base
   * resources.
   */
  private suspend fun populateSecondaryResources(
    secondaryResources: List<FhirResourceConfig>?,
    configComputedRuleValues: Map<String, Any>,
    resultsDataMap: MutableMap<String, RepositoryResourceData>,
  ) {
    if (!secondaryResources.isNullOrEmpty()) {
      val secondaryRepositoryResourceData = mutableListOf<RepositoryResourceData>()
      secondaryResources.forEach { secondaryFhirResourceConfig ->
        val resultsMap =
          searchNestedResources(
            baseResourceIds = null,
            fhirResourceConfig = secondaryFhirResourceConfig,
            resultsDataMap = resultsDataMap,
            configComputedRuleValues = configComputedRuleValues,
            activeResourceFilters = null,
            currentPage = null,
            pageSize = 1,
          )
        secondaryRepositoryResourceData.addAll(resultsMap.values)
      }
      resultsDataMap.forEach { entry ->
        entry.value.secondaryRepositoryResourceData = secondaryRepositoryResourceData
      }
    }
  }

  /**
   * Groups resources by their [ResourceConfig] and batches them into groups of up to a specified
   * size.
   *
   * This function combines resources across multiple triples with the same [ResourceConfig],
   * ensuring all resources are grouped together while maintaining their association with the
   * respective [RepositoryResourceData]. Each batch contains a mapping of resource logicalId to
   * their corresponding [RepositoryResourceData], allowing traceability.
   *
   * @param relatedResourcesQueue An [ArrayDeque] containing triples of:
   * - A list of [Resource.logicalId]s to be grouped and batched.
   * - A [ResourceConfig] shared by all resources in the triple.
   * - A [RepositoryResourceData] shared by all resources in the triple.
   *
   * @param batchSize The maximum number of resources in each batch. Must be greater than 0.
   * @return A [ArrayDeque] of triples where each triple contains:
   * - A list of [Resource.logicalId]s grouped into batches of up to [batchSize].
   * - The [ResourceConfig] shared by the batch.
   * - A map of [Resource.logicalId] to its corresponding [RepositoryResourceData], ensuring
   *   traceability.
   *
   * @throws IllegalArgumentException if [batchSize] is less than or equal to 0.
   *
   * ```
   */
  fun groupAndBatchQueriedResources(
    relatedResourcesQueue: RelatedResourcesQueue,
    batchSize: Int,
  ): SearchQueryResultQueue {
    require(batchSize > 0) { "Batch size must be greater than 0" }
    val resultQueue = SearchQueryResultQueue()
    val bufferMap = mutableMapOf<ResourceConfig, MutableList<Pair<String, String>>>()

    while (relatedResourcesQueue.isNotEmpty()) {
      val (resourceIds, config, data) = relatedResourcesQueue.removeFirst()
      val buffer = bufferMap.getOrPut(config) { mutableListOf() }

      resourceIds.forEach { id -> buffer.add(id to data) }

      // Create and add batches to the result queue
      while (buffer.size >= batchSize) {
        val batch = List(batchSize) { buffer.removeFirst() }
        resultQueue.addLast(
          Triple(
            first = batch.map { it.first },
            second = config,
            third = batch.associate { it.first to it.second },
          ),
        )
      }
    }

    // Add any remaining items in the buffers
    for ((config, buffer) in bufferMap) {
      if (buffer.isNotEmpty()) {
        val batch = buffer.toList()
        resultQueue.addLast(
          Triple(
            first = batch.map { it.first },
            second = config,
            third = batch.associate { it.first to it.second },
          ),
        )
        buffer.clear()
      }
    }
    return resultQueue
  }
}
