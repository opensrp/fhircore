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

package org.smartregister.fhircore.quest.ui.shared.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.RowArrangement
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.TextFontWeight
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

/**
 * This function takes a list of [ViewProperties] and build views recursively as configured in the
 * properties. The content used in the views is provided via [resourceData] class.
 *
 * Note that by default the view render is not rendered in a view group like a Column/Row. This is
 * to allow us to call the function recursively for nested view group layout. Therefore when using
 * the this layout, provide a parent layout (usually Row/Column) so that the views can be rendered
 * appropriately otherwise the generated view group will be rendered one on top of the other.
 */
@Composable
fun ViewRenderer(
  viewProperties: List<ViewProperties>,
  resourceData: ResourceData,
  navController: NavController,
  decodedImageMap: SnapshotStateMap<String, Bitmap>,
  areViewPropertiesInterpolated: Boolean = false,
) {
  viewProperties.forEach { properties ->
    val interpolatedProperties =
      if (areViewPropertiesInterpolated) {
        properties
      } else {
        properties.interpolate(resourceData.computedValuesMap)
      }
    GenerateView(
      modifier = generateModifier(properties),
      properties = interpolatedProperties,
      resourceData = resourceData,
      navController = navController,
      decodedImageMap = decodedImageMap,
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun PreviewWeightedViewsInRow() {
  ViewRenderer(
    viewProperties =
      listOf(
        RowProperties(
          viewType = ViewType.ROW,
          fillMaxWidth = true,
          children =
            listOf(
              ButtonProperties(
                viewType = ViewType.BUTTON,
                text = "Due Service",
                status = "DUE",
                fillMaxWidth = true,
                weight = 1.0f,
              ),
              ButtonProperties(
                viewType = ViewType.BUTTON,
                text = "Completed Service",
                status = "COMPLETED",
                fillMaxWidth = true,
                weight = 1.0f,
              ),
            ),
        ),
      ),
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    navController = rememberNavController(),
    decodedImageMap = remember { mutableStateMapOf() },
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun PreviewWrappedViewsInRow() {
  ViewRenderer(
    viewProperties =
      listOf(
        RowProperties(
          viewType = ViewType.ROW,
          fillMaxWidth = true,
          wrapContent = true,
          children =
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
                padding = 8,
              ),
              CompoundTextProperties(
                viewType = ViewType.COMPOUND_TEXT,
                primaryText = "TB Danger Signs",
                primaryTextColor = "#D2760D",
                primaryTextBackgroundColor = "#FFECD6",
                padding = 8,
              ),
              CompoundTextProperties(
                viewType = ViewType.COMPOUND_TEXT,
                primaryText = "HIV Danger Signs",
                primaryTextColor = "#D2760D",
                primaryTextBackgroundColor = "#FFECD6",
                padding = 8,
              ),
              CompoundTextProperties(
                viewType = ViewType.COMPOUND_TEXT,
                primaryText = "COVID Danger Signs",
                primaryTextColor = "#D2760D",
                primaryTextBackgroundColor = "#FFECD6",
                padding = 8,
              ),
            ),
        ),
      ),
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    navController = rememberNavController(),
    decodedImageMap = remember { mutableStateMapOf() },
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun PreviewSameSizedViewInRow() {
  ViewRenderer(
    viewProperties =
      listOf(
        RowProperties(
          viewType = ViewType.ROW,
          fillMaxWidth = true,
          wrapContent = false,
          alignment = ViewAlignment.START,
          arrangement = RowArrangement.CENTER,
          children =
            listOf(
              CompoundTextProperties(
                viewType = ViewType.COMPOUND_TEXT,
                primaryText = "Janet Sade",
                primaryTextColor = "#000000",
                primaryTextBackgroundColor = "#CFCFCF",
                padding = 8,
              ),
              CompoundTextProperties(
                viewType = ViewType.COMPOUND_TEXT,
                primaryText = "ANC Danger Signs",
                primaryTextColor = "#D2760D",
                primaryTextBackgroundColor = "#FFECD6",
                padding = 8,
                alignment = ViewAlignment.END,
              ),
            ),
        ),
      ),
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    navController = rememberNavController(),
    decodedImageMap = remember { mutableStateMapOf() },
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun PreviewCardViewWithRows() {
  ViewRenderer(
    viewProperties =
      listOf(
        CardViewProperties(
          viewType = ViewType.CARD,
          fillMaxWidth = true,
          header =
            CompoundTextProperties(
              viewType = ViewType.COMPOUND_TEXT,
              primaryText = "PATIENTS",
              primaryTextColor = "#6F7274",
              padding = 8,
            ),
          content =
            listOf(
              RowProperties(
                viewType = ViewType.ROW,
                alignment = ViewAlignment.START,
                children =
                  listOf(
                    CompoundTextProperties(
                      viewType = ViewType.COMPOUND_TEXT,
                      primaryText = "HHs",
                      primaryTextColor = "#000000",
                    ),
                    ColumnProperties(
                      viewType = ViewType.COLUMN,
                      weight = 0.7f,
                      children =
                        listOf(
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "33",
                            primaryTextColor = "#6F7274",
                            secondaryText = "(Overdue)",
                            secondaryTextColor = "#FF0000",
                          ),
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "VIEW ALL",
                            primaryTextColor = "#508BE8",
                            primaryTextFontWeight = TextFontWeight.MEDIUM,
                          ),
                        ),
                    ),
                  ),
              ),
              RowProperties(
                viewType = ViewType.ROW,
                alignment = ViewAlignment.START,
                children =
                  listOf(
                    CompoundTextProperties(
                      viewType = ViewType.COMPOUND_TEXT,
                      primaryText = "PNC",
                      primaryTextColor = "#000000",
                    ),
                    ColumnProperties(
                      viewType = ViewType.COLUMN,
                      weight = 0.7f,
                      children =
                        listOf(
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "10",
                            primaryTextColor = "#6F7274",
                          ),
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "VIEW ALL",
                            primaryTextColor = "#508BE8",
                            primaryTextFontWeight = TextFontWeight.MEDIUM,
                          ),
                        ),
                    ),
                  ),
              ),
              RowProperties(
                viewType = ViewType.ROW,
                alignment = ViewAlignment.START,
                children =
                  listOf(
                    CompoundTextProperties(
                      viewType = ViewType.COMPOUND_TEXT,
                      primaryText = "ANC",
                      primaryTextColor = "#000000",
                    ),
                    ColumnProperties(
                      viewType = ViewType.COLUMN,
                      weight = 0.7f,
                      children =
                        listOf(
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "2",
                            primaryTextColor = "#6F7274",
                          ),
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "VIEW ALL",
                            primaryTextColor = "#508BE8",
                            primaryTextFontWeight = TextFontWeight.MEDIUM,
                          ),
                        ),
                    ),
                  ),
              ),
            ),
        ),
      ),
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    navController = rememberNavController(),
    decodedImageMap = remember { mutableStateMapOf() },
  )
}
