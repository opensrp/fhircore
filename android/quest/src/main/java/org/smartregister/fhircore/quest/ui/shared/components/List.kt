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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.flowlayout.FlowRow
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ListOrientation
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.ListResource
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.util.extensions.conditional

const val VERTICAL_ORIENTATION = "verticalOrientation"
const val HORIZONTAL_ORIENTATION = "horizontalOrientation"

@Composable
fun List(
  modifier: Modifier,
  viewProperties: ListProperties,
  resourceData: ResourceData,
  navController: NavController,
) {
  val currentListResourceData = resourceData.listResourceDataMap?.get(viewProperties.id)
  if (currentListResourceData.isNullOrEmpty()) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
      Text(
        text = viewProperties.emptyList?.message ?: "",
        modifier = modifier.padding(8.dp).align(Alignment.Center),
        color = DefaultColor,
        fontStyle = FontStyle.Italic
      )
    }
  } else {
    Box(
      modifier =
        modifier
          .background(
            viewProperties.backgroundColor?.interpolate(resourceData.computedValuesMap).parseColor()
          )
          .testTag(VERTICAL_ORIENTATION)
    ) {
      when (viewProperties.orientation) {
        ListOrientation.VERTICAL ->
          Column(
            modifier =
              modifier
                .conditional(viewProperties.fillMaxWidth, { fillMaxWidth() })
                .conditional(viewProperties.fillMaxHeight, { fillMaxHeight() })
                .testTag(VERTICAL_ORIENTATION)
          ) {
            currentListResourceData.forEachIndexed { index, listResourceData ->
              Spacer(modifier = modifier.height(6.dp))
              Column(
                modifier =
                  Modifier.padding(
                    horizontal = viewProperties.padding.dp,
                    vertical = viewProperties.padding.div(4).dp
                  )
              ) {
                ViewRenderer(
                  viewProperties = viewProperties.registerCard.views,
                  resourceData = listResourceData,
                  navController = navController,
                )
              }
              Spacer(modifier = modifier.height(6.dp))
              if (index < currentListResourceData.lastIndex && viewProperties.showDivider)
                Divider(color = DividerColor, thickness = 0.5.dp)
            }
          }
        ListOrientation.HORIZONTAL ->
          FlowRow(modifier = modifier.fillMaxWidth().testTag(HORIZONTAL_ORIENTATION)) {
            currentListResourceData.forEachIndexed { _, listResourceData ->
              ViewRenderer(
                viewProperties = viewProperties.registerCard.views,
                resourceData = listResourceData,
                navController = navController,
              )
            }
          }
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ListWithHorizontalOrientationPreview() {
  Column(modifier = Modifier.fillMaxWidth()) {
    List(
      modifier = Modifier,
      viewProperties =
        ListProperties(
          viewType = ViewType.LIST,
          orientation = ListOrientation.HORIZONTAL,
          backgroundColor = "#FFFFFF",
          fillMaxWidth = true,
          id = "listId",
          padding = 8,
          borderRadius = 10,
          emptyList = NoResultsConfig(message = "No care Plans"),
          resources =
            listOf(ListResource(id = "carePlanList", resourceType = ResourceType.CarePlan)),
          fillMaxHeight = true,
          registerCard =
            RegisterCardConfig(
              views =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Malaria",
                    primaryTextColor = "#DF0E1A",
                    primaryTextBackgroundColor = "#F9CFD1",
                    padding = 8,
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "ANC Danger Signs",
                    primaryTextColor = "#D2760D",
                    primaryTextBackgroundColor = "#FFECD6",
                    padding = 8
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "TB Danger Signs",
                    primaryTextColor = "#D2760D",
                    primaryTextBackgroundColor = "#FFECD6",
                    padding = 8
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "HIV Danger Signs",
                    primaryTextColor = "#D2760D",
                    primaryTextBackgroundColor = "#FFECD6",
                    padding = 8,
                  ),
                ),
            )
        ),
      navController = rememberNavController(),
      resourceData =
        ResourceData(
          baseResourceId = "baseId",
          baseResourceType = ResourceType.Patient,
          computedValuesMap = emptyMap()
        )
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ListWithVerticalOrientationPreview() {
  Column(modifier = Modifier.fillMaxWidth()) {
    List(
      modifier = Modifier,
      viewProperties =
        ListProperties(
          viewType = ViewType.LIST,
          backgroundColor = "#FCFCFC",
          orientation = ListOrientation.VERTICAL,
          id = "listId",
          padding = 8,
          borderRadius = 10,
          emptyList = NoResultsConfig(message = "No care Plans"),
          resources =
            listOf(ListResource(id = "carePlanList", resourceType = ResourceType.CarePlan)),
          fillMaxWidth = true,
          registerCard =
            RegisterCardConfig(
              views =
                listOf(
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Family Planning",
                    primaryTextColor = "#508BE8",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "Malaria",
                    primaryTextColor = "#508BE8",
                  ),
                  CompoundTextProperties(
                    viewType = ViewType.COMPOUND_TEXT,
                    primaryText = "HIV",
                    primaryTextColor = "#508BE8",
                  )
                ),
            ),
        ),
      navController = rememberNavController(),
      resourceData =
        ResourceData(
          baseResourceId = "baseId",
          baseResourceType = ResourceType.Patient,
          computedValuesMap = emptyMap()
        )
    )
  }
}
