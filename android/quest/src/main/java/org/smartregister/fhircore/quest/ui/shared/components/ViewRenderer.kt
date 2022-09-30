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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowColumn
import com.google.accompanist.flowlayout.FlowRow
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.PersonalDataProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.SpacerProperties
import org.smartregister.fhircore.engine.configuration.view.ViewGroupProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.profile.components.PersonalDataView

/**
 * This function takes a list of [ViewProperties] and build views recursively as configured in the
 * properties. The content used in the views is provided via [ResourceData] class.
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
  navController: NavController
) {
  viewProperties.forEach { properties ->
    // Render views recursively
    if (properties is ViewGroupProperties) {
      if (properties.children.isEmpty()) return
      when (properties.viewType) {
        ViewType.COLUMN, ViewType.ROW ->
          RenderViewGroup(
            modifier = modifier,
            viewProperties = properties,
            resourceData = resourceData,
            navController = navController
          )
        else -> return
      }
    } else {
      RenderChildView(
        modifier = modifier,
        viewProperties = properties,
        resourceData = resourceData,
        navController = navController
      )
    }
  }
}

@Composable
private fun RenderViewGroup(
  modifier: Modifier = Modifier,
  viewProperties: ViewGroupProperties,
  resourceData: ResourceData,
  navController: NavController
) {

  viewProperties.children.forEach { childViewProperty ->
    if (childViewProperty is ViewGroupProperties) {
      if (childViewProperty.viewType == ViewType.COLUMN) {
        FlowColumn(
          modifier =
            modifier
              .background(
                childViewProperty
                  .backgroundColor
                  ?.interpolate(resourceData.computedValuesMap)
                  .parseColor()
              )
              .fillMaxWidth()
              .padding(childViewProperty.padding.dp)
        ) {
          ViewRenderer(
            modifier = modifier,
            viewProperties = childViewProperty.children,
            resourceData = resourceData,
            navController = navController
          )
        }
      } else if (childViewProperty.viewType == ViewType.ROW) {
        FlowRow(
          modifier =
            modifier
              .background(
                childViewProperty
                  .backgroundColor
                  ?.interpolate(resourceData.computedValuesMap)
                  .parseColor()
              )
              .fillMaxWidth()
              .padding(childViewProperty.padding.dp)
        ) {
          ViewRenderer(
            modifier = modifier,
            viewProperties = childViewProperty.children,
            resourceData = resourceData,
            navController = navController
          )
        }
      }
    }
    RenderChildView(
      modifier = modifier,
      viewProperties = childViewProperty,
      resourceData = resourceData,
      navController = navController
    )
  }
}

@Composable
private fun RenderChildView(
  modifier: Modifier = Modifier,
  viewProperties: ViewProperties,
  resourceData: ResourceData,
  navController: NavController,
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
        navController = navController
      )
    is CardViewProperties ->
      CardView(
        modifier = modifier,
        viewProperties = viewProperties,
        resourceData = resourceData,
        navController = navController
      )
    is PersonalDataProperties -> PersonalDataView(personalDataCardProperties = viewProperties)
    is ButtonProperties ->
      ActionableButton(
        buttonProperties = viewProperties,
        navController = navController,
        resourceData = resourceData
      )
    is SpacerProperties -> SpacerView(spacerProperties = viewProperties)
    is ListProperties ->
      List(
        modifier = modifier,
        viewProperties = viewProperties,
        resourceData = resourceData,
        navController = navController,
      )
  }
}
