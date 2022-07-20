/*
 * Copyright 2021 Ona Systems, Inc
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

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.register.ResourceConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.repository.RegisterRepository
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import timber.log.Timber

class PatientRegisterRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DefaultDispatcherProvider,
  val configurationRegistry: ConfigurationRegistry,
  val rulesFactory: RulesFactory
) :
  RegisterRepository,
  DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    registerId: String
  ): List<ResourceData> =
    try {
      val registerConfiguration =
        configurationRegistry.retrieveConfiguration<RegisterConfiguration>(
          ConfigType.Register,
          configId = registerId
        )
      val baseResourceConfig = registerConfiguration.fhirResource.baseResource
      val relatedResourcesConfig = registerConfiguration.fhirResource.relatedResources
      val baseResourceClass = baseResourceConfig.resource.resourceClassType()

      val baseResources =
        searchResource(
          baseResourceClass = baseResourceClass,
          dataQueries = baseResourceConfig.dataQueries,
          loadAll = loadAll,
          registerId = registerId,
          currentPage = currentPage
        )
      // Retrieve data for each of the configured related resources
      baseResources.map { baseResource: Resource ->
        val retrievedRelatedResources = mutableMapOf<String, List<Resource>>()
        relatedResourcesConfig.forEach { resourceConfig: ResourceConfig ->
          val relatedResourceClass = resourceConfig.resource.resourceClassType().newInstance()
          val relatedResourceSearch =
            Search(type = relatedResourceClass.resourceType).apply {
              // Filter resource by reference
              filterByResourceTypeId(
                ReferenceClientParam(resourceConfig.searchParameter),
                baseResourceClass.newInstance().resourceType,
                baseResource.logicalId
              )
              resourceConfig.dataQueries?.forEach { filterBy(it) }
            }
          val relatedResources = fhirEngine.search<Resource>(relatedResourceSearch)
          retrievedRelatedResources[resourceConfig.resource] = relatedResources
        }

        // Compute values via rules engine and return a map. Rule names MUST be unique
        val computedValuesMap =
          rulesFactory.fireRule(
            ruleConfigs = registerConfiguration.registerCard.rules,
            baseResource = baseResource,
            relatedResources = retrievedRelatedResources
          )
        ResourceData(
          baseResource = baseResource,
          relatedResources = retrievedRelatedResources,
          computedValuesMap = computedValuesMap
        )
      }
    } catch (resourceNotFoundException: ResourceNotFoundException) {
      Timber.e(resourceNotFoundException)
      emptyList()
    }

  private suspend fun searchResource(
    baseResourceClass: Class<out Resource>,
    dataQueries: List<DataQuery>?,
    loadAll: Boolean,
    registerId: String,
    currentPage: Int
  ): List<Resource> {
    val resourceType = baseResourceClass.newInstance().resourceType
    val search =
      Search(type = resourceType).apply {
        dataQueries?.forEach { filterBy(it) }
        filter(TokenClientParam(ACTIVE), { value = of(true) }) // filter only active ones
        count =
          if (loadAll) countRegisterData(resourceType, registerId).toInt()
          else PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }
    return fhirEngine.search(search)
  }

  override suspend fun countRegisterData(resourceType: ResourceType, registerId: String): Long =
    fhirEngine.count(
      Search(resourceType).apply { filter(TokenClientParam(ACTIVE), { value = of(true) }) }
    )

  override suspend fun loadProfileData(profileId: String, identifier: String): ProfileData? =
    withContext(dispatcherProvider.io()) {
      // TODO return profile data
      null
    }

  companion object {
    const val ACTIVE = "active"
  }
}
