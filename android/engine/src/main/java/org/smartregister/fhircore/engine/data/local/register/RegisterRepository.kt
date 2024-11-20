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
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Search
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
import org.smartregister.fhircore.engine.data.local.ContentCache
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.batchedSearch
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

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
    val repositoryResourceDataList = mutableListOf<RepositoryResourceData>()
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val configComputedRuleValues = registerConfiguration.configRules.configRulesComputedValues()
    val requiredFhirResourceConfig = fhirResourceConfig ?: registerConfiguration.fhirResource

    val searchResults =
      searchResources(
        baseResourceIds = null,
        baseResourceConfig = requiredFhirResourceConfig.baseResource,
        relatedResourcesConfigs = requiredFhirResourceConfig.relatedResources,
        activeResourceFilters = registerConfiguration.activeResourceFilters,
        configComputedRuleValues = configComputedRuleValues,
        currentPage = currentPage,
        pageSize = registerConfiguration.pageSize,
      )

    val processedResults =
      ArrayDeque(
        handleSearchResults(
          searchResults = searchResults,
          repositoryResourceDataList = repositoryResourceDataList,
          repositoryResourceDataMap = null,
          relatedResourceConfigs = requiredFhirResourceConfig.relatedResources,
          baseResourceConfigId = requiredFhirResourceConfig.baseResource.id,
          pageSize = registerConfiguration.pageSize,
        ),
      )

    while (processedResults.isNotEmpty()) {
      val (baseResources, resourceConfig, repositoryResourceDataMap) =
        processedResults.removeFirst()
      val newSearchResults =
        searchResources(
          baseResourceIds = baseResources.map { it.logicalId },
          baseResourceConfig = resourceConfig,
          relatedResourcesConfigs = resourceConfig.relatedResources,
          activeResourceFilters = registerConfiguration.activeResourceFilters,
          configComputedRuleValues = configComputedRuleValues,
          currentPage = null,
          pageSize = null,
        )

      val newProcessedResults =
        handleSearchResults(
          searchResults = newSearchResults,
          repositoryResourceDataList = repositoryResourceDataList,
          repositoryResourceDataMap = repositoryResourceDataMap,
          relatedResourceConfigs = resourceConfig.relatedResources,
          baseResourceConfigId = requiredFhirResourceConfig.baseResource.id,
          pageSize = registerConfiguration.pageSize,
        )
      processedResults.addAll(newProcessedResults)
    }

    val rules = rulesExecutor.rulesFactory.generateRules(registerConfiguration.registerCard.rules)
    return repositoryResourceDataList.map {
      rulesExecutor.processResourceData(
        repositoryResourceData = it,
        params = paramsMap,
        rules = rules,
      )
    }
  }

  private fun handleSearchResults(
    searchResults: List<SearchResult<Resource>>,
    repositoryResourceDataList: MutableList<RepositoryResourceData>,
    repositoryResourceDataMap: Map<String, RepositoryResourceData>?,
    relatedResourceConfigs: List<ResourceConfig>,
    baseResourceConfigId: String?,
    pageSize: Int,
  ): List<Triple<List<Resource>, ResourceConfig, Map<String, RepositoryResourceData>>> {
    val relatedResourcesQueue =
      ArrayDeque<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>>()
    val (forwardIncludes, reverseIncludes) =
      relatedResourceConfigs.filter { !it.resultAsCount }.partition { !it.isRevInclude }
    val forwardIncludesMap =
      forwardIncludes.groupBy { it.searchParameter!! }.mapValues { it.value.first() }
    val reverseIncludesMap =
      reverseIncludes
        .groupBy { "${it.resource.name}_${it.searchParameter}".lowercase() }
        .mapValues { it.value.first() }

    searchResults.forEach { searchResult: SearchResult<Resource> ->
      val repositoryResourceData =
        repositoryResourceDataMap?.get(searchResult.resource.logicalId)
          ?: RepositoryResourceData(
            resource = searchResult.resource,
            resourceConfigId = baseResourceConfigId,
          )
      searchResult.included?.forEach { entry ->
        val relatedResourceConfig = forwardIncludesMap[entry.key]
        createRepositoryResourceData(
          resources = entry.value,
          relatedResourceConfig = relatedResourceConfig,
          repositoryResourceData = repositoryResourceData,
          relatedResourcesQueue = relatedResourcesQueue,
        )
      }

      searchResult.revIncluded?.forEach { entry ->
        val (resourceType, searchParam) = entry.key
        val name = "${resourceType.name}_$searchParam".lowercase()
        val relatedResourceConfig = reverseIncludesMap[name]
        createRepositoryResourceData(
          resources = entry.value,
          relatedResourceConfig = relatedResourceConfig,
          repositoryResourceData = repositoryResourceData,
          relatedResourcesQueue = relatedResourcesQueue,
        )
      }
      if (repositoryResourceDataMap == null) {
        repositoryResourceDataList.add(repositoryResourceData)
      }
    }

    return groupAndBatchResources(relatedResourcesQueue, pageSize).toList()
  }

  private fun createRepositoryResourceData(
    resources: List<Resource>,
    relatedResourceConfig: ResourceConfig?,
    repositoryResourceData: RepositoryResourceData?,
    relatedResourcesQueue:
      ArrayDeque<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>>,
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
        relatedResourcesQueue.addLast(
          Triple(
            first = resources,
            second = relatedResourceConfig!!,
            third = repositoryResourceData,
          ),
        )
      }
    }
  }

  suspend fun searchResources(
    baseResourceIds: List<String>?,
    baseResourceConfig: ResourceConfig,
    relatedResourcesConfigs: List<ResourceConfig>,
    activeResourceFilters: List<ActiveResourceFilterConfig>,
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
    paramsList: Array<ActionParameter>?,
  ): ResourceData {
    /*
      val paramsMap: Map<String, String> =
        paramsList
          ?.asSequence()
          ?.filter {
            (it.paramType == ActionParameterType.PARAMDATA ||
              it.paramType == ActionParameterType.UPDATE_DATE_ON_EDIT) && it.value.isNotEmpty()
          }
          ?.associate { it.key to it.value } ?: emptyMap()

      val profileConfiguration = retrieveProfileConfiguration(profileId, paramsMap)
      val resourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
      val baseResourceConfig = resourceConfig.baseResource

      val baseResource: Resource =
        fhirEngine.get(baseResourceConfig.resource, resourceId.extractLogicalIdUuid())

      val configComputedRuleValues = profileConfiguration.configRules.configRulesComputedValues()

      val retrievedRelatedResources =
        retrieveRelatedResources(
          resource = baseResource,
          relatedResourcesConfigs = resourceConfig.relatedResources,
          configComputedRuleValues = configComputedRuleValues,
        )

      RepositoryResourceData(
        resourceRulesEngineFactId = baseResourceConfig.id ?: baseResourceConfig.resource.name,
        resource = baseResource,
        relatedResourcesMap = retrievedRelatedResources.relatedResourceMap,
        relatedResourcesCountMap = retrievedRelatedResources.relatedResourceCountMap,
        secondaryRepositoryResourceData =
          profileConfiguration.secondaryResources.retrieveSecondaryRepositoryResourceData(
            profileConfiguration.filterActiveResources,
          ),
      )
    }*/
    return ResourceData("", ResourceType.Patient, emptyMap())
  }

  fun retrieveProfileConfiguration(profileId: String, paramsMap: Map<String, String>) =
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

  /**
   * Groups resources by their [ResourceConfig] and batches them into groups of up to a specified
   * size.
   *
   * This function combines resources across multiple triples with the same [ResourceConfig],
   * ensuring all resources are grouped together while maintaining their association with the
   * respective [RepositoryResourceData]. Each batch contains a mapping of resources to their
   * corresponding [RepositoryResourceData], allowing traceability.
   *
   * @param deque An [ArrayDeque] containing triples of:
   * - A list of [Resource]s to be grouped and batched.
   * - A [ResourceConfig] shared by all resources in the triple.
   * - A [RepositoryResourceData] shared by all resources in the triple.
   *
   * @param batchSize The maximum number of resources in each batch. Must be greater than 0.
   * @return A [Sequence] of triples where each triple contains:
   * - A list of [Resource]s grouped into batches of up to [batchSize].
   * - The [ResourceConfig] shared by the batch.
   * - A map of [Resource] to its corresponding [RepositoryResourceData], ensuring traceability.
   *
   * @throws IllegalArgumentException if [batchSize] is less than or equal to 0.
   *
   * ```
   */
  fun groupAndBatchResources(
    deque: ArrayDeque<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>>,
    batchSize: Int,
  ): Sequence<Triple<List<Resource>, ResourceConfig, Map<String, RepositoryResourceData>>> {
    require(batchSize > 0) { "Batch size must be greater than 0" }
    return sequence {
      val bufferMap =
        mutableMapOf<ResourceConfig, MutableList<Pair<Resource, RepositoryResourceData>>>()

      while (deque.isNotEmpty()) {
        val (resources, config, data) = deque.removeFirst()
        val resourcePairs = resources.map { it to data }
        bufferMap.getOrPut(config) { mutableListOf() }.addAll(resourcePairs)

        // Check if we can emit any batches for the current config
        val buffer = bufferMap[config]!!
        while (buffer.size >= batchSize) {
          yield(
            Triple(
              buffer.take(batchSize).map { it.first },
              config,
              buffer.take(batchSize).associate { it.first.logicalId to it.second },
            ),
          )
          buffer.subList(0, batchSize).clear()
        }
      }

      // Emit remaining items in the buffers
      for ((config, buffer) in bufferMap) {
        if (buffer.isNotEmpty()) {
          yield(
            Triple(
              buffer.map { it.first },
              config,
              buffer.associate { it.first.logicalId to it.second },
            ),
          )
        }
      }
    }
  }
}
