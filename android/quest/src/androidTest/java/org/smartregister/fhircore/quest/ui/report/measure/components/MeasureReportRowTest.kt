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
import androidx.compose.ui.test.performClick
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig

class MeasureReportRowTest {

  private val mockListener: () -> Unit = spyk({})

  @get:Rule val composeTestRule = createComposeRule()

  private val measureReportConfig =
    MeasureReportConfig(
      id = "101",
      title = "2+ ANC Contacts",
      description = "Pregnant women with at least two ANC Contacts",
      module = "Module1",
    )

  @Before
  fun setup() {
    composeTestRule.setContent {
      MeasureReportRow(title = measureReportConfig.module, onRowClick = mockListener)
    }
  }

  @Test
  fun testMeasureRowRendersTitleCorrectly() {
    composeTestRule.onNodeWithTag(MEASURE_ROW_TITLE_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText(measureReportConfig.title).assertExists().assertIsDisplayed()
  }

  @Test
  fun testMeasureRowRendersDescriptionCorrectly() {
    composeTestRule
      .onNodeWithTag(MEASURE_ROW_DESCRIPTION_TEST_TAG, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText(measureReportConfig.description)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testMeasureRowRendersForwardIconCorrectly() {
    composeTestRule
      .onNodeWithTag(MEASURE_ROW_FORWARD_ARROW_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testThatRowClickCallsTheListener() {
    val measureRow = composeTestRule.onNodeWithTag(MEASURE_ROW_TEST_TAG)
    measureRow.assertExists()
    measureRow.performClick()
    verify { mockListener() }
  }
}
