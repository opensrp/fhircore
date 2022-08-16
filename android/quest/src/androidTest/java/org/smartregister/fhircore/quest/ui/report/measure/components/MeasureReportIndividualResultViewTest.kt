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

package org.smartregister.fhircore.quest.ui.report.measure.components

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData

class MeasureReportIndividualResultViewTest {

  @get:Rule val composeTestRule = createEmptyComposeRule()

  private lateinit var scenario: ActivityScenario<ComponentActivity>

  private val patientViewData =
    MeasureReportPatientViewData(
      name = "Jacky Coughlin",
      gender = "F",
      age = "27",
      logicalId = "12444"
    )

  @Before
  fun setup() {
    scenario = ActivityScenario.launch(ComponentActivity::class.java)
  }

  @Test
  fun testResultViewRendersPersonalDetailsCorrectly() {
    initComposable(isMatchedIndicator = true)
    composeTestRule.onNodeWithTag(PERSONAL_DETAILS_TEST_TAG).assertExists()
    composeTestRule
      .onNodeWithText(useUnmergedTree = true, text = patientViewData.personalDetails())
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testResultViewRendersIndicatorStatusCorrectly() {
    initComposable(isMatchedIndicator = true)
    composeTestRule.onNodeWithTag(RESULT_VIEW_INDICATOR_STATUS).assertExists()
    composeTestRule
      .onNodeWithText(useUnmergedTree = true, text = "True")
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testResultViewRendersCheckIconCorrectly() {
    initComposable(isMatchedIndicator = true)
    composeTestRule.onNodeWithTag(RESULT_VIEW_CHECK_ICON).assertExists().assertIsDisplayed()
  }

  @Test
  fun testResultViewRendersStalledIconCorrectly() {
    initComposable(isMatchedIndicator = false)
    composeTestRule.onNodeWithTag(RESULT_VIEW_STALLED_ICON).assertExists().assertIsDisplayed()
  }

  private fun initComposable(isMatchedIndicator: Boolean) {
    scenario.onActivity { activity ->
      activity.setContent {
        AppTheme {
          MeasureReportIndividualResultView(
            patientViewData = patientViewData,
            isMatchedIndicator = isMatchedIndicator,
            indicatorStatus = "True",
            indicatorDescription = ""
          )
        }
      }
    }
  }
}
