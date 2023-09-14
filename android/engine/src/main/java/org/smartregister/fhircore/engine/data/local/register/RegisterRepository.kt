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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.ActiveResourceFilterConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

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
  ) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    registerId: String,
    paramsMap: Map<String, String>?,
  ): List<RepositoryResourceData> {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    return searchResourcesRecursively(
      filterActiveResources = registerConfiguration.activeResourceFilters,
      fhirResourceConfig = registerConfiguration.fhirResource,
      secondaryResourceConfigs = registerConfiguration.secondaryResources,
      currentPage = currentPage,
      pageSize = registerConfiguration.pageSize,
      configRules = registerConfiguration.configRules,
    )
  }

  private suspend fun searchResourcesRecursively(
    filterActiveResources: List<ActiveResourceFilterConfig>?,
    fhirResourceConfig: FhirResourceConfig,
    secondaryResourceConfigs: List<FhirResourceConfig>?,
    currentPage: Int? = null,
    pageSize: Int? = null,
    configRules: List<RuleConfig>?,
  ): List<RepositoryResourceData> {
    val baseResourceConfig = fhirResourceConfig.baseResource
    val relatedResourcesConfig = fhirResourceConfig.relatedResources
    val configComputedRuleValues = configRules.configRulesComputedValues()
    val search =
      Search(type = baseResourceConfig.resource).apply {
        applyConfiguredSortAndFilters(
          resourceConfig = baseResourceConfig,
          filterActiveResources = filterActiveResources,
          sortData = true,
          configComputedRuleValues = configComputedRuleValues,
        )
        if (currentPage != null && pageSize != null) {
          count = pageSize
          from = currentPage * pageSize
        }
      }

    val baseFhirResources =
      kotlin
        .runCatching {
          withContext(dispatcherProvider.io()) { fhirEngine.search<Resource>(search) }
        }
        .onFailure { Timber.e(it, "Error retrieving resources. Empty list returned by default") }
        .getOrDefault(emptyList())

    return baseFhirResources.map { searchResult ->
      val retrievedRelatedResources =
        withContext(dispatcherProvider.io()) {
          retrieveRelatedResources(
            resources = listOf(searchResult.resource),
            relatedResourcesConfigs = relatedResourcesConfig,
            relatedResourceWrapper = RelatedResourceWrapper(),
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      RepositoryResourceData(
        resourceRulesEngineFactId = baseResourceConfig.id ?: baseResourceConfig.resource.name,
        resource = searchResult.resource,
        relatedResourcesMap = retrievedRelatedResources.relatedResourceMap,
        relatedResourcesCountMap = retrievedRelatedResources.relatedResourceCountMap,
        secondaryRepositoryResourceData =
          withContext(dispatcherProvider.io()) {
            secondaryResourceConfigs.retrieveSecondaryRepositoryResourceData(
              filterActiveResources,
            )
          },
      )
    }
  }

  /** This function fetches other resources that are not linked to the base/primary resource. */
  private suspend fun List<FhirResourceConfig>?.retrieveSecondaryRepositoryResourceData(
    filterActiveResources: List<ActiveResourceFilterConfig>?,
  ): LinkedList<RepositoryResourceData> {
    val secondaryRepositoryResourceDataLinkedList = LinkedList<RepositoryResourceData>()
    this?.forEach {
      secondaryRepositoryResourceDataLinkedList.addAll(
        searchResourcesRecursively(
          fhirResourceConfig = it,
          filterActiveResources = filterActiveResources,
          secondaryResourceConfigs = null,
          configRules = null,
        ),
      )
    }
    return secondaryRepositoryResourceDataLinkedList
  }

  /** Count register data for the provided [registerId]. Use the configured base resource filters */
  override suspend fun countRegisterData(
    registerId: String,
    paramsMap: Map<String, String>?,
  ): Long {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val baseResourceConfig = registerConfiguration.fhirResource.baseResource
    val configComputedRuleValues = registerConfiguration.configRules.configRulesComputedValues()
    val search =
      Search(baseResourceConfig.resource).apply {
        applyConfiguredSortAndFilters(
          resourceConfig = baseResourceConfig,
          sortData = false,
          filterActiveResources = registerConfiguration.activeResourceFilters,
          configComputedRuleValues = configComputedRuleValues,
        )
      }
    return search.count(
      onFailure = {
        Timber.e(it, "Error counting register data for register id: ${registerConfiguration.id}")
      },
    )
  }

  override suspend fun loadProfileData(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsList: Array<ActionParameter>?,
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

    val configComputedRuleValues = profileConfiguration.configRules.configRulesComputedValues()

    val retrievedRelatedResources =
      withContext(dispatcherProvider.io()) {
        retrieveRelatedResources(
          resources = listOf(baseResource),
          relatedResourcesConfigs = resourceConfig.relatedResources,
          relatedResourceWrapper = RelatedResourceWrapper(),
          configComputedRuleValues = configComputedRuleValues,
        )
      }
    return RepositoryResourceData(
      resourceRulesEngineFactId = baseResourceConfig.id ?: baseResourceConfig.resource.name,
      resource = baseResource,
      relatedResourcesMap = retrievedRelatedResources.relatedResourceMap,
      relatedResourcesCountMap = retrievedRelatedResources.relatedResourceCountMap,
      secondaryRepositoryResourceData =
        withContext(dispatcherProvider.io()) {
          profileConfiguration.secondaryResources.retrieveSecondaryRepositoryResourceData(
            profileConfiguration.filterActiveResources,
          )
        },
    )
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

  private fun List<RuleConfig>?.configRulesComputedValues(): Map<String, Any> {
    if (this == null) return emptyMap()
    val configRules = configRulesExecutor.generateRules(this)
    return configRulesExecutor.fireRules(configRules)
  }

  companion object {
    const val ACTIVE = "active"
  }
}
