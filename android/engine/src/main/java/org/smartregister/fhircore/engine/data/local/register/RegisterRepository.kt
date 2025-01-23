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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.ContentCache
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
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
  override val configRulesExecutor: ConfigRulesExecutor,
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
    configRulesExecutor = configRulesExecutor,
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
  ): List<RepositoryResourceData> {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val requiredFhirResourceConfig = fhirResourceConfig ?: registerConfiguration.fhirResource
    val configComputedRuleValues = registerConfiguration.configRules.configRulesComputedValues()

    val filterByRelatedEntityLocationMetaTag =
      registerConfiguration.filterDataByRelatedEntityLocation
    val pageSize = registerConfiguration.pageSize
    val repositoryResourceDataList =
      searchNestedResources(
        baseResourceIds = null,
        fhirResourceConfig = requiredFhirResourceConfig,
        configComputedRuleValues = configComputedRuleValues,
        activeResourceFilters = registerConfiguration.activeResourceFilters,
        filterByRelatedEntityLocationMetaTag = filterByRelatedEntityLocationMetaTag,
        currentPage = currentPage,
        pageSize = pageSize,
      )

    populateSecondaryResources(
      secondaryResources = registerConfiguration.secondaryResources,
      configComputedRuleValues = configComputedRuleValues,
      repositoryResourceDataList = repositoryResourceDataList,
    )

    return repositoryResourceDataList
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

    if (!filterByRelatedEntityLocation) {
      return fhirEngine.count(
        Search(baseResourceConfig.resource).apply {
          applyConfiguredSortAndFilters(
            resourceConfig = baseResourceConfig,
            sortData = false,
            filterActiveResources = filterActiveResources,
            configComputedRuleValues = configComputedRuleValues,
          )
        },
      )
    }

    val locationIds = retrieveRelatedEntitySyncLocationIds().chunked(SQL_WHERE_CLAUSE_LIMIT)
    var total = 0L
    for (ids in locationIds) {
      val search =
        createSearch(
          baseResourceIds = ids,
          baseResourceConfig = baseResourceConfig,
          filterActiveResources = filterActiveResources,
          configComputedRuleValues = configComputedRuleValues,
          sortData = false,
          currentPage = null,
          count = null,
          relTagCodeSystem =
            context.getString(R.string.sync_strategy_related_entity_location_system),
        )
      total += fhirEngine.count(search)
    }
    return total
  }

  override suspend fun loadProfileData(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsMap: Map<String, String>?,
  ): RepositoryResourceData? {
    val profileConfiguration = retrieveProfileConfiguration(profileId, paramsMap)
    val requiredFhirResourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
    val configComputedRuleValues = profileConfiguration.configRules.configRulesComputedValues()

    val repositoryResourceDataList =
      searchNestedResources(
        baseResourceIds = listOf(resourceId),
        fhirResourceConfig = requiredFhirResourceConfig,
        configComputedRuleValues = configComputedRuleValues,
        activeResourceFilters = null,
        filterByRelatedEntityLocationMetaTag = false,
        currentPage = null,
        pageSize = null,
      )

    populateSecondaryResources(
      secondaryResources = profileConfiguration.secondaryResources,
      configComputedRuleValues = configComputedRuleValues,
      repositoryResourceDataList = repositoryResourceDataList,
    )
    return repositoryResourceDataList.firstOrNull()
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

  /**
   * Retrieve and populate secondary resources in [repositoryResourceDataList]. Every
   * [RepositoryResourceData] in [repositoryResourceDataList] must have a copy of the secondary
   * resources. Secondary resources are independent resources and have no relationship with the
   * primary base resources.
   */
  private suspend fun populateSecondaryResources(
    secondaryResources: List<FhirResourceConfig>?,
    configComputedRuleValues: Map<String, Any>,
    repositoryResourceDataList: List<RepositoryResourceData>,
  ) {
    if (!secondaryResources.isNullOrEmpty()) {
      val secondaryRepositoryResourceData = mutableListOf<RepositoryResourceData>()
      secondaryResources.forEach { secondaryFhirResourceConfig ->
        val result =
          searchNestedResources(
            baseResourceIds = null,
            fhirResourceConfig = secondaryFhirResourceConfig,
            configComputedRuleValues = configComputedRuleValues,
            activeResourceFilters = null,
            filterByRelatedEntityLocationMetaTag = false,
            currentPage = null,
            pageSize = 1,
          )
        secondaryRepositoryResourceData.addAll(result)
      }
      repositoryResourceDataList.forEach { item ->
        item.secondaryRepositoryResourceData = secondaryRepositoryResourceData
      }
    }
  }
}
