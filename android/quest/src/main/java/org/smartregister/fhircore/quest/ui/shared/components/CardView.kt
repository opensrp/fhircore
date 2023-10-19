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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.TextCase
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.util.extensions.conditional

@Composable
fun CardView(
  modifier: Modifier = Modifier,
  viewProperties: CardViewProperties,
  resourceData: ResourceData,
  navController: NavController,
) {
  val headerActionVisible = viewProperties.headerAction?.visible.toBoolean()
  Column(modifier = modifier.background(viewProperties.headerBackgroundColor.parseColor())) {
    // Header section
    Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      if (viewProperties.header != null) {
        CompoundText(
          modifier =
            modifier
              .conditional(headerActionVisible, { weight(if (headerActionVisible) 0.6f else 1f) })
              .wrapContentWidth(Alignment.Start),
          compoundTextProperties = viewProperties.header!!.copy(textCase = TextCase.UPPER_CASE),
          resourceData = resourceData,
          navController = navController,
        )
        if (viewProperties.headerAction != null && headerActionVisible) {
          CompoundText(
            modifier = modifier.wrapContentWidth(Alignment.End).weight(0.4f),
            compoundTextProperties = viewProperties.headerAction!!,
            resourceData = resourceData,
            navController = navController,
          )
        }
      }
    }
    // Card section
    Card(
      elevation = viewProperties.elevation.dp,
      modifier =
        modifier
          .padding(
            start = viewProperties.padding.dp,
            end = viewProperties.padding.dp,
            bottom = viewProperties.padding.dp,
          )
          .fillMaxWidth()
          .clip(RoundedCornerShape(viewProperties.cornerSize.dp)),
    ) {
      Column(modifier = modifier.padding(viewProperties.contentPadding.dp)) {
        ViewRenderer(
          viewProperties = viewProperties.content,
          resourceData = resourceData,
          navController = navController,
        )
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun CardViewWithoutPaddingPreview() {
  Column(modifier = Modifier.fillMaxWidth()) {
    CardView(
      viewProperties =
        CardViewProperties(
          viewType = ViewType.CARD,
          content =
            listOf(
              CompoundTextProperties(
                primaryText = "Richard Brown, M, 21",
                primaryTextColor = "#000000",
              ),
            ),
          header =
            CompoundTextProperties(
              primaryText = "Immunizations at 10 weeks",
              fontSize = 18.0f,
              primaryTextColor = "#6F7274",
            ),
          headerAction =
            CompoundTextProperties(
              primaryText = "Record All",
              primaryTextColor = "infoColor",
              clickable = "true",
              visible = "true",
            ),
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun CardViewWithPaddingPreview() {
  Column(modifier = Modifier.fillMaxWidth()) {
    CardView(
      viewProperties =
        CardViewProperties(
          viewType = ViewType.CARD,
          padding = 16,
          content =
            listOf(
              ColumnProperties(
                viewType = ViewType.COLUMN,
                children =
                  listOf(
                    ButtonProperties(
                      status = "OVERDUE",
                      viewType = ViewType.BUTTON,
                      text = "Sick child followup",
                    ),
                    ButtonProperties(
                      status = "COMPLETED",
                      viewType = ViewType.BUTTON,
                      text = "COVID Vaccination",
                    ),
                  ),
              ),
            ),
          header =
            CompoundTextProperties(fontSize = 18.0f, primaryTextColor = "#6F7274", padding = 16),
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun CardViewWithoutPaddingAndHeaderPreview() {
  Column(modifier = Modifier.fillMaxWidth()) {
    CardView(
      viewProperties =
        CardViewProperties(
          viewType = ViewType.CARD,
          content =
            listOf(
              CompoundTextProperties(
                primaryText = "Richard Brown, M, 21",
                primaryTextColor = "#000000",
              ),
            ),
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}
