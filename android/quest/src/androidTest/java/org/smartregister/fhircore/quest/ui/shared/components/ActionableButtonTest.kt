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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Patient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.shared.models.ViewComponentEvent

class ActionableButtonTest {

  private val mockOnViewComponentClick: (ViewComponentEvent) -> Unit = spyk({})

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun init() {
    composeRule.setContent {
      Column(modifier = Modifier.height(50.dp)) {
        ActionableButton(
          buttonProperties =
            ButtonProperties(
              status = "COMPLETED",
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
          onViewComponentEvent = mockOnViewComponentClick
        )
      }
    }
  }

  @Test
  fun testActionableButtonRendersAncClickWorksCorrectly() {
    composeRule
      .onNodeWithText("Button Text", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()

    composeRule.onNodeWithTag(ACTIONABLE_BUTTON_TEST_TAG).performClick()
    verify { mockOnViewComponentClick(any()) }
  }
}
