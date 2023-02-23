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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import io.mockk.mockk
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.SpacerProperties
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
  fun testGenerateViewRendersActionableButtonWhenViewTypeIsButton() {
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
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
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
  fun testGenerateViewRendersCardViewWithoutPaddingContentCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            primaryText = "Richard Brown, M, 21",
            primaryTextColor = "#000000",
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Richard Brown, M, 21").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersCardViewWithoutPaddingHeaderCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            primaryText = "HOUSE MEMBERS",
            fontSize = 18.0f,
            primaryTextColor = "#6F7274",
            padding = 16
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("HOUSE MEMBERS").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersCardViewWithPaddingHeaderCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            primaryText = "VISITS",
            fontSize = 18.0f,
            primaryTextColor = "#6F7274",
            padding = 16
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("VISITS").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersCardViewWithPaddingOverdueButtonCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          ButtonProperties(
            status = "OVERDUE",
            viewType = ViewType.BUTTON,
            text = "Sick child followup",
            actions =
              listOf(
                ActionConfig(
                  trigger = ActionTrigger.ON_CLICK,
                  workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
                  questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                )
              )
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Sick child followup").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersCardViewWithPaddingCompletedButtonCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          ButtonProperties(
            status = "COMPLETED",
            viewType = ViewType.BUTTON,
            text = "COVID Vaccination",
            actions =
              listOf(
                ActionConfig(
                  trigger = ActionTrigger.ON_CLICK,
                  workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
                  questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                )
              )
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("COVID Vaccination").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersPersonalDataGenderLabelCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties = CompoundTextProperties(primaryText = "Sex"),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Sex").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersPersonalDataGenderValueCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties = CompoundTextProperties(primaryText = "Female"),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Female").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersPersonalDataDobLabelCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties = CompoundTextProperties(primaryText = "DOB"),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("DOB").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersPersonalDataDobValueCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties = CompoundTextProperties(primaryText = "01 2000"),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("01 2000").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersPersonalDataAgeTitleCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties = CompoundTextProperties(primaryText = "Age"),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Age").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersPersonalDataAgeValueCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties = CompoundTextProperties(primaryText = "22y"),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("22y").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersVerticalSpacerViewCorrectly() {
    val spacerProperties = SpacerProperties(height = 16F, width = null)
    composeRule.setContent {
      GenerateView(
        properties = spacerProperties,
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithTag(VERTICAL_SPACER_TEST_TAG).assertExists()
  }

  @Test
  fun testGenerateViewRendersHorizontalSpacerViewCorrectly() {
    val spacerProperties = SpacerProperties(height = null, width = 16F)
    composeRule.setContent {
      GenerateView(
        properties = spacerProperties,
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithTag(HORIZONTAL_SPACER_TEST_TAG, useUnmergedTree = true).assertExists()
  }
}
