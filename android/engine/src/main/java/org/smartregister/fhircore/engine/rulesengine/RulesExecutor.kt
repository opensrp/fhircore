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
import org.smartregister.fhircore.engine.configuration.view.TabViewProperties
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

class RulesExecutor @Inject constructor(val rulesFactory: RulesFactory) {

  suspend fun processResourceData(
    repositoryResourceData: RepositoryResourceData,
    ruleConfigs: List<RuleConfig>,
    params: Map<String, String>?
  ): ResourceData {
    val computedValuesMap =
      computeResourceDataRules(
        ruleConfigs = ruleConfigs,
        repositoryResourceData = repositoryResourceData
      )
    return ResourceData(
      baseResourceId = repositoryResourceData.resource.logicalId.extractLogicalIdUuid(),
      baseResourceType = repositoryResourceData.resource.resourceType,
      computedValuesMap =
        if (params != null) computedValuesMap.plus(params).toMap() else computedValuesMap.toMap()
    )
  }

  /**
   * This function pre-computes all the Rules for [ViewType]'s of List including list nested in the
   * views. The LIST view computed values includes the parent's.
   */
  suspend fun processListResourceData(
    listProperties: ListProperties,
    relatedResourcesMap: Map<String, List<Resource>>,
    computedValuesMap: Map<String, Any>,
  ): List<ResourceData> {
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

  /**
   * This function computes rules based on the provided facts [RepositoryResourceData.resource],
   * [RepositoryResourceData.relatedResourcesMap] and
   * [RepositoryResourceData.relatedResourcesCountMap]. The function returns the outcome of the
   * computation in a map; the name of the rule is used as the key.
   */
  private suspend fun computeResourceDataRules(
    ruleConfigs: List<RuleConfig>,
    repositoryResourceData: RepositoryResourceData
  ): Map<String, Any> {
    return rulesFactory.fireRules(
      rules = rulesFactory.generateRules(ruleConfigs),
      repositoryResourceData = repositoryResourceData
    )
  }

  private suspend fun List<Resource>.mapToResourceData(
    relatedResourcesMap: Map<String, List<Resource>>,
    ruleConfigs: List<RuleConfig>,
    listRelatedResources: List<ExtractedResource>,
    computedValuesMap: Map<String, Any>
  ) =
    this.map { resource ->
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
        computeResourceDataRules(
          ruleConfigs = ruleConfigs,
          repositoryResourceData =
            RepositoryResourceData(
              resourceRulesEngineFactId = null,
              resource = resource,
              relatedResourcesMap = listItemRelatedResources
            )
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
    relatedResourceMap: Map<String, List<Resource>>,
    listResource: ListResource
  ): List<Resource> {
    val relatedResourceKey = listResource.relatedResourceId ?: listResource.resourceType.name
    val newListRelatedResources = relatedResourceMap[relatedResourceKey]

    // conditionalFhirPath expression e.g. "Task.status == 'ready'" to filter tasks that are due
    if (newListRelatedResources != null &&
        !listResource.conditionalFhirPathExpression.isNullOrEmpty()
    ) {
      return rulesFactory.rulesEngineService.filterResources(
        resources = newListRelatedResources,
        fhirPathExpression = listResource.conditionalFhirPathExpression
      )
    }

    return newListRelatedResources ?: listOf()
  }
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
      ViewType.TABS -> viewPropertiesLinkedList.addAll((properties as TabViewProperties).tabContents)
      ViewType.LIST ->
        viewPropertiesLinkedList.addAll((properties as ListProperties).registerCard.views)
      else -> {}
    }
  }
  return listProperties
}
