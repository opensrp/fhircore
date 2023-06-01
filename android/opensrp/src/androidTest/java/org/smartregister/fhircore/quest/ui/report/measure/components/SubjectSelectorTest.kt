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

package org.smartregister.fhircore.opensrp.ui.report.measure.components

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
import org.smartregister.fhircore.opensrp.ui.shared.models.MeasureReportSubjectViewData

class SubjectSelectorTest {

  private val mockListener: () -> Unit = spyk({})

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    composeTestRule.setContent {
      SubjectSelector(
        subjects = setOf(MeasureReportSubjectViewData(ResourceType.Patient, "1", "Mary Magdalene")),
        onAddSubject = mockListener,
        onRemoveSubject = {}
      )
    }
  }

  @Test
  fun testSubjectSelectorRendersSubjectNameCorrectly() {
    composeTestRule.onNodeWithTag(SUBJECT_NAME_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Mary Magdalene").assertExists().assertIsDisplayed()
  }

  @Test
  fun testSubjectSelectorRendersCloseIconBackgroundCorrectly() {
    composeTestRule
      .onNodeWithTag(CLOSE_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testSubjectSelectorRendersChangeTextCorrectly() {
    composeTestRule.onNodeWithTag(CHANGE_TEXT_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("ADD").assertExists().assertIsDisplayed()
  }

  @Test
  fun testThatChangeRowClickCallsTheListener() {
    val changeRow = composeTestRule.onNodeWithTag(CHANGE_TEXT_TEST_TAG)
    changeRow.assertExists()
    changeRow.performClick()
    verify { mockListener() }
  }
}
