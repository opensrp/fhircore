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
import org.smartregister.fhircore.engine.configuration.view.ListResource
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.DataType
import org.smartregister.fhircore.engine.domain.model.ExtractedResource
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
    val secondaryResourceConfig = registerConfiguration.secondaryResources
    val secondaryResourceData = LinkedList<RelatedResourceData>()
    val resourceData = mutableListOf<ResourceData>()

    // retrieve secondary ResourceData if secondaryResources are configured
    if (!secondaryResourceConfig.isNullOrEmpty()) {
      secondaryResourceData.addAll(retrieveSecondaryResources(secondaryResourceConfig))
    }

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
    baseResources.map { baseResource: Resource ->
      val currentRelatedResources = LinkedList<RelatedResourceData>()

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

      // Include secondary resourceData in each row if secondaryResources are configured
      currentRelatedResources.addAll(secondaryResourceData)

      val currentResourceData =
        processResourceData(
          relatedResources = currentRelatedResources,
          baseResource = baseResource,
          views = registerConfiguration.registerCard.views,
          rules = registerConfiguration.registerCard.rules
        )

      resourceData.add(currentResourceData)
    }

    return resourceData
  }

  private fun processResourceData(
    relatedResources: LinkedList<RelatedResourceData>,
    baseResource: Resource,
    rules: List<RuleConfig>,
    views: List<ViewProperties>
  ): ResourceData {

    val relatedResourcesMap = relatedResources.createRelatedResourcesMap()

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
   * This function re-uses the parent view' s [computedValuesMap] and [relatedResourcesMap]. It does
   * not re-query data from the cache. For a List view, the base resource will always be available
   * in the parent's view relatedResourcesMap. We retrieve it and use it to get it's related
   * resources so we can fire rules for the List.
   */
  fun computeListRules(
    views: List<ViewProperties>,
    relatedResourcesMap: MutableMap<String, MutableList<Resource>>,
    computedValuesMap: Map<String, Any>
  ): Map<String, List<ResourceData>> {
    val listViewProperties = views.retrieveListProperties()
    val resourceDataMap = mutableMapOf<String, List<ResourceData>>()
    listViewProperties.forEach { listProperties ->
      if (listProperties.resources.isNotEmpty()) {
        val resourceDataList: List<ResourceData> =
          listProperties.resources.flatMap { listResource ->
            filteredListResources(relatedResourcesMap, listResource)
              .mapToResourceData(
                relatedResourcesMap = relatedResourcesMap,
                ruleConfigs = listProperties.registerCard.rules,
                listRelatedResources = listResource.relatedResources,
                computedValuesMap = computedValuesMap
              )
          }
        resourceDataMap[listProperties.id] = resourceDataList
      }
      if (listProperties.practitionerList.isNotEmpty()) {
        val resourceDataList: List<ResourceData> =
          listProperties.resources.flatMap { resource ->
            filteredResourcesPerPractitioner(
              relatedResourcesMap,
              resource
            )
              .mapToResourceData(
                relatedResourcesMap = relatedResourcesMap,
                ruleConfigs = listProperties.registerCard.rules,
                listRelatedResources = resource.relatedResources,
                computedValuesMap = computedValuesMap
              )
          }
        resourceDataMap[listProperties.id] = resourceDataList
      }
    }
    return resourceDataMap
  }

  private fun List<Resource>.mapToResourceData(
    relatedResourcesMap: MutableMap<String, MutableList<Resource>>,
    ruleConfigs: List<RuleConfig>,
    listRelatedResources: List<ExtractedResource>,
    computedValuesMap: Map<String, Any>
  ) =
    this.map { resource ->
      val listItemRelatedResources =
        listRelatedResources.associate { (id, resourceType, fhirPathExpression) ->
          (id
            ?: resourceType.name) to
            rulesFactory.rulesEngineService.retrieveRelatedResources(
              resource = resource,
              relatedResourceKey = id ?: resourceType.name,
              referenceFhirPathExpression = fhirPathExpression,
              relatedResourcesMap = relatedResourcesMap
            )
        }

      // Values computed from the rules defined in LIST view RegisterCard
      val listComputedValuesMap =
        rulesFactory.fireRule(
          ruleConfigs = ruleConfigs,
          baseResource = resource,
          relatedResourcesMap = listItemRelatedResources
        )

      // LIST view should reuse the previously computed values
      ResourceData(
        baseResourceId = resource.logicalId.extractLogicalIdUuid(),
        baseResourceType = resource.resourceType,
        computedValuesMap = computedValuesMap.plus(listComputedValuesMap),
        listResourceDataMap = emptyMap()
      )
    }

  /**
   * This function returns a list of filtered resources. The required list is obtained from
   * [relatedResourceMap], then a filter is applied based on the condition returned from the
   * extraction of the [ListResource] conditional FHIR path expression
   */
  private fun filteredListResources(
    relatedResourceMap: MutableMap<String, MutableList<Resource>>,
    listResource: ListResource
  ): MutableList<Resource> {
    val relatedResourceKey = listResource.relatedResourceId ?: listResource.resourceType.name
    val newListRelatedResources = relatedResourceMap[relatedResourceKey]

    // conditionalFhirPath expression e.g. "Task.status == 'ready'" to filter tasks that are due
    if (newListRelatedResources != null &&
        !listResource.conditionalFhirPathExpression.isNullOrEmpty()
    ) {
      return rulesFactory
        .rulesEngineService
        .filterResources(
          resources = newListRelatedResources,
          fhirPathExpression = listResource.conditionalFhirPathExpression
        )
        .toMutableList()
    }

    return newListRelatedResources ?: mutableListOf()
  }

  /**
   * This function returns a list of filtered resources. The required list is obtained from
   * [relatedResourceMap], then a filter is applied based on the condition returned from the
   * extraction of the [ListResource] conditional FHIR path expression
   */
  private fun filteredResourcesPerPractitioner(
    relatedResourceMap: MutableMap<String, MutableList<Resource>>,
    listResource: ListResource
  ): MutableList<Resource> {
    val relatedResourceKey = listResource.relatedResourceId ?: listResource.resourceType.name
    val resultingPractitionerFilteredResource = relatedResourceMap[relatedResourceKey]

    // conditionalFhirPath expression e.g. "Task.status == 'ready'" to filter tasks that are due
    if (resultingPractitionerFilteredResource != null && !listResource.practitionerFhirPathExpression.isNullOrEmpty()
    ) {
      return rulesFactory
        .rulesEngineService
        .filterResources(
          resources = resultingPractitionerFilteredResource,
          fhirPathExpression = listResource.practitionerFhirPathExpression
        )
        .toMutableList()
    }

    return resultingPractitionerFilteredResource ?: mutableListOf()
  }

  /**
   * This function obtains all [ListProperties] from the [ViewProperties] list; including the nested
   * LISTs
   */
  private fun List<ViewProperties>.retrieveListProperties(): List<ListProperties> {
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
   * This function creates a map of resource config Id ( or resource type if the id is not
   * configured) against [Resource] from a list of nested [RelatedResourceData].
   *
   * Example: A list of [RelatedResourceData] with Patient as its base resource and two nested
   * [RelatedResourceData] of resource type Condition & CarePlan returns:
   * ```
   * {
   * "Patient" -> [Patient],
   * "Condition" -> [Condition],
   * "CarePlan" -> [CarePlan]
   * }
   * ```
   *
   * NOTE: [RelatedResourceData] are represented as tree however they grouped by their resource
   * config Id ( or resource type if the id is not configured) as key and value as list of
   * [Resource] s in the map.
   */
  private fun LinkedList<RelatedResourceData>.createRelatedResourcesMap():
    MutableMap<String, MutableList<Resource>> {
    val relatedResourcesMap = mutableMapOf<String, MutableList<Resource>>()
    while (this.isNotEmpty()) {
      val relatedResourceData = this.removeFirst()
      relatedResourcesMap
        .getOrPut(
          relatedResourceData.resourceConfigId ?: relatedResourceData.resource.resourceType.name
        ) { mutableListOf() }
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
        relatedResourcesData.addLast(
          RelatedResourceData(resource = resource, resourceConfigId = resourceConfig.id)
        )
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
          relatedResourcesData.addLast(
            RelatedResourceData(resource = resource, resourceConfigId = resourceConfig.id)
          )
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
    currentPage: Int? = null,
    pageSize: Int? = null
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
        if (currentPage != null && pageSize != null) {
          count = pageSize
          from = currentPage * pageSize
        }
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
    fhirResourceConfig: FhirResourceConfig?,
    practitionerId: String?
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
    val secondaryResourceConfig = profileConfiguration.secondaryResources

    val baseResource: Resource =
      withContext(dispatcherProvider.io()) {
        fhirEngine.get(baseResourceType, resourceId.extractLogicalIdUuid())
      }

    val relatedResources = LinkedList<RelatedResourceData>()

    relatedResourcesConfig.forEach { config: ResourceConfig ->
      val resources =
        withContext(dispatcherProvider.io()) {
          searchRelatedResources(
            resourceConfig = config,
            baseResourceType = baseResourceType,
            baseResource = baseResource,
            fhirPathExpression = config.fhirPathExpression
          )
        }
      relatedResources.addAll(resources)
    }

    if (!secondaryResourceConfig.isNullOrEmpty()) {
      relatedResources.addAll(retrieveSecondaryResources(secondaryResourceConfig))
    }

    return processResourceData(
      relatedResources = relatedResources,
      baseResource = baseResource,
      views = profileConfiguration.views,
      rules = profileConfiguration.rules
    )
  }

  private suspend fun retrieveSecondaryResources(
    resourceConfigList: List<FhirResourceConfig>
  ): LinkedList<RelatedResourceData> {
    val relatedResourceData = LinkedList<RelatedResourceData>()

    resourceConfigList.map { fhirResourceConfig: FhirResourceConfig ->
      val baseResources: List<Resource> =
        withContext(dispatcherProvider.io()) {
          searchResource(
            baseResourceClass = fhirResourceConfig.baseResource.resource.resourceClassType(),
            dataQueries = fhirResourceConfig.baseResource.dataQueries,
            sortConfigs = fhirResourceConfig.baseResource.sortConfigs
          )
        }

      baseResources.map { baseResource: Resource ->
        val baseRelatedResourceList = LinkedList<RelatedResourceData>()
        fhirResourceConfig.relatedResources.forEach { resourceConfig: ResourceConfig ->
          val currentRelatedResources =
            withContext(dispatcherProvider.io()) {
              searchRelatedResources(
                resourceConfig = resourceConfig,
                baseResourceType = baseResource.resourceType,
                baseResource = baseResource,
                fhirPathExpression = resourceConfig.fhirPathExpression
              )
            }
          baseRelatedResourceList.addAll(currentRelatedResources)
        }
        relatedResourceData.add(
          RelatedResourceData(
            baseResource,
            baseRelatedResourceList,
            fhirResourceConfig.baseResource.id
          )
        )
      }
    }

    return relatedResourceData
  }

  fun retrieveRegisterConfiguration(registerId: String): RegisterConfiguration =
    configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId)

  companion object {
    const val ACTIVE = "active"
  }
}
