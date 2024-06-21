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
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.ui.report.measure.components.FAMILY_NAME_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.components.MeasureReportSubjectRow
import org.smartregister.fhircore.quest.ui.report.measure.components.SUBJECT_DETAILS_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.components.SUBJECT_ROW_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData

class MeasureReportSubjectRowTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val measureReportSubjectViewData =
    MeasureReportSubjectViewData(
      type = ResourceType.Patient,
      logicalId = "10101",
      display = "John Test, M, 45",
      family = "Test Family",
    )

  @Test
  fun testSubjectRowRendersSubjectDetailsCorrectly() {
    composeTestRule.setContent {
      MeasureReportSubjectRow(
        measureReportSubjectViewData = measureReportSubjectViewData,
        onRowClick = {},
      )
    }
    composeTestRule.onNodeWithTag(SUBJECT_DETAILS_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithText(measureReportSubjectViewData.display)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testSubjectRowRendersFamilyNameCorrectly() {
    composeTestRule.setContent {
      MeasureReportSubjectRow(
        measureReportSubjectViewData = measureReportSubjectViewData,
        onRowClick = {},
      )
    }
    composeTestRule.onNodeWithTag(FAMILY_NAME_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithText(text = measureReportSubjectViewData.family.toString())
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testThatRowClickCallsTheListener() {
    var clicked = false

    composeTestRule.setContent {
      MeasureReportSubjectRow(
        measureReportSubjectViewData = measureReportSubjectViewData,
        onRowClick = { clicked = true },
      )
    }
    val subjectRow = composeTestRule.onNodeWithTag(SUBJECT_ROW_TEST_TAG)
    subjectRow.assertExists()
    subjectRow.performClick()
    Assert.assertTrue(clicked)
  }
}
