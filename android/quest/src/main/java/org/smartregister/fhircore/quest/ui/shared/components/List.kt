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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor

@Composable
fun List(
  modifier: Modifier,
  viewProperties: ListProperties,
  resourceData: ResourceData,
  navController: NavController,
  viewModel: ViewRendererViewModel = hiltViewModel(),
) {
  val resources = resourceData.relatedResourcesMap[viewProperties.baseResource]
  Column(
    modifier =
      modifier
        .background(
          viewProperties.backgroundColor.interpolate(resourceData.computedValuesMap).parseColor()
        )
        .padding(
          horizontal = viewProperties.padding.dp,
          vertical = viewProperties.padding.div(4).dp
        )
  ) {
    resources?.forEachIndexed { index, resource ->
      // Retrieve available related resources proceed to compute rules
      val listItemResourceData =
        produceState(ResourceData(resource)) {
            val newRelatedResources =
              viewProperties.relatedResources.associate {
                Pair(
                  it.resourceType,
                  viewModel.rulesFactory.rulesEngineService.retrieveRelatedResources(
                    resource = resource,
                    relatedResourceType = it.resourceType,
                    fhirPathExpression = it.fhirPathExpression,
                    resourceData = resourceData
                  )
                )
              }
            val computedValuesMap =
              viewModel.rulesFactory.fireRule(
                ruleConfigs = viewProperties.registerCard.rules,
                baseResource = resource,
                relatedResourcesMap = newRelatedResources
              )

            value = ResourceData(resource, newRelatedResources, computedValuesMap)
          }
          .value

      Column {
        Spacer(modifier = modifier.height(5.dp))
        ViewRenderer(
          viewProperties = viewProperties.registerCard.views,
          resourceData = listItemResourceData,
          navController = navController,
        )
        Spacer(modifier = modifier.height(5.dp))
        if ((index < resources.lastIndex) && viewProperties.showDivider)
          Divider(color = DividerColor)
      }
    }
  }
}
