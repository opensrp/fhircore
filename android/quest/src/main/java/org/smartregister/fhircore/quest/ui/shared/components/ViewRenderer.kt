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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowColumn
import com.google.accompanist.flowlayout.FlowRow
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.PersonalDataProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.ViewGroupProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.quest.ui.profile.components.PersonalDataView
import org.smartregister.fhircore.quest.ui.register.components.ServiceCard
import org.smartregister.fhircore.quest.ui.shared.models.ViewComponentEvent

/**
 * This function takes a list of [ViewProperties] and build views recursively as configured in the
 * properties. The content used in the views is provided via [ResourceData] class. The card also has
 * an [onViewComponentClick] listener that responds to the click events.
 *
 * Note that by default the view render is not rendered in a view group like a Column/Row. This is
 * to allow us to call the function recursively for nested view group layout. Therefore when using
 * the this layout, provide a parent layout (usually Row/Column) so that the views can be rendered
 * appropriately otherwise the generated view group will be rendered one on top of the other.
 */
@Composable
fun ViewRenderer(
  modifier: Modifier = Modifier,
  viewProperties: List<ViewProperties>,
  resourceData: ResourceData,
  onViewComponentClick: (ViewComponentEvent) -> Unit
) {
  viewProperties.forEach { properties ->
    // Render views recursively
    if (properties is ViewGroupProperties) {
      if (properties.children.isEmpty()) return
      when (properties.viewType) {
        ViewType.COLUMN, ViewType.ROW ->
          RenderViewGroup(modifier, properties, resourceData, onViewComponentClick)
        else -> return
      }
    } else {
      RenderChildView(
        modifier = modifier,
        viewProperties = properties,
        resourceData = resourceData,
        onViewComponentClick = onViewComponentClick
      )
    }
  }
}

@Composable
private fun RenderViewGroup(
  modifier: Modifier = Modifier,
  viewProperties: ViewGroupProperties,
  resourceData: ResourceData,
  onViewComponentClick: (ViewComponentEvent) -> Unit
) {
  viewProperties.children.forEach { childViewProperty ->
    if (childViewProperty is ViewGroupProperties) {
      if (childViewProperty.viewType == ViewType.COLUMN) {
        FlowColumn {
          ViewRenderer(
            modifier = modifier,
            viewProperties = childViewProperty.children,
            resourceData = resourceData,
            onViewComponentClick = onViewComponentClick,
          )
        }
      } else if (childViewProperty.viewType == ViewType.ROW) {
        FlowRow {
          ViewRenderer(
            modifier = modifier,
            viewProperties = childViewProperty.children,
            resourceData = resourceData,
            onViewComponentClick = onViewComponentClick,
          )
        }
      }
    }
    RenderChildView(
      modifier = modifier,
      viewProperties = childViewProperty,
      resourceData = resourceData,
      onViewComponentClick = onViewComponentClick
    )
  }
}

@Composable
private fun RenderChildView(
  modifier: Modifier = Modifier,
  viewProperties: ViewProperties,
  resourceData: ResourceData,
  onViewComponentClick: (ViewComponentEvent) -> Unit
) {
  when (viewProperties) {
    is CompoundTextProperties ->
      CompoundText(
        modifier = modifier,
        compoundTextProperties = viewProperties,
        computedValuesMap = resourceData.computedValuesMap,
      )
    is ServiceCardProperties ->
      ServiceCard(
        serviceCardProperties = viewProperties,
        resourceData = resourceData,
        onViewComponentClick = onViewComponentClick
      )
    is CardViewProperties ->
      Card(
        elevation = viewProperties.elevation.dp,
        modifier =
          modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(viewProperties.cornerSize.dp))
            .padding(viewProperties.padding.dp)
      ) {
        Column {
          ViewRenderer(
            viewProperties = viewProperties.content,
            resourceData = resourceData,
            onViewComponentClick = onViewComponentClick
          )
        }
      }
    is PersonalDataProperties -> PersonalDataView(personalDataCardProperties = viewProperties)
  }
}
