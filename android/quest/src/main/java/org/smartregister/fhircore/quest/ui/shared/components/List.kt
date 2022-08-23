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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.shared.models.ViewComponentEvent

@Composable
fun List(
  modifier: Modifier,
  resourceData: ResourceData,
  viewProperties: ListProperties,
  viewModel: ViewRendererViewModel,
  onViewComponentClick: (ViewComponentEvent) -> Unit
) {
  val resources = remember { resourceData.relatedResourcesMap[viewProperties.baseResource] }
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
      // Retrieve all the related resources from the already provided resource data
      val relatedResources =
        produceState(initialValue = emptyMap()) {
            value =
              viewProperties.relatedResources.associate {
                Pair(
                  it.resourceType,
                  viewModel.rulesFactory.rulesEngineService.retrieveRelatedResources(
                    resource = resource,
                    relatedResourceType = it.resourceType,
                    fhirPathExpression = it.fhirPathExpression
                  )
                )
              }
          }
          .value

      // Fire rules engine to compute values
      val computedValuesMap =
        produceState<Map<String, Any>>(initialValue = emptyMap()) {
            value =
              viewModel.rulesFactory.fireRule(
                viewProperties.registerCard.rules,
                resource,
              )
          }
          .value

      Column {
        Spacer(modifier = modifier.height(8.dp))
        ViewRenderer(
          viewProperties = viewProperties.registerCard.views,
          resourceData = ResourceData(resource, relatedResources, computedValuesMap),
          onViewComponentClick = onViewComponentClick,
        )
        Spacer(modifier = modifier.height(8.dp))
        if ((index < resources.lastIndex) && viewProperties.showDivider)
          Divider(color = DividerColor)
      }
    }
  }
}
