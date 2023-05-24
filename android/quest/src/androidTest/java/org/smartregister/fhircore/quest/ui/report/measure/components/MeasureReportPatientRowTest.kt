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

package org.smartregister.fhircore.quest.ui.report.measure.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData

class MeasureReportSubjectRowTest {

  private val mockListener: (MeasureReportSubjectViewData) -> Unit = spyk({})

  @get:Rule val composeTestRule = createComposeRule()

  private val measureReportSubjectViewData =
    MeasureReportSubjectViewData(
      type = ResourceType.Patient,
      logicalId = "10101",
      display = "John Test, M, 45",
      family = "Test Family"
    )

  @Before
  fun setup() {
    composeTestRule.setContent {
      MeasureReportSubjectRow(
        measureReportSubjectViewData = measureReportSubjectViewData,
        onRowClick = mockListener
      )
    }
  }

  @Test
  fun testSubjectRowRendersSubjectDetailsCorrectly() {
    composeTestRule.onNodeWithTag(SUBJECT_DETAILS_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithText(measureReportSubjectViewData.display)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testSubjectRowRendersFamilyNameCorrectly() {
    composeTestRule.onNodeWithTag(FAMILY_NAME_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithText(text = measureReportSubjectViewData.family.toString())
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testThatRowClickCallsTheListener() {
    val subjectRow = composeTestRule.onNodeWithTag(SUBJECT_ROW_TEST_TAG)
    subjectRow.assertExists()
    subjectRow.performClick()
    verify { mockListener(any()) }
  }
}
