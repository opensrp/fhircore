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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

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
  ) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    registerId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsMap: Map<String, String>?,
  ): List<RepositoryResourceData> {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    return searchResourcesRecursively(
      filterByRelatedEntityLocationMetaTag =
        registerConfiguration.filterDataByRelatedEntityLocation,
      filterActiveResources = registerConfiguration.activeResourceFilters,
      fhirResourceConfig = fhirResourceConfig ?: registerConfiguration.fhirResource,
      secondaryResourceConfigs = registerConfiguration.secondaryResources,
      currentPage = currentPage,
      pageSize = registerConfiguration.pageSize,
      configRules = registerConfiguration.configRules,
    )
  }

  /** Count register data for the provided [registerId]. Use the configured base resource filters */
  override suspend fun countRegisterData(
    registerId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsMap: Map<String, String>?,
  ): Long {
    return withContext(dispatcherProvider.io()) {
      val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
      val fhirResource = fhirResourceConfig ?: registerConfiguration.fhirResource
      val baseResourceConfig = fhirResource.baseResource
      val configComputedRuleValues = registerConfiguration.configRules.configRulesComputedValues()
      val filterByRelatedEntityLocation = registerConfiguration.filterDataByRelatedEntityLocation
      val filterActiveResources = registerConfiguration.activeResourceFilters
      countResources(
        filterByRelatedEntityLocation = filterByRelatedEntityLocation,
        baseResourceConfig = baseResourceConfig,
        filterActiveResources = filterActiveResources,
        configComputedRuleValues = configComputedRuleValues,
      )
    }
  }

  override suspend fun loadProfileData(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsList: Array<ActionParameter>?,
  ): RepositoryResourceData {
    return withContext(dispatcherProvider.io()) {
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
    }
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
}
