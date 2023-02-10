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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.core.util.Pair
import java.util.Calendar
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DateSelectionBoxTest {

  private val dateRange =
    mutableStateOf(Pair(Calendar.getInstance().timeInMillis, Calendar.getInstance().timeInMillis))

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    composeTestRule.setContent {
      DateSelectionBox(
        startDate = "Start date",
        endDate = "End date",
        onDateRangeSelected = {},
        dateRange = dateRange
      )
    }
  }

  @Test
  fun testDateSelectionBoxRendersDateRangeTitleCorrectly() {
    composeTestRule.onNodeWithTag(DATE_RANGE_TITLE_TEST_TAG, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Date range").assertExists().assertIsDisplayed()
  }

  @Test
  fun testDateSelectionBoxRendersStartDateCorrectly() {
    composeTestRule.onNodeWithText("Start date").assertExists().assertIsDisplayed()
  }

  @Test
  fun testDateSelectionBoxRendersEndDateCorrectly() {
    composeTestRule.onNodeWithText("End date").assertExists().assertIsDisplayed()
  }

  @Test
  fun testDateSelectionBoxRendersDateRangeSeparatorCorrectly() {
    composeTestRule.onNodeWithTag(DATE_RANGE_SEPARATOR_TEST_TAG).assertExists()
    composeTestRule.onNodeWithText("-").assertExists().assertIsDisplayed()
  }

  @Test
  fun testDateSelectionBoxRendersCalendarIconCorrectly() {
    composeTestRule.onNodeWithTag(CALENDAR_ICON_TEST_TAG).assertExists().assertIsDisplayed()
  }
}
