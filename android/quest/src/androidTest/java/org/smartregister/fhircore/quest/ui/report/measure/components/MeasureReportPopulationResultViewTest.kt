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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
        dataList = measureReportIndividualResultList
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
  fun testPopulationResultCardRendersPopulationTitleCorrectly() {
    composeTestRule.onNodeWithTag(POPULATION_TITLE_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithText(measureReportPopulationResultList.first().title.uppercase())
      .assertExists()
      .assertIsDisplayed()
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
  fun testPopulationResultCardRendersPopulationCountCorrectly() {
    composeTestRule.onNodeWithTag(POPULATION_COUNT_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithText(measureReportPopulationResultList.first().count.uppercase())
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testPopulationResultCardDisplaysDivider() {
    composeTestRule
      .onNodeWithTag(POPULATION_RESULT_CARD_DIVIDER_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testPopulationResultItemRendersProgressBarCorrectly() {
    composeTestRule
      .onAllNodesWithTag(POPULATION_RESULT_ITEM_PROGRESS_BAR_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(2)
  }

  @Test
  fun testPopulationResultItemRendersTitleTextCorrectly() {
    composeTestRule
      .onNodeWithTag(POPULATION_REPORT_INDIVIDUAL_RESULT_TITLE_TEST_TAG, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText(measureReportIndividualResultList.first().title)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testPopulationResultItemRendersPercentageTextCorrectly() {
    composeTestRule
      .onNodeWithTag(
        POPULATION_REPORT_INDIVIDUAL_RESULT_PERCENTAGE_TEST_TAG,
        useUnmergedTree = true
      )
      .assertExists()
    composeTestRule
      .onAllNodesWithText("${measureReportIndividualResultList.first().percentage}%")
      .assertCountEquals(2)
  }

  @Test
  fun testPopulationResultItemRendersCountTextCorrectly() {
    composeTestRule
      .onNodeWithTag(POPULATION_REPORT_INDIVIDUAL_RESULT_COUNT_TEST_TAG, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText(measureReportIndividualResultList.first().count)
      .assertExists()
      .assertIsDisplayed()
  }
}
