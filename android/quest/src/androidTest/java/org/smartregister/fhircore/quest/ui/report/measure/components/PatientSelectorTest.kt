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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PatientSelectorTest {

  private val mockListener: () -> Unit = spyk({})

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    composeTestRule.setContent {
      PatientSelector(patientName = "Mary Magdalene", onChangePatient = mockListener)
    }
  }

  @Test
  fun testPatientSelectorRendersPatientNameCorrectly() {
    composeTestRule.onNodeWithTag(PATIENT_NAME_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Mary Magdalene").assertExists().assertIsDisplayed()
  }

  @Test
  fun testPatientSelectorRendersCloseIconCorrectly() {
    composeTestRule
      .onNodeWithTag(CLOSE_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testPatientSelectorRendersCloseIconBackgroundCorrectly() {
    composeTestRule
      .onNodeWithTag(CLOSE_ICON_BACKGROUND_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testPatientSelectorRendersChangeTextCorrectly() {
    composeTestRule.onNodeWithTag(CHANGE_TEXT_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Change").assertExists().assertIsDisplayed()
  }

  @Test
  fun testThatChangeRowClickCallsTheListener() {
    val changeRow = composeTestRule.onNodeWithTag(CHANGE_ROW_TEST_TAG)
    changeRow.assertExists()
    changeRow.performClick()
    verify { mockListener() }
  }
}
