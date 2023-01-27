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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.flowlayout.FlowColumn
import com.google.accompanist.flowlayout.FlowRow
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ListOrientation
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.R

@Composable
fun List(
  modifier: Modifier,
  viewProperties: ListProperties,
  resourceData: ResourceData,
  navController: NavController,
) {
  val currentListResourceData = resourceData.listResourceDataMap[viewProperties.id]

  Column(
    modifier =
    modifier
      .background(
        viewProperties.backgroundColor
          ?.interpolate(resourceData.computedValuesMap)
          .parseColor()
      )
      .padding(
        horizontal = viewProperties.padding.dp,
        vertical = viewProperties.padding.div(4).dp
      )
  ) {
    if (currentListResourceData.isNullOrEmpty()) {
      Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Text(
          text = viewProperties.emptyList?.message ?: stringResource(id = R.string.no_visits),
          modifier = modifier
            .padding(8.dp)
            .align(Alignment.Center),
          color = DefaultColor,
          fontStyle = FontStyle.Italic
        )
      }
    } else {

      when (viewProperties.orientation) {
        ListOrientation.VERTICAL ->
          Column {
            currentListResourceData.forEachIndexed { index, listResourceData ->
              DisplayListItem(
                modifier = modifier,
                viewProperties = viewProperties,
                listResourceData = listResourceData,
                navController = navController,
                index = index,
                currentListResourceData = currentListResourceData
              )
            }
          }
        ListOrientation.HORIZONTAL ->
          FlowRow {
            currentListResourceData.forEachIndexed { index, listResourceData ->
              DisplayListItem(
                modifier = modifier,
                viewProperties = viewProperties,
                listResourceData = listResourceData,
                navController = navController,
                index = index,
                currentListResourceData = currentListResourceData
              )
            }
          }
      }
    }
  }
}

@Composable
private fun DisplayListItem(
  modifier: Modifier,
  viewProperties: ListProperties,
  listResourceData: ResourceData,
  navController: NavController,
  index: Int,
  currentListResourceData: List<ResourceData>
) {
  Spacer(modifier = modifier.height(5.dp))
  Box {
    ViewRenderer(
      viewProperties = viewProperties.registerCard.views,
      resourceData = listResourceData,
      navController = navController,
    )
  }
  Spacer(modifier = modifier.height(5.dp))
  if (index < currentListResourceData.lastIndex &&
    viewProperties.showDivider &&
    viewProperties.orientation == ListOrientation.VERTICAL
  )
    Divider(color = DividerColor, thickness = 0.5.dp)
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun ListInWithRowsPreview() {
  Column {
    ViewRenderer(
      navController = rememberNavController(),
      resourceData =
      ResourceData(
        "id",
        ResourceType.CarePlan,
        mapOf("1" to "Family", "2" to "Home"),
        mapOf(
          "listId" to
                  listOf(
                    ResourceData(
                      baseResourceId = "1",
                      ResourceType.CarePlan,
                      mapOf(),
                      emptyMap(),
                    )
                  )
        )
      ),
      viewProperties =
      listOf(
        ListProperties(
          viewType = ViewType.LIST,
          orientation = ListOrientation.HORIZONTAL,
          id = "listId",
          padding = 10,
          borderRadius = 10,
          emptyList = NoResultsConfig(message = "No care Plans"),
          baseResource = ResourceType.CarePlan,
          fillMaxHeight = true,
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
              )
            ),
          )
        )
      )
    )
  }
}
