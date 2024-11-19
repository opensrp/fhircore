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
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.ActiveResourceFilterConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
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
import javax.inject.Inject
import javax.inject.Singleton

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
  ) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    registerId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsMap: Map<String, String>?,
  ): List<ResourceData> {
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

    val repositoryResourceDataList = mutableListOf<RepositoryResourceData>()
    val processedResult =
      handleSearchResults(
        searchResults = searchResults,
        repositoryResourceDataList = repositoryResourceDataList,
        repositoryResourceData = null,
        relatedResourceConfigs = requiredFhirResourceConfig.relatedResources,
        baseResourceConfigId = requiredFhirResourceConfig.baseResource.id,
        pageSize = registerConfiguration.pageSize
      )

    val resourcesQueue =
      ArrayDeque<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>>().apply {
        addAll(processedResult)
      }
    while (resourcesQueue.isNotEmpty()) {
      val (baseResources, resourceConfig, repositoryResourceData) = resourcesQueue.removeFirst()
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
          repositoryResourceData = repositoryResourceData,
          relatedResourceConfigs = resourceConfig.relatedResources,
          baseResourceConfigId = resourceConfig.id,
          pageSize = registerConfiguration.pageSize
        )
      resourcesQueue.addAll(newProcessedResults)
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
    repositoryResourceData: RepositoryResourceData?,
    relatedResourceConfigs: List<ResourceConfig>,
    baseResourceConfigId: String?,
    pageSize: Int
  ): Sequence<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>> {
    val relatedResourcesQueue =
      ArrayDeque<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>>()
    val addToRepoDataMap = repositoryResourceData == null
    val (forwardIncludes, reverseIncludes) =
      relatedResourceConfigs.filter { !it.resultAsCount }.partition { !it.isRevInclude }
    val forwardIncludesMap =
      forwardIncludes.groupBy { it.searchParameter!! }.mapValues { it.value.first() }

    val reverseIncludesMap =
      reverseIncludes
        .groupBy { "${it.resource.name}_${it.searchParameter}".lowercase() }
        .mapValues { it.value.first() }

    searchResults.forEach { searchResult: SearchResult<Resource> ->
      val currentRepositoryResourceData =
        repositoryResourceData
          ?: RepositoryResourceData(
            resource = searchResult.resource,
            resourceConfigId = baseResourceConfigId,
          )
      searchResult.included?.forEach { entry ->
        val relatedResourceConfig = forwardIncludesMap[entry.key]
        createRepositoryResourceData(
          repositoryResourceData = currentRepositoryResourceData,
          relatedResourceConfig = relatedResourceConfig,
          resources = entry.value,
          relatedResourcesQueue = relatedResourcesQueue,
        )
      }

      searchResult.revIncluded?.forEach { entry ->
        val (resourceType, searchParam) = entry.key
        val name = "${resourceType.name}_$searchParam".lowercase()
        val relatedResourceConfig = reverseIncludesMap[name]
        createRepositoryResourceData(
          repositoryResourceData = currentRepositoryResourceData,
          relatedResourceConfig = relatedResourceConfig,
          resources = entry.value,
          relatedResourcesQueue = relatedResourcesQueue,
        )
      }
      if (addToRepoDataMap) {
        repositoryResourceDataList.add(currentRepositoryResourceData)
      }
    }
    return groupAndBatchResources(relatedResourcesQueue, pageSize)
  }

  private fun createRepositoryResourceData(
    repositoryResourceData: RepositoryResourceData,
    relatedResourceConfig: ResourceConfig?,
    resources: List<Resource>,
    relatedResourcesQueue:
      ArrayDeque<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>>,
  ) {
    val key = relatedResourceConfig?.id ?: relatedResourceConfig?.resource?.name
    if (!key.isNullOrBlank()) {
      repositoryResourceData.apply {
        relatedResourcesMap
          .getOrPut(key = key) { mutableListOf() }
          .apply { (this as MutableList).addAll(resources.distinctBy { it.logicalId }) }
      }
    }

    if (!relatedResourceConfig?.relatedResources.isNullOrEmpty() && resources.isNotEmpty()) {
      relatedResourcesQueue.addLast(
        Triple(
          first = resources,
          second = relatedResourceConfig!!,
          third = repositoryResourceData,
        ),
      )
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
   * Groups resources by their [ResourceConfig] (using `id` if available, or `resourceType` as a
   * fallback), then batches them into groups of up to a specified size.
   *
   * This function ensures that all resources with the same [ResourceConfig] are processed together,
   * preserving their associated [RepositoryResourceData], before splitting them into batches.
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
   * - The [RepositoryResourceData] shared by the batch.
   *
   * @throws IllegalArgumentException if [batchSize] is less than or equal to 0
   *
   * ```
   */
  fun groupAndBatchResources(
    deque: ArrayDeque<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>>,
    batchSize: Int,
  ): Sequence<Triple<List<Resource>, ResourceConfig, RepositoryResourceData>> {
    require(batchSize > 0) { "Batch size must be greater than 0" }
    return sequence {
      // Group by ResourceConfig's `id` or `resourceType`
      val groupedByConfig =
        deque.groupBy { triple -> triple.second.id ?: triple.second.resource.name }

      // Iterate through each group and batch resources
      groupedByConfig.forEach { (_, group) ->
        val buffer = mutableListOf<Resource>()
        var currentConfig: ResourceConfig? = null
        var currentData: RepositoryResourceData? = null

        group.forEach { (resources, config, data) ->
          if (buffer.isEmpty()) {
            currentConfig = config
            currentData = data
          }

          buffer.addAll(resources)

          while (buffer.size >= batchSize) {
            yield(Triple(buffer.subList(0, batchSize).toList(), currentConfig!!, currentData!!))
            buffer.subList(0, batchSize).clear()
          }
        }

        // Emit remaining items in the buffer
        if (buffer.isNotEmpty()) {
          yield(Triple(buffer.toList(), currentConfig!!, currentData!!))
        }
      }
    }
  }
}
