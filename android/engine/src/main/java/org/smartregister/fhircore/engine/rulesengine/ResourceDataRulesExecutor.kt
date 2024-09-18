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

package org.smartregister.fhircore.engine.rulesengine

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.google.android.fhir.datacapture.extensions.logicalId
import javax.inject.Inject
import org.hl7.fhir.r4.model.Resource
import org.jeasy.rules.api.Facts
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.ListResource
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid

/**
 * This class is used to fire rules used to extract and manipulate data from FHIR resources.
 *
 * NOTE: that the [Facts] object is not thread safe, each thread should have its own set of data to
 * work on. When used in multi-threaded environment may exhibit unexpected behavior and return wrong
 * results when rules are fired. Use the [ResourceDataRulesExecutor] in the same coroutine context
 * of the caller.
 */
class ResourceDataRulesExecutor @Inject constructor(val rulesFactory: RulesFactory) {

  fun processResourceData(
    repositoryResourceData: RepositoryResourceData,
    ruleConfigs: List<RuleConfig>,
    params: Map<String, String>?,
  ): ResourceData {
    val computedValuesMap =
      computeResourceDataRules(
        ruleConfigs = ruleConfigs,
        repositoryResourceData = repositoryResourceData,
        params = params ?: emptyMap(),
      )
    return ResourceData(
      baseResourceId = repositoryResourceData.resource.logicalId.extractLogicalIdUuid(),
      baseResourceType = repositoryResourceData.resource.resourceType,
      computedValuesMap = computedValuesMap,
    )
  }

  /**
   * This function pre-computes all the Rules for [ViewType]'s of List including list nested in the
   * views. The LIST view computed values includes the parent's. Every list identified by
   * [ListProperties.id] is added to the [listResourceDataStateMap], where the value is
   * [SnapshotStateList] to ensure the items are rendered (incrementally) as they are added to the
   * list
   */
  fun processListResourceData(
    listProperties: ListProperties,
    relatedResourcesMap: Map<String, List<Resource>>,
    computedValuesMap: Map<String, Any>,
    listResourceDataStateMap: SnapshotStateMap<String, SnapshotStateList<ResourceData>>,
  ) {
    listProperties.resources.forEach { listResource ->
      // Initialize to be updated incrementally as resources are transformed into ResourceData
      val resourceDataSnapshotStateList = mutableStateListOf<ResourceData>()
      listResourceDataStateMap[listProperties.id] = resourceDataSnapshotStateList

      filteredListResources(relatedResourcesMap, listResource)
        .mapToResourceData(
          listResource = listResource,
          relatedResourcesMap = relatedResourcesMap,
          ruleConfigs = listProperties.registerCard.rules,
          computedValuesMap = computedValuesMap,
          resourceDataSnapshotStateList = resourceDataSnapshotStateList,
        )
    }
  }

  /**
   * This function computes rules based on the provided facts [RepositoryResourceData.resource],
   * [RepositoryResourceData.relatedResourcesMap] and
   * [RepositoryResourceData.relatedResourcesCountMap]. The function returns the outcome of the
   * computation in a map; the name of the rule is used as the key.
   */
  fun computeResourceDataRules(
    ruleConfigs: List<RuleConfig>,
    repositoryResourceData: RepositoryResourceData?,
    params: Map<String, String>,
  ): Map<String, Any> {
    return rulesFactory.fireRules(
      rules = rulesFactory.generateRules(ruleConfigs),
      repositoryResourceData = repositoryResourceData,
      params = params,
    )
  }

  private fun List<Resource>.mapToResourceData(
    listResource: ListResource,
    relatedResourcesMap: Map<String, List<Resource>>,
    ruleConfigs: List<RuleConfig>,
    computedValuesMap: Map<String, Any>,
    resourceDataSnapshotStateList: SnapshotStateList<ResourceData>,
  ) {
    this.forEach { resource ->
      val listItemRelatedResources = mutableMapOf<String, List<Resource>>()
      listResource.relatedResources.forEach { relatedListResource ->
        val retrieveRelatedResources: List<Resource>? =
          relatedListResource.fhirPathExpression.let {
            rulesFactory.rulesEngineService.retrieveRelatedResources(
              resource = resource,
              relatedResourceKey =
                relatedListResource.relatedResourceId ?: relatedListResource.resourceType.name,
              referenceFhirPathExpression = it,
              relatedResourcesMap = relatedResourcesMap,
            )
          }
        if (!retrieveRelatedResources.isNullOrEmpty()) {
          listItemRelatedResources[
            relatedListResource.id ?: relatedListResource.resourceType.name,
          ] =
            if (!relatedListResource.conditionalFhirPathExpression.isNullOrEmpty()) {
              rulesFactory.rulesEngineService.filterResources(
                retrieveRelatedResources,
                relatedListResource.conditionalFhirPathExpression,
              )
            } else {
              retrieveRelatedResources
            }
        }
      }

      val listComputedValuesMap =
        computeResourceDataRules(
          ruleConfigs = ruleConfigs,
          repositoryResourceData =
            RepositoryResourceData(
              resourceRulesEngineFactId = null,
              resource = resource,
              relatedResourcesMap = listItemRelatedResources,
            ),
          params = emptyMap(),
        )

      resourceDataSnapshotStateList.add(
        ResourceData(
          baseResourceId = resource.logicalId.extractLogicalIdUuid(),
          baseResourceType = resource.resourceType,
          computedValuesMap =
            computedValuesMap.plus(listComputedValuesMap), // Reuse computed values
        ),
      )
    }
  }

  /**
   * This function returns a list of filtered resources. The required list is obtained from
   * [relatedResourceMap], then a filter is applied based on the condition returned from the
   * extraction of the [ListResource] conditional FHIR path expression
   */
  private fun filteredListResources(
    relatedResourceMap: Map<String, List<Resource>>,
    listResource: ListResource,
  ): List<Resource> {
    val relatedResourceKey = listResource.relatedResourceId ?: listResource.resourceType.name
    val newListRelatedResources = relatedResourceMap[relatedResourceKey]

    // conditionalFhirPath expression e.g. "Task.status == 'ready'" to filter tasks that are due
    val resources =
      if (
        newListRelatedResources != null &&
          !listResource.conditionalFhirPathExpression.isNullOrEmpty()
      ) {
        rulesFactory.rulesEngineService.filterResources(
          resources = newListRelatedResources,
          conditionalFhirPathExpression = listResource.conditionalFhirPathExpression,
        )
      } else {
        newListRelatedResources ?: listOf()
      }

    val sortConfig = listResource.sortConfig

    // Sort resources if sort configuration is provided
    return if (sortConfig != null && sortConfig.fhirPathExpression.isNotEmpty()) {
      rulesFactory.rulesEngineService.sortResources(
        resources = resources,
        fhirPathExpression = sortConfig.fhirPathExpression,
        dataType = sortConfig.dataType.name,
        order = sortConfig.order.name,
      ) ?: resources
    } else {
      resources
    }
  }
}
