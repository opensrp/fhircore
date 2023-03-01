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

package org.smartregister.fhircore.engine.rulesengine

import com.google.android.fhir.logicalId
import java.util.LinkedList
import javax.inject.Inject
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.ListResource
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ExtractedResource
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.pmap

class RulesExecutor @Inject constructor(val rulesFactory: RulesFactory) {

  suspend fun processResourceData(
    baseResource: Resource,
    relatedRepositoryResourceData: LinkedList<RepositoryResourceData>,
    ruleConfigs: List<RuleConfig>
  ): ResourceData {
    val relatedResourcesMap = relatedRepositoryResourceData.createRelatedResourcesMap()
    val computedValuesMap =
      computeRules(
        ruleConfigs = ruleConfigs,
        baseResource = baseResource,
        relatedResourcesMap = relatedResourcesMap
      )
    return ResourceData(
      baseResourceId = baseResource.logicalId.extractLogicalIdUuid(),
      baseResourceType = baseResource.resourceType,
      computedValuesMap = computedValuesMap
    )
  }

  /**
   * This function pre-computes all the Rules for [ViewType]'s of List including list nested in the
   * views. The LIST view computed values includes the parent's.
   */
  suspend fun processListResourceData(
    listProperties: ListProperties,
    relatedRepositoryResourceData: LinkedList<RepositoryResourceData>,
    computedValuesMap: Map<String, Any>
  ): List<ResourceData> {
    val relatedResourcesMap = relatedRepositoryResourceData.createRelatedResourcesMap()
    return listProperties.resources.flatMap { listResource ->
      filteredListResources(relatedResourcesMap, listResource)
        .mapToResourceData(
          relatedResourcesMap = relatedResourcesMap,
          ruleConfigs = listProperties.registerCard.rules,
          listRelatedResources = listResource.relatedResources,
          computedValuesMap = computedValuesMap
        )
    }
  }

  private suspend fun computeRules(
    ruleConfigs: List<RuleConfig>,
    baseResource: Resource,
    relatedResourcesMap: Map<String, List<Resource>>
  ): Map<String, Any> =
    // Compute values via rules engine and return a map. Rule names MUST be unique
    rulesFactory.fireRules(
      rules = rulesFactory.generateRules(ruleConfigs),
      baseResource = baseResource,
      relatedResourcesMap = relatedResourcesMap
    )

  private suspend fun List<Resource>.mapToResourceData(
    relatedResourcesMap: MutableMap<String, MutableList<Resource>>,
    ruleConfigs: List<RuleConfig>,
    listRelatedResources: List<ExtractedResource>,
    computedValuesMap: Map<String, Any>
  ) =
    this.pmap { resource ->
      val listItemRelatedResources: Map<String, List<Resource>> =
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

      val listComputedValuesMap =
        computeRules(
          ruleConfigs = ruleConfigs,
          baseResource = resource,
          relatedResourcesMap = listItemRelatedResources
        )

      // LIST view should reuse the previously computed values
      ResourceData(
        baseResourceId = resource.logicalId.extractLogicalIdUuid(),
        baseResourceType = resource.resourceType,
        computedValuesMap = computedValuesMap.plus(listComputedValuesMap)
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
}

/**
 * This function creates a map of resource config Id ( or resource type if the id is not configured)
 * against [Resource] from a list of nested [RepositoryResourceData].
 *
 * Example: A list of [RepositoryResourceData] with Patient as its base resource and two nested
 * [RepositoryResourceData] of resource type Condition & CarePlan returns:
 * ```
 * {
 * "Patient" -> [Patient],
 * "Condition" -> [Condition],
 * "CarePlan" -> [CarePlan]
 * }
 * ```
 *
 * NOTE: [RepositoryResourceData] are represented as tree however they grouped by their resource
 * config Id ( or resource type if the id is not configured) as key and value as list of [Resource]
 * s in the map.
 */
fun LinkedList<RepositoryResourceData>.createRelatedResourcesMap():
  MutableMap<String, MutableList<Resource>> {
  val relatedResourcesMap = mutableMapOf<String, MutableList<Resource>>()
  while (this.isNotEmpty()) {
    val relatedResourceData = this.removeFirst()
    relatedResourcesMap
      .getOrPut(relatedResourceData.configId ?: relatedResourceData.resource.resourceType.name) {
        mutableListOf()
      }
      .add(relatedResourceData.resource)
    relatedResourceData.relatedResources.forEach { this.addLast(it) }
  }
  return relatedResourcesMap
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
      ViewType.COLUMN -> viewPropertiesLinkedList.addAll((properties as ColumnProperties).children)
      ViewType.ROW -> viewPropertiesLinkedList.addAll((properties as RowProperties).children)
      ViewType.CARD -> viewPropertiesLinkedList.addAll((properties as CardViewProperties).content)
      ViewType.LIST ->
        viewPropertiesLinkedList.addAll((properties as ListProperties).registerCard.views)
      else -> {}
    }
  }
  return listProperties
}
