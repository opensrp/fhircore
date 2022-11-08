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
import io.mockk.spyk
import java.util.Date
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.quest.ui.report.measure.models.ReportRangeSelectionData
import org.smartregister.fhircore.quest.ui.report.measure.screens.SHOW_FIXED_RANGE_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.SHOW_PROGRESS_INDICATOR_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.showFixedMonthYearListing

class ReportTypeSelectorScreenTest {

  private val mockListener: (Date?) -> Unit = spyk({})
  private val mockBackListener: () -> Unit = spyk({})

  @get:Rule val composeTestRule = createComposeRule()

  private val monthList =
    listOf(ReportRangeSelectionData("March", "October", "2022-02-02".getDate("dd mm yyyy")))

  private val dateRange = HashMap<String, List<ReportRangeSelectionData>>()
  @Before
  fun setup() {
    dateRange["2022"] = monthList
  }

  @Test
  fun testMonthYearListDisplayed() {
    composeTestRule.setContent {
      showFixedMonthYearListing(
        screenTitle = "Measure Report",
        onMonthSelected = mockListener,
        onBackPress = mockBackListener,
        reportGenerationRange = dateRange,
        showProgressIndicator = false
      )
    }
    composeTestRule
      .onNodeWithTag(SHOW_FIXED_RANGE_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testProgressIndicatorIsDisplayed() {
    composeTestRule.setContent {
      showFixedMonthYearListing(
        screenTitle = "Measure Report",
        onMonthSelected = mockListener,
        onBackPress = mockBackListener,
        reportGenerationRange = dateRange,
        showProgressIndicator = true
      )
    }

    composeTestRule
      .onNodeWithTag(SHOW_PROGRESS_INDICATOR_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }
}
