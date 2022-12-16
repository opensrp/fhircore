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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import io.mockk.mockk
import org.hl7.fhir.r4.model.Patient
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.SpacerProperties
import org.smartregister.fhircore.engine.configuration.view.TextFontWeight
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType

class ViewGeneratorTest {

  @get:Rule val composeRule = createComposeRule()

  private val navController = mockk<NavController>(relaxed = true, relaxUnitFun = true)

  @Test
  fun testFullNameCompoundTextNoSecondaryTextIsRenderedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            primaryText = "Full Name, Age",
            primaryTextColor = "#000000",
            primaryTextFontWeight = TextFontWeight.SEMI_BOLD
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Full Name, Age").assertExists().assertIsDisplayed()
  }

  @Test
  fun testSexCompoundTextNoSecondaryTextIsRenderedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            primaryText = "Sex",
            primaryTextColor = "#5A5A5A",
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Sex").assertExists().assertIsDisplayed()
  }

  @Test
  fun testFullNameCompoundTextWithSecondaryTextIsRenderedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(primaryText = "Full Name, Sex, Age", primaryTextColor = "#000000"),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Full Name, Sex, Age").assertExists().assertIsDisplayed()
  }

  @Test
  fun testLastVisitedCompoundTextWithSecondaryTextIsRenderedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            primaryText = "Last visited",
            primaryTextColor = "#5A5A5A",
            secondaryText = "Yesterday",
            secondaryTextColor = "#FFFFFF",
            separator = ".",
            secondaryTextBackgroundColor = "#FFA500"
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Last visited").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Yesterday").assertExists().assertIsDisplayed()
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
        resourceData = ResourceData(Patient()),
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
  fun testGenerateViewRendersOverDueServiceCardTitleCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Overdue household service",
            primaryTextColor = "#000000",
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Overdue household service").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersOverDueServiceCardTownCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Town/Village",
            primaryTextColor = "#5A5A5A",
            secondaryText = "HH No.",
            secondaryTextColor = "#555AAA"
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Town/Village").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("HH No.").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersOverDueServiceCardLastVisitedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Last visited yesterday",
            primaryTextColor = "#5A5A5A",
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Last visited yesterday").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersOverDueServiceCardServiceButtonCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          ButtonProperties(
            status = "OVERDUE",
            text = "1",
            smallSized = false,
            actions =
              listOf(
                ActionConfig(
                  trigger = ActionTrigger.ON_CLICK,
                  workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
                  questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                )
              )
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("1").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersDueServiceCardTitleCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Due household services",
            primaryTextColor = "#000000",
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Due household services").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersDueServiceCardTownCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Town/Village",
            primaryTextColor = "#5A5A5A",
            secondaryText = "HH No.",
            secondaryTextColor = "#555AAA"
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Town/Village").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("HH No.").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersDueServiceCardLastVisitedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Last visited yesterday",
            primaryTextColor = "#5A5A5A",
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Last visited yesterday").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersDueServiceCardServiceButtonCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          ButtonProperties(
            status = "DUE",
            text = "Issue Bed net",
            smallSized = false,
            actions =
              listOf(
                ActionConfig(
                  trigger = ActionTrigger.ON_CLICK,
                  workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
                  questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                )
              )
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Issue Bed net").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersUpcomingServiceCardTitleCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Upcoming household service",
            primaryTextColor = "#000000",
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Upcoming household service").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersUpcomingServiceCardTownCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Town/Village",
            primaryTextColor = "#5A5A5A",
            secondaryText = "HH No.",
            secondaryTextColor = "#555AAA"
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Town/Village").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("HH No.").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersUpcomingServiceCardLastVisitedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Last visited yesterday",
            primaryTextColor = "#5A5A5A",
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Last visited yesterday").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersUpcomingServiceCardServiceButtonCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          ButtonProperties(
            status = "UPCOMING",
            text = "Next visit 09-10-2022",
            smallSized = false,
            actions =
              listOf(
                ActionConfig(
                  trigger = ActionTrigger.ON_CLICK,
                  workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
                  questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                )
              )
          ),
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Next visit 09-10-2022").assertExists().assertIsDisplayed()
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
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
        resourceData = ResourceData(Patient()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("22y").assertExists().assertIsDisplayed()
  }

  @Test
  fun testGenerateViewRendersVerticalSpacerViewCorrectly() {
    val spacerProperties = SpacerProperties(height = 16F, width = null)
    composeRule.setContent { SpacerView(spacerProperties = spacerProperties) }
    composeRule.onNodeWithTag(VERTICAL_SPACER_TEST_TAG).assertExists()
  }

  @Test
  fun testGenerateViewRendersHorizontalSpacerViewCorrectly() {
    val spacerProperties = SpacerProperties(height = null, width = 16F)
    composeRule.setContent { SpacerView(spacerProperties = spacerProperties) }
    composeRule.onNodeWithTag(HORIZONTAL_SPACER_TEST_TAG, useUnmergedTree = true).assertExists()
  }
}
