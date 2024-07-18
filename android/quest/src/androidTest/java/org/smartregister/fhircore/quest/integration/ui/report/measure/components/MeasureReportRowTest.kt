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

package org.smartregister.fhircore.quest.integration.ui.report.measure.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.quest.ui.report.measure.components.MEASURE_ROW_FORWARD_ARROW_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.components.MEASURE_ROW_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.components.MEASURE_ROW_TITLE_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.components.MeasureReportRow

class MeasureReportRowTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val reportConfiguration =
    ReportConfiguration(
      id = "101",
      title = "2+ ANC Contacts",
      description = "Pregnant women with at least two ANC Contacts",
      module = "Module 1- ANC Contacts",
    )

  @Test
  fun testMeasureRowRendersTitleCorrectly() {
    composeTestRule.setContent {
      MeasureReportRow(title = reportConfiguration.module, onRowClick = {})
    }

    composeTestRule.onNodeWithTag(MEASURE_ROW_TITLE_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText(reportConfiguration.module).assertExists().assertIsDisplayed()
  }

  @Test
  fun testMeasureRowRendersForwardIconCorrectly() {
    composeTestRule.setContent {
      MeasureReportRow(title = reportConfiguration.module, onRowClick = {})
    }

    composeTestRule
      .onNodeWithTag(MEASURE_ROW_FORWARD_ARROW_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testThatRowClickCallsTheListener() {
    var clicked = false

    composeTestRule.setContent {
      MeasureReportRow(title = reportConfiguration.module, onRowClick = { clicked = true })
    }

    val measureRow = composeTestRule.onNodeWithTag(MEASURE_ROW_TEST_TAG)
    measureRow.assertExists()
    measureRow.performClick()
    Assert.assertTrue(clicked)
  }
}
