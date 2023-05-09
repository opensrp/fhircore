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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import io.mockk.mockk
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnArrangement
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.PersonalDataItem
import org.smartregister.fhircore.engine.configuration.view.PersonalDataProperties
import org.smartregister.fhircore.engine.configuration.view.RowArrangement
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType

class ViewGeneratorTest {

  @get:Rule val composeRule = createComposeRule()

  private val navController = mockk<NavController>(relaxed = true, relaxUnitFun = true)
  private val resourceData = mockk<ResourceData>(relaxed = true, relaxUnitFun = true)

  @Test
  fun canGenerateServiceCardViewCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          ServiceCardProperties(
            viewType = ViewType.SERVICE_CARD,
            details =
              listOf(
                CompoundTextProperties(
                  viewType = ViewType.COMPOUND_TEXT,
                  primaryText = "Upcoming household service",
                  primaryTextColor = "#000000"
                )
              ),
            serviceMemberIcons = "CHILD,CHILD,CHILD,CHILD",
            showVerticalDivider = false,
            serviceButton =
              ButtonProperties(
                visible = "true",
                status = ServiceStatus.DUE.name,
                text = "Next visit 09-10-2022",
                smallSized = false
              )
          ),
        resourceData = resourceData,
        navController = navController
      )
    }
    composeRule.onNodeWithText("Upcoming household service").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Next visit 09-10-2022").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersActionableStatusButtonWhenViewTypeIsButton() {
    composeRule.setContent {
      GenerateView(
        properties =
          ButtonProperties(
            status = "DUE",
            text = "Button Text",
            actions =
              listOf(
                ActionConfig(
                  trigger = ActionTrigger.ON_CLICK,
                  workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
                  questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                )
              )
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
        navController = navController
      )
    }
    composeRule
      .onNodeWithText("Button Text", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  fun testColumnIsRenderedWhenViewTypeIsColumnAndPropertyWrapContentIsTrue() {
    composeRule.setContent {
      GenerateView(
        properties =
          ColumnProperties(
            wrapContent = true,
            children =
              listOf(
                ButtonProperties(status = "DUE", text = "Due Task"),
                ButtonProperties(status = "COMPLETED", text = "Completed Task"),
                ButtonProperties(status = "READY", text = "Ready Task"),
              ),
            viewType = ViewType.COLUMN
          ),
        resourceData = resourceData,
        navController = navController
      )
    }
    composeRule
      .onNodeWithText("Due Task", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithText("Completed Task", useUnmergedTree = true).assertExists()
    composeRule
      .onNodeWithText("Ready Task", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testColumnIsRenderedWhenViewTypeIsColumnAndWrapContentIsFalse() {
    composeRule.setContent {
      GenerateView(
        properties =
          ColumnProperties(
            wrapContent = false,
            alignment = ViewAlignment.CENTER,
            arrangement = ColumnArrangement.TOP,
            children =
              listOf(
                ButtonProperties(status = "DUE", text = "Due Task", visible = "true"),
                ButtonProperties(status = "COMPLETED", text = "Completed Task", visible = "false"),
                ButtonProperties(status = "READY", text = "Ready Task", visible = "true"),
              ),
            viewType = ViewType.COLUMN
          ),
        resourceData = resourceData,
        navController = navController
      )
    }
    composeRule
      .onNodeWithText("Due Task", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithText("Completed Task", useUnmergedTree = true).assertDoesNotExist()
    composeRule
      .onNodeWithText("Ready Task", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testChildIsVisibleTogglesVisibilityOfComponentsNestedInColumn() {
    composeRule.setContent {
      GenerateView(
        properties =
          ColumnProperties(
            wrapContent = false,
            alignment = ViewAlignment.START,
            arrangement = ColumnArrangement.TOP,
            children =
              listOf(
                CardViewProperties(
                  viewType = ViewType.CARD,
                  content =
                    listOf(
                      CompoundTextProperties(
                        primaryText = "Richard Brown, M, 29",
                        primaryTextColor = "#000000",
                        visible = "false"
                      )
                    )
                ),
                CardViewProperties(
                  viewType = ViewType.CARD,
                  content =
                    listOf(
                      CompoundTextProperties(
                        primaryText = "Jane Brown, M, 26",
                        primaryTextColor = "#000000",
                      )
                    )
                ),
                CardViewProperties(
                  viewType = ViewType.CARD,
                  content =
                    listOf(
                      CompoundTextProperties(
                        primaryText = "Billy Brown, M, 20",
                        primaryTextColor = "#000000",
                        visible = "false"
                      )
                    )
                )
              ),
            viewType = ViewType.COLUMN
          ),
        resourceData = resourceData,
        navController = navController
      )
    }
    composeRule.onNodeWithText("Richard Brown, M, 29", useUnmergedTree = true).assertDoesNotExist()
    composeRule
      .onNodeWithText("Jane Brown, M, 26", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithText("Billy Brown, M, 20", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun testRowIsRenderedWhenViewTypeIsRowAndPropertyWrapContentIsTrue() {
    composeRule.setContent {
      GenerateView(
        properties =
          RowProperties(
            wrapContent = true,
            children =
              listOf(
                ButtonProperties(status = "DUE", text = "Due Task"),
                ButtonProperties(status = "COMPLETED", text = "Completed Task"),
                ButtonProperties(status = "READY", text = "Ready Task"),
              ),
            viewType = ViewType.ROW
          ),
        resourceData = resourceData,
        navController = navController
      )
    }
    composeRule
      .onNodeWithText("Due Task", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithText("Completed Task", useUnmergedTree = true).assertExists()
    composeRule
      .onNodeWithText("Ready Task", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testRowIsRenderedWhenViewTypeIsRowAndWrapContentIsFalse() {
    composeRule.setContent {
      GenerateView(
        properties =
          RowProperties(
            wrapContent = false,
            alignment = ViewAlignment.CENTER,
            arrangement = RowArrangement.START,
            children =
              listOf(
                ButtonProperties(status = "DUE", text = "Due Task", visible = "true"),
                ButtonProperties(status = "COMPLETED", text = "Completed Task", visible = "false"),
                ButtonProperties(status = "READY", text = "Ready Task", visible = "true"),
              ),
            viewType = ViewType.ROW
          ),
        resourceData = resourceData,
        navController = navController
      )
    }
    composeRule
      .onNodeWithText("Due Task", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithText("Completed Task", useUnmergedTree = true).assertDoesNotExist()
    composeRule.onNodeWithText("Ready Task", useUnmergedTree = true).assertExists()
  }

  @Test
  fun testGenerateViewRendersViewTypePersonalDataCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          PersonalDataProperties(
            personalDataItems =
              listOf(
                PersonalDataItem(
                  label =
                    CompoundTextProperties(
                      primaryText = "Sex",
                    ),
                  displayValue =
                    CompoundTextProperties(
                      primaryText = "Male",
                    )
                )
              )
          ),
        resourceData = resourceData,
        navController = navController
      )
    }
    composeRule.onNodeWithText("Sex").assertIsDisplayed()
    composeRule.onNodeWithText("Male").assertIsDisplayed()
  }
}
