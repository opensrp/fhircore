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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.material.datepicker.MaterialDatePicker
import io.mockk.spyk
import java.util.Date
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.ui.report.measure.ReportTypeSelectorUiState
import org.smartregister.fhircore.quest.ui.report.measure.models.ReportRangeSelectionData
import org.smartregister.fhircore.quest.ui.report.measure.screens.DateRangeSelector
import org.smartregister.fhircore.quest.ui.report.measure.screens.FixedMonthYearListing
import org.smartregister.fhircore.quest.ui.report.measure.screens.MONTH_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.PLEASE_WAIT_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.ReportFilterSelector
import org.smartregister.fhircore.quest.ui.report.measure.screens.SHOW_DATE_PICKER_FORM_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.SHOW_FIXED_RANGE_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.SHOW_PROGRESS_INDICATOR_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.TEST_MONTH_CLICK_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.YEAR_TEST_TAG

class ReportTypeSelectorScreenTest {

  private val mockRangeSelectListener: (Date?) -> Unit = spyk({})
  private val mockDateSelectListener: (androidx.core.util.Pair<Long, Long>?) -> Unit = spyk({})
  private val mockTypeSelectListener: (MeasureReport.MeasureReportType?) -> Unit = spyk({})
  private val mockBackListener: () -> Unit = spyk({})

  @get:Rule val composeTestRule = createComposeRule()

  private val monthList = listOf(ReportRangeSelectionData("March", "October", Date()))

  private val dateRange = HashMap<String, List<ReportRangeSelectionData>>()

  @Before
  fun setup() {
    dateRange["2022"] = monthList
  }

  @Test
  fun testMonthYearListDisplayed() {
    composeTestRule.setContent {
      FixedMonthYearListing(
        onMonthSelected = mockRangeSelectListener,
        reportGenerationRange = dateRange,
        showProgressIndicator = false,
        innerPadding = PaddingValues(),
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
      FixedMonthYearListing(
        onMonthSelected = mockRangeSelectListener,
        reportGenerationRange = dateRange,
        showProgressIndicator = true,
        innerPadding = PaddingValues(),
      )
    }

    composeTestRule
      .onNodeWithTag(SHOW_PROGRESS_INDICATOR_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithTag(SHOW_FIXED_RANGE_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testMonthClickListener() {
    composeTestRule.setContent {
      FixedMonthYearListing(
        onMonthSelected = mockRangeSelectListener,
        reportGenerationRange = dateRange,
        showProgressIndicator = false,
        innerPadding = PaddingValues(),
      )
    }

    composeTestRule
      .onNodeWithTag(TEST_MONTH_CLICK_TAG, useUnmergedTree = true)
      .assertExists()
      .performClick()
      .assertIsDisplayed()
  }

  @Test
  fun testShowDatePickerFormr() {
    composeTestRule.setContent {
      DateRangeSelector(
        showProgressIndicator = false,
        startDate = "Start Date",
        endDate = "End Date",
        onGenerateReportClicked = mockBackListener,
        dateRange = mutableStateOf(defaultDateRangeState()),
        onDateRangeSelected = mockDateSelectListener,
        generateReport = false,
        innerPadding = PaddingValues(),
      )
    }
    composeTestRule
      .onNodeWithTag(SHOW_DATE_PICKER_FORM_TAG, useUnmergedTree = true)
      .assertExists()
      .performClick()
      .assertIsDisplayed()
  }

  @Test
  fun testPleaseWaitDisplayedCorrectly() {
    composeTestRule.setContent {
      FixedMonthYearListing(
        onMonthSelected = mockRangeSelectListener,
        reportGenerationRange = dateRange,
        showProgressIndicator = true,
        innerPadding = PaddingValues(),
      )
    }
    composeTestRule
      .onNodeWithTag(PLEASE_WAIT_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .performClick()
      .assertIsDisplayed()
  }

  @Test
  fun testScreenTitleIsDisplayedCorrectly() {
    composeTestRule.setContent {
      ReportFilterSelector(
        screenTitle = "Measure Report",
        reportTypeState = mutableStateOf(MeasureReportType.SUMMARY),
        showFixedRangeSelection = true,
        showSubjectSelection = true,
        uiState = ReportTypeSelectorUiState(),
        dateRange = null,
        reportPeriodRange = mapOf(),
        onBackPressed = {},
        onSelectReportDate = {},
        onDateRangeSelected = {},
        onReportTypeSelected = {},
        onSubjectRemoved = {},
      )
    }
    composeTestRule.onNodeWithText("Measure Report").assertExists().assertIsDisplayed()
  }

  @Test
  fun testCorrectHeaderYearDisplayedCorrectly() {
    composeTestRule.setContent {
      FixedMonthYearListing(
        onMonthSelected = mockRangeSelectListener,
        reportGenerationRange = dateRange,
        showProgressIndicator = false,
        innerPadding = PaddingValues(),
      )
    }
    composeTestRule
      .onNodeWithTag(YEAR_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .performClick()
      .assertIsDisplayed()

    composeTestRule.onNodeWithText("2022").assertExists().assertIsDisplayed()
  }

  @Test
  fun testCorrectHeaderMonthDisplayedCorrectly() {
    composeTestRule.setContent {
      FixedMonthYearListing(
        onMonthSelected = mockRangeSelectListener,
        reportGenerationRange = dateRange,
        showProgressIndicator = false,
        innerPadding = PaddingValues(),
      )
    }
    composeTestRule
      .onNodeWithTag(MONTH_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .performClick()
      .assertIsDisplayed()

    composeTestRule.onNodeWithText("March").assertExists().assertIsDisplayed()
  }

  private fun defaultDateRangeState() =
    androidx.core.util.Pair(
      MaterialDatePicker.thisMonthInUtcMilliseconds(),
      MaterialDatePicker.todayInUtcMilliseconds(),
    )
}
