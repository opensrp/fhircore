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
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.register.ResourceConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RelatedResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

class RegisterRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DefaultDispatcherProvider,
  val configurationRegistry: ConfigurationRegistry,
  val rulesFactory: RulesFactory
) :
  Repository, DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider) {

  override suspend fun loadRegisterData(currentPage: Int, registerId: String): List<ResourceData> =
    try {
      val registerConfiguration = retrieveRegisterConfiguration(registerId)
      val baseResourceConfig = registerConfiguration.fhirResource.baseResource
      val relatedResourcesConfig = registerConfiguration.fhirResource.relatedResources
      val baseResourceClass = baseResourceConfig.resource.resourceClassType()
      val baseResourceType = baseResourceClass.newInstance().resourceType

      val baseResources: List<Resource> =
        searchResource(
          baseResourceClass = baseResourceClass,
          dataQueries = baseResourceConfig.dataQueries,
          currentPage = currentPage
        )

      // Retrieve data for each of the configured related resources
      // Also retrieve data for nested related resources for each of the related resource
      baseResources.map { baseResource: Resource ->
        val currentRelatedResources = LinkedList<RelatedResourceData>()

        // Retrieve related resources recursively
        relatedResourcesConfig.forEach { resourceConfig: ResourceConfig ->
          val relatedResources =
            searchRelatedResources(
              resourceConfig = resourceConfig,
              baseResourceType = baseResourceType,
              baseResource = baseResource,
              fhirPathExpression = resourceConfig.fhirPathExpression
            )
          currentRelatedResources.addAll(relatedResources)
        }

        val relatedResourcesMap = currentRelatedResources.createRelatedResourcesMap()

        // Compute values via rules engine and return a map. Rule names MUST be unique
        val computedValuesMap =
          rulesFactory.fireRule(
            ruleConfigs = registerConfiguration.registerCard.rules,
            baseResource = baseResource,
            relatedResourcesMap = relatedResourcesMap
          )

        ResourceData(baseResource, relatedResourcesMap, computedValuesMap)
      }
    } catch (resourceNotFoundException: ResourceNotFoundException) {
      Timber.e(resourceNotFoundException)
      emptyList()
    }

  /**
   *
   * This function creates a map of resource type against [Resource] from a list of nested
   * [RelatedResourceData].
   *
   * Example: A list of [RelatedResourceData] with Patient as its resource and two nested
   * [RelatedResourceData] of resource type Condition & CarePlan returns:
   *
   * ```
   * {
   * "Patient" -> [Patient],
   * "Condition" -> [Condition],
   * "CarePlan" -> [CarePlan]
   * }
   * ```
   *
   * NOTE: [RelatedResourceData] are represented as tree however they grouped by their resource type
   * as key and value as list of [Resource] s in the map.
   */
  private fun LinkedList<RelatedResourceData>.createRelatedResourcesMap():
    MutableMap<String, MutableList<Resource>> {
    val relatedResourcesMap = mutableMapOf<String, MutableList<Resource>>()
    while (this.isNotEmpty()) {
      val relatedResourceData = this.removeFirst()
      relatedResourcesMap
        .getOrPut(relatedResourceData.resource.resourceType.name) { mutableListOf() }
        .add(relatedResourceData.resource)
      relatedResourceData.relatedResources.forEach { this.addLast(it) }
    }
    return relatedResourcesMap
  }

  private suspend fun searchRelatedResources(
    resourceConfig: ResourceConfig,
    baseResourceType: ResourceType,
    baseResource: Resource,
    fhirPathExpression: String?
  ): LinkedList<RelatedResourceData> {
    val relatedResourceClass = resourceConfig.resource.resourceClassType()
    val relatedResourceType = relatedResourceClass.newInstance().resourceType
    val relatedResourceData = LinkedList<RelatedResourceData>()
    if (fhirPathExpression.isNullOrEmpty()) {
      val relatedResourceSearch =
        Search(type = relatedResourceType).apply {
          filterByResourceTypeId(
            ReferenceClientParam(resourceConfig.searchParameter),
            baseResourceType,
            baseResource.logicalId
          )
          resourceConfig.dataQueries?.forEach { filterBy(it) }
        }
      fhirEngine.search<Resource>(relatedResourceSearch).forEach {
        relatedResourceData.addLast(RelatedResourceData(it))
      }
    } else {
      FhirPathDataExtractor.extractData(baseResource, fhirPathExpression)
        .takeWhile { it is Reference }
        .map { it as Reference }
        .map {
          fhirEngine.get(
            resourceConfig.resource.resourceClassType().newInstance().resourceType,
            it.extractId()
          )
        }
        .forEach { resource ->
          relatedResourceData.addLast(RelatedResourceData(resource = resource))
        }
    }
    relatedResourceData.forEach { resourceData: RelatedResourceData ->
      resourceConfig.relatedResources?.forEach {
        val searchRelatedResources =
          searchRelatedResources(
            resourceConfig = it,
            baseResourceType = resourceData.resource.resourceType,
            baseResource = resourceData.resource,
            fhirPathExpression = it.fhirPathExpression
          )
        resourceData.relatedResources.addAll(searchRelatedResources)
      }
    }
    return relatedResourceData
  }

  private suspend fun searchResource(
    baseResourceClass: Class<out Resource>,
    dataQueries: List<DataQuery>?,
    currentPage: Int
  ): List<Resource> {
    val resourceType = baseResourceClass.newInstance().resourceType
    val search =
      Search(type = resourceType).apply {
        dataQueries?.forEach { filterBy(it) }
        // For patient return only active members
        if (resourceType == ResourceType.Patient) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
        count = PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }
    return fhirEngine.search(search)
  }

  /** Count register data for the provided [registerId]. Use the configured base resource filters */
  override suspend fun countRegisterData(registerId: String): Long {
    val registerConfiguration = retrieveRegisterConfiguration(registerId)
    val baseResourceConfig = registerConfiguration.fhirResource.baseResource
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()

    val resourceType = baseResourceClass.newInstance().resourceType
    return fhirEngine.count(
      Search(resourceType).apply {
        baseResourceConfig.dataQueries?.forEach { filterBy(it) }
        // For patient return only active members count
        if (resourceType == ResourceType.Patient) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
      }
    )
  }

  override suspend fun loadProfileData(profileId: String, identifier: String): ProfileData? =
    withContext(dispatcherProvider.io()) {
      // TODO return profile data
      null
    }

  fun retrieveRegisterConfiguration(registerId: String): RegisterConfiguration =
    configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId)

  companion object {
    const val ACTIVE = "active"
  }
}
