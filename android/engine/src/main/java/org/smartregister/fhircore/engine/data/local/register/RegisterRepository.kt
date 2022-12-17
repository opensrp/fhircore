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

import ca.uhn.fhir.rest.gclient.DateClientParam
import ca.uhn.fhir.rest.gclient.NumberClientParam
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
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
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.DataType
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RelatedResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.SortConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
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
  override val sharedPreferencesHelper: SharedPreferencesHelper,
  override val configurationRegistry: ConfigurationRegistry,
  override val configService: ConfigService,
  val rulesFactory: RulesFactory,
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

  override suspend fun loadRegisterData(currentPage: Int, registerId: String): List<ResourceData> {
    val registerConfiguration = retrieveRegisterConfiguration(registerId)
    val baseResourceConfig = registerConfiguration.fhirResource.baseResource
    val relatedResourcesConfig = registerConfiguration.fhirResource.relatedResources
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val baseResourceType = baseResourceClass.newInstance().resourceType

    val baseResources: List<Resource> =
      withContext(dispatcherProvider.io()) {
        searchResource(
          baseResourceClass = baseResourceClass,
          dataQueries = baseResourceConfig.dataQueries,
          sortConfigs = baseResourceConfig.sortConfigs,
          currentPage = currentPage,
          pageSize = registerConfiguration.pageSize
        )
      }

    // Retrieve data for each of the configured related resources
    // Also retrieve data for nested related resources for each of the related resource
    val resourceData =
      baseResources.map { baseResource: Resource ->
        retrieveRelatedResources(
          relatedResourcesConfig = relatedResourcesConfig,
          baseResourceType = baseResourceType,
          baseResource = baseResource,
          views = registerConfiguration.registerCard.views,
          rules = registerConfiguration.registerCard.rules
        )
      }
    return resourceData
  }

  private suspend fun retrieveRelatedResources(
    relatedResourcesConfig: List<ResourceConfig>,
    baseResourceType: ResourceType,
    baseResource: Resource,
    rules: List<RuleConfig>,
    views: List<ViewProperties>
  ): ResourceData {
    val currentRelatedResources = LinkedList<RelatedResourceData>()

    // Retrieve related resources recursively
    relatedResourcesConfig.forEach { resourceConfig: ResourceConfig ->
      val relatedResources =
        withContext(dispatcherProvider.io()) {
          searchRelatedResources(
            resourceConfig = resourceConfig,
            baseResourceType = baseResourceType,
            baseResource = baseResource,
            fhirPathExpression = resourceConfig.fhirPathExpression
          )
        }
      currentRelatedResources.addAll(relatedResources)
    }

    val relatedResourcesMap = currentRelatedResources.createRelatedResourcesMap()

    // Compute values via rules engine and return a map. Rule names MUST be unique
    val computedValuesMap =
      rulesFactory.fireRule(
        ruleConfigs = rules,
        baseResource = baseResource,
        relatedResourcesMap = relatedResourcesMap
      )

    val listResourceDataMap = computeListRules(views, relatedResourcesMap, computedValuesMap)

    return ResourceData(
      baseResourceId = baseResource.logicalId.extractLogicalIdUuid(),
      baseResourceType = baseResource.resourceType,
      computedValuesMap = computedValuesMap,
      listResourceDataMap = listResourceDataMap
    )
  }

  /**
   * This function pre-computes all the Rules for [ViewType]'s of List including list nested in the
   * views. This function also retrieves Lists rendered inside other views and computes their rules.
   * The LIST view computed values includes the parent's.
   *
   * This function re-uses the parent view's [computedValuesMap] and [relatedResourcesMap]. It does
   * not re-query data from the cache. For a List view, the base resource will always be available
   * in the parent's view relatedResourcesMap. We retrieve it and use it to get it's related
   * resources so we can fire rules for the List.
   */
  private fun computeListRules(
    views: List<ViewProperties>,
    relatedResourcesMap: MutableMap<String, MutableList<Resource>>,
    computedValuesMap: Map<String, Any>
  ) =
    views.retrieveListProperties().associate { viewProperties ->

      // Retrieve baseResource for LIST, then the related resources then fire rules
      val resourceDataList =
        relatedResourcesMap[viewProperties.baseResource]?.map { resource ->
          val newRelatedResources =
            viewProperties.relatedResources.associate {
              Pair(
                it.resourceType,
                rulesFactory.rulesEngineService.retrieveRelatedResources(
                  resource = resource,
                  relatedResourceType = it.resourceType,
                  fhirPathExpression = it.fhirPathExpression,
                  relatedResourcesMap = relatedResourcesMap
                )
              )
            }

          // Values computed from the rules defined in LIST view RegisterCard
          val listComputedValuesMap =
            rulesFactory.fireRule(
              ruleConfigs = viewProperties.registerCard.rules,
              baseResource = resource,
              relatedResourcesMap = newRelatedResources
            )

          // LIST view should reuse the previously computed values
          ResourceData(
            baseResourceId = resource.logicalId.extractLogicalIdUuid(),
            baseResourceType = resource.resourceType,
            computedValuesMap = computedValuesMap.plus(listComputedValuesMap),
            listResourceDataMap = emptyMap()
          )
        }
          ?: emptyList()

      Pair(viewProperties.id, resourceDataList)
    }

  /**
   * This function obtains all [ListProperties] from the [ViewProperties] list; including the nested
   * LISTs
   */
  fun List<ViewProperties>.retrieveListProperties(): List<ListProperties> {
    val listProperties = mutableListOf<ListProperties>()
    val viewPropertiesLinkedList: LinkedList<ViewProperties> = LinkedList(this)
    while (viewPropertiesLinkedList.isNotEmpty()) {
      val properties = viewPropertiesLinkedList.removeFirst()
      if (properties.viewType == ViewType.LIST) {
        listProperties.add(properties as ListProperties)
      }
      when (properties.viewType) {
        ViewType.COLUMN ->
          viewPropertiesLinkedList.addAll((properties as ColumnProperties).children)
        ViewType.ROW -> viewPropertiesLinkedList.addAll((properties as RowProperties).children)
        ViewType.CARD -> viewPropertiesLinkedList.addAll((properties as CardViewProperties).content)
        ViewType.LIST ->
          viewPropertiesLinkedList.addAll((properties as ListProperties).registerCard.views)
        else -> {}
      }
    }
    return listProperties
  }

  /**
   *
   * This function creates a map of resource type against [Resource] from a list of nested
   * [RelatedResourceData].
   *
   * Example: A list of [RelatedResourceData] with Patient as its base resource and two nested
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
    val relatedResourcesData = LinkedList<RelatedResourceData>()
    if (fhirPathExpression.isNullOrEmpty()) {
      val relatedResourceSearch =
        Search(type = relatedResourceType).apply {
          filterByResourceTypeId(
            ReferenceClientParam(resourceConfig.searchParameter),
            baseResourceType,
            baseResource.logicalId
          )
          resourceConfig.dataQueries?.forEach { filterBy(it) }
          sort(resourceConfig.sortConfigs)
        }
      fhirEngine.search<Resource>(relatedResourceSearch).forEach { resource ->
        relatedResourcesData.addLast(RelatedResourceData(resource = resource))
      }
    } else {
      fhirPathDataExtractor
        .extractData(baseResource, fhirPathExpression)
        .takeWhile { it is Reference }
        .map { it as Reference }
        .mapNotNull {
          try {
            fhirEngine.get(
              resourceConfig.resource.resourceClassType().newInstance().resourceType,
              it.extractId()
            )
          } catch (exception: ResourceNotFoundException) {
            Timber.e(exception)
            null
          }
        }
        .forEach { resource ->
          relatedResourcesData.addLast(RelatedResourceData(resource = resource))
        }
    }
    relatedResourcesData.forEach { resourceData: RelatedResourceData ->
      resourceConfig.relatedResources.forEach {
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
    return relatedResourcesData
  }

  private suspend fun searchResource(
    baseResourceClass: Class<out Resource>,
    dataQueries: List<DataQuery>?,
    sortConfigs: List<SortConfig>,
    currentPage: Int,
    pageSize: Int
  ): List<Resource> {
    val resourceType = baseResourceClass.newInstance().resourceType
    val search =
      Search(type = resourceType).apply {
        dataQueries?.forEach { filterBy(it) }
        // For patient return only active members
        if (resourceType == ResourceType.Patient) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
        sort(sortConfigs)
        count = pageSize
        from = currentPage * pageSize
      }
    return fhirEngine.search(search)
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
  override suspend fun countRegisterData(registerId: String): Long {
    val registerConfiguration = retrieveRegisterConfiguration(registerId)
    val baseResourceConfig = registerConfiguration.fhirResource.baseResource
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val resourceType = baseResourceClass.newInstance().resourceType

    val search =
      Search(resourceType).apply {
        baseResourceConfig.dataQueries?.forEach { filterBy(it) }
        // For patient return only active members count
        if (resourceType == ResourceType.Patient) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
      }

    return fhirEngine.count(search)
  }

  override suspend fun loadProfileData(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig?
  ): ResourceData {
    val profileConfiguration =
      configurationRegistry.retrieveConfiguration<ProfileConfiguration>(
        ConfigType.Profile,
        profileId
      )
    val resourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
    val baseResourceConfig = resourceConfig.baseResource
    val relatedResourcesConfig = resourceConfig.relatedResources
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val baseResourceType = baseResourceClass.newInstance().resourceType

    val baseResource: Resource =
      withContext(dispatcherProvider.io()) {
        fhirEngine.get(baseResourceType, resourceId.extractLogicalIdUuid())
      }

    return retrieveRelatedResources(
      relatedResourcesConfig = relatedResourcesConfig,
      baseResourceType = baseResourceType,
      baseResource = baseResource,
      views = profileConfiguration.views,
      rules = profileConfiguration.rules
    )
  }

  fun retrieveRegisterConfiguration(registerId: String): RegisterConfiguration =
    configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId)

  companion object {
    const val ACTIVE = "active"
  }
}
