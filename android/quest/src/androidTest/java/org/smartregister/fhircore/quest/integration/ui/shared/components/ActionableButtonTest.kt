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

package org.smartregister.fhircore.quest.integration.ui.shared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation.testing.TestNavHostController
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.quest.ui.shared.components.ActionableButton

class ActionableButtonTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testActionableButtonRendersAncClickWorksCorrectlyWithStatusDue() {
    setContent(ServiceStatus.DUE.name)

    composeRule
      .onNodeWithText("Button Text", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  fun testActionableButtonRendersAncClickWorksCorrectlyWithStatusOverdue() {
    setContent(ServiceStatus.OVERDUE.name)

    composeRule
      .onNodeWithText("Button Text", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  fun testActionableButtonRendersAncClickWorksCorrectlyWithStatusInProgress() {
    setContent(ServiceStatus.IN_PROGRESS.name)

    composeRule
      .onNodeWithText("Button Text", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  fun testActionableButtonRendersAncClickWorksCorrectlyWithStatusSetUsingComputedValues() {
    setContent("@{ status }", computedValuesMap = mapOf("status" to "DUE"))

    composeRule
      .onNodeWithText("Button Text", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  fun testActionableButtonRendersAncClickWorksCorrectlyWhenDisabled() {
    setContent(ServiceStatus.OVERDUE.name, "false")

    composeRule
      .onNodeWithText("Button Text", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  private fun setContent(
    serviceStatus: String,
    enabled: String = "true",
    computedValuesMap: Map<String, Any> = emptyMap(),
  ) {
    composeRule.setContent {
      Column(modifier = Modifier.height(50.dp)) {
        ActionableButton(
          buttonProperties =
            ButtonProperties(
              status = serviceStatus,
              text = "Button Text",
              actions =
                listOf(
                  ActionConfig(
                    trigger = ActionTrigger.ON_CLICK,
                    workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE.name,
                    questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                  ),
                ),
              enabled = enabled,
              startIcon = ImageConfig("ic_home", ICON_TYPE_LOCAL),
            ),
          resourceData = ResourceData("id", ResourceType.Patient, computedValuesMap),
          navController = TestNavHostController(LocalContext.current),
          decodeImage = null,
        )
      }
    }
  }
}
