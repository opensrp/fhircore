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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.flowlayout.FlowColumn
import com.google.accompanist.flowlayout.FlowRow
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.HorizontalViewArrangement
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
import org.smartregister.fhircore.quest.util.extensions.conditional

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
  if (viewProperties.isEmpty()) return
  viewProperties.forEach { properties ->
    if (properties is ViewGroupProperties) {
      RenderViewGroup(
        viewProperties = properties,
        resourceData = resourceData,
        navController = navController
      )
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
  viewProperties: ViewGroupProperties,
  resourceData: ResourceData,
  navController: NavController
) {
  val modifier =
    Modifier.conditional(viewProperties.fillMaxSize, { fillMaxSize() })
      .conditional(viewProperties.fillMaxWidth, { fillMaxWidth() })
      .conditional(viewProperties.fillMaxHeight, { fillMaxHeight() })
      .background(
        viewProperties.backgroundColor?.interpolate(resourceData.computedValuesMap).parseColor()
      )
      .clip(RoundedCornerShape(viewProperties.borderRadius.dp))
      .padding(viewProperties.padding.dp)

  // NOTE: Do not use th same modifier in the nested ViewRender call. Use a new Modifier for every
  // ViewRenderer so as not to use the same styling for the Column and Row components
  when (viewProperties.viewType) {
    ViewType.COLUMN -> {
      if (viewProperties.wrapContent) {
        FlowColumn(modifier = modifier) {
          ViewRenderer(
            viewProperties = viewProperties.children,
            resourceData = resourceData,
            navController = navController
          )
        }
      } else {
        Column(
          modifier = modifier,
          verticalArrangement = viewProperties.verticalArrangement?.position ?: Arrangement.Top
        ) {
          ViewRenderer(
            viewProperties = viewProperties.children,
            resourceData = resourceData,
            navController = navController
          )
        }
      }
    }
    ViewType.ROW -> {
      if (viewProperties.wrapContent) {
        FlowRow(modifier = modifier) {
          ViewRenderer(
            viewProperties = viewProperties.children,
            resourceData = resourceData,
            navController = navController
          )
        }
      } else {
        Row(
          modifier = modifier,
          horizontalArrangement = viewProperties.horizontalArrangement?.position
              ?: Arrangement.Start
        ) {
          ViewRenderer(
            viewProperties = viewProperties.children,
            resourceData = resourceData,
            navController = navController
          )
        }
      }
    }
    else -> return
  }
}

@Composable
private fun RenderChildView(
  modifier: Modifier,
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
    is PersonalDataProperties ->
      PersonalDataView(
        personalDataCardProperties = viewProperties,
        computedValuesMap = resourceData.computedValuesMap
      )
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

@Preview(showBackground = true)
@Composable
private fun RenderNestedButtonsPreview() {
  val viewProperties =
    listOf<ViewProperties>(
      ViewGroupProperties(
        viewType = ViewType.COLUMN,
        fillMaxWidth = true,
        children =
          listOf(
            CompoundTextProperties(
              primaryText = "Nelson Mandela",
              primaryTextColor = "#000000",
            ),
            CompoundTextProperties(
              primaryText = "ANC  ~ Presidential Candidate",
              primaryTextColor = "#5A5A5A",
            ),
            ViewGroupProperties(
              viewType = ViewType.ROW,
              fillMaxWidth = true,
              children =
                listOf(
                  ButtonProperties(status = "OVERDUE", text = "Revoke", fillMaxWidth = false),
                  ButtonProperties(status = "COMPLETED", text = "Elected", fillMaxWidth = false)
                ),
              horizontalArrangement = HorizontalViewArrangement.SPACE_AROUND
            )
          )
      )
    )

  ViewRenderer(
    viewProperties = viewProperties,
    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
    navController = rememberNavController()
  )
}

@Preview(showBackground = true)
@Composable
private fun RenderNestedCompoundTextsPreview() {
  ViewRenderer(
    viewProperties =
      listOf(
        ViewGroupProperties(
          viewType = ViewType.ROW,
          fillMaxWidth = true,
          wrapContent = true,
          children =
            listOf(
              CompoundTextProperties(
                primaryText = "Janet Wix",
                primaryTextColor = "#000000",
              ),
              CompoundTextProperties(
                primaryText = "Last visited",
                primaryTextColor = "#5A5A5A",
                secondaryText = "Yesterday",
                secondaryTextColor = "#FFFFFF",
                separator = "-",
                secondaryTextBackgroundColor = "#BFA500",
              ),
              CompoundTextProperties(
                primaryText = "LMP",
                primaryTextColor = "#5A5A5A",
                secondaryText = "02-01-2022",
                secondaryTextColor = "#FFFFFF",
                separator = "-",
                secondaryTextBackgroundColor = "#CFA450",
              ),
              CompoundTextProperties(
                primaryText = "EDD",
                primaryTextColor = "#5A5A5A",
                secondaryText = "03-09-2022",
                secondaryTextColor = "#FFFFFF",
                separator = "-",
                secondaryTextBackgroundColor = "#DFA400",
              )
            ),
          horizontalArrangement = HorizontalViewArrangement.SPACE_EVENLY
        )
      ),
    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
    navController = rememberNavController()
  )
}
