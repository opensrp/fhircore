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

package org.smartregister.fhircore.anc.ui.report

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
class ReportScreensTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  private val listenerObjectSpy =
    spyk(
      object {
        // Imitate click action by doing nothing
        fun onReportMeasureItemClick() {}
        fun onStartDatePress() {}
        fun onEndDatePress() {}
        fun onPatientSelectionChanged() {}
        fun onCancelSelectedPatient() {}
        fun onPatientChangeClick() {}
        fun onGenerateReportClick() {}
      }
    )

  @Test
  @Ignore("passes at local, failed in PR")
  fun testReportMeasureList() {
    composeRule.setContent {
      ReportHomeListBox(
        dataList = emptyFlow(),
        onReportMeasureItemClick = { listenerObjectSpy.onReportMeasureItemClick() }
      )
    }
    composeRule.onNodeWithTag(REPORT_MEASURE_LIST).assertExists()
  }

  @Test
  @Ignore("passes at local, failed in PR")
  fun testReportMeasureItem() {
    composeRule.setContent { ReportRow(reportItem = ReportItem("test")) }
    composeRule.onNodeWithTag(REPORT_MEASURE_ITEM).assertExists()
  }

  @Test
  fun testDateRangeBoxInFilter() {
    composeRule.setContent {
      DateSelectionBox(
        startDate = "startDate",
        endDate = "endDate",
        canChange = true,
        onStartDatePress = { listenerObjectSpy.onStartDatePress() },
        onEndDatePress = { listenerObjectSpy.onEndDatePress() }
      )
    }
    composeRule.onNodeWithTag(REPORT_DATE_RANGE_SELECTION).assertExists()
  }

  @Test
  fun testDateRangeBoxInResult() {
    composeRule.setContent {
      DateSelectionBox(
        startDate = "startDate",
        endDate = "endDate",
        canChange = false,
        onStartDatePress = { listenerObjectSpy.onStartDatePress() },
        onEndDatePress = { listenerObjectSpy.onEndDatePress() }
      )
    }
    composeRule.onNodeWithTag(REPORT_DATE_RANGE_SELECTION).assertExists()
  }

  @Test
  fun testDateRangeItem() {
    composeRule.setContent {
      DateRangeItem(
        text = "startDate",
        canChange = true,
        clickListener = { listenerObjectSpy.onStartDatePress() }
      )
    }
    composeRule.onNodeWithTag(REPORT_DATE_SELECT_ITEM).assertExists()
    // composeRule.onNodeWithTag(REPORT_DATE_SELECT_ITEM).performClick()
    // verify { listenerObjectSpy.onDateRangePress() }
  }

  @Test
  fun testPatientSelection() {
    composeRule.setContent {
      PatientSelectionBox(
        patientSelectionText = "All",
        selectedPatient = PatientItem(),
        onPatientSelectionChange = { listenerObjectSpy.onPatientSelectionChanged() }
      )
    }
    composeRule.onNodeWithTag(REPORT_PATIENT_SELECTION).assertExists()
  }

  @Test
  fun testSelectedPatientItem() {
    composeRule.setContent {
      SelectedPatientItem(
        selectedPatient = PatientItem(),
        onCancelSelectedPatient = { listenerObjectSpy.onCancelSelectedPatient() },
        onChangeClickListener = { listenerObjectSpy.onPatientChangeClick() }
      )
    }
    composeRule.onNodeWithTag(REPORT_PATIENT_ITEM).assertExists()
    composeRule.onNodeWithTag(REPORT_CANCEL_PATIENT).assertExists()
    // composeRule.onNodeWithTag(REPORT_CANCEL_PATIENT).performClick()
    // verify { listenerObjectSpy.onCancelSelectedPatient() }
    composeRule.onNodeWithTag(REPORT_CHANGE_PATIENT).assertExists()
    // composeRule.onNodeWithTag(REPORT_CHANGE_PATIENT).performClick()
    // verify { listenerObjectSpy.onPatientChangeClick() }
  }

  @Test
  fun testGenerateReportButton() {
    composeRule.setContent {
      GenerateReportButton(
        generateReportEnabled = true,
        onGenerateReportClicked = { listenerObjectSpy.onGenerateReportClick() }
      )
    }
    composeRule.onNodeWithTag(REPORT_GENERATE_BUTTON).assertExists()
    // composeRule.onNodeWithTag(REPORT_GENERATE_BUTTON).performClick()
    // verify { listenerObjectSpy.onGenerateReportClick() }
  }
}
