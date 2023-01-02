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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult

class MeasureReportPopulationResultViewTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val measureReportIndividualResultList =
    listOf(
      MeasureReportIndividualResult(
        status = "Test Status",
        isMatchedIndicator = false,
        description = "This is sample description",
        title = "Title Individual Result",
        percentage = "50.0",
        count = "1"
      )
    )

  private val measureReportPopulationResultList =
    listOf(
      MeasureReportPopulationResult(
        title = "Population Title",
        count = "2",
        indicatorTitle = "Indicator1",
        dataList = measureReportIndividualResultList,
        measureReportDenominator = 2
      )
    )

  @Before
  fun setup() {
    composeTestRule.setContent {
      val dataList = measureReportPopulationResultList
      MeasureReportPopulationResultView(dataList = dataList)
    }
  }

  @Test
  fun testPopulationResultCardRendersPopulationIndicatorCorrectly() {
    composeTestRule.onNodeWithTag(POPULATION_INDICATOR_TITLE, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithText(measureReportPopulationResultList.first().indicatorTitle.uppercase())
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testPopulationResultCardRendersPopulationDenominatorCorrectly() {
    composeTestRule.onNodeWithTag(POPULATION_COUNT_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithText(
        measureReportPopulationResultList.first().measureReportDenominator.toString().uppercase()
      )
      .assertExists()
      .assertIsDisplayed()
  }
}
