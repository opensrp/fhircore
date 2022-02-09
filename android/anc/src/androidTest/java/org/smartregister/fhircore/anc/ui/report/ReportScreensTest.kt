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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem

@ExperimentalCoroutinesApi
class ReportScreensTest {

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @get:Rule(order = 2) var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val listenerObjectSpy =
    spyk(
      object {
        // Imitate click action by doing nothing
        fun onBackPress() {}
        fun onReportMeasureItemClick() {}
        fun onDateRangeClick() {}
        fun onPatientSelectionChanged(patientSelectionType: String) {}
        fun onCancelSelectedPatient() {}
        fun onPatientChangeClick() {}
      }
    )

  @Test
  fun testReportMeasureList() {
    composeRule.setContent {
      ReportHomeListBox(dataList = emptyFlow(), onReportMeasureItemClick = {})
    }
    composeRule.onNodeWithTag(REPORT_MEASURE_LIST).assertExists()
  }

  @Test
  fun testReportMeasureItem() {
    composeRule.setContent { ReportRow(reportItem = ReportItem("test")) }
    composeRule.onNodeWithTag(REPORT_MEASURE_ITEM).assertExists()
  }

  @Test
  fun testTopBarBox() {
    composeRule.setContent {
      TopBarBox(topBarTitle = "test", onBackPress = { listenerObjectSpy.onBackPress() })
    }
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertExists()
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
  }

  @Test
  fun testDateRangeBoxInFilter() {
    composeRule.setContent {
      DateSelectionBox(
        startDate = "startDate",
        endDate = "endDate",
        canChange = true,
        showDateRangePicker = true,
        onDateRangeClick = listenerObjectSpy::onDateRangeClick
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
        showDateRangePicker = true,
        onDateRangeClick = listenerObjectSpy::onDateRangeClick
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
      )
    }
    composeRule.onNodeWithTag(REPORT_DATE_SELECT_ITEM).assertExists()
  }

  @Test
  fun testPatientSelectionForAll() {
    composeRule.setContent {
      PatientSelectionBox(
        reportType = "All",
        selectedPatient = null,
        onReportTypeSelected = { it, _ -> listenerObjectSpy.onPatientSelectionChanged(it) },
        radioOptions = listOf(Pair("All", true), Pair("Individual", false))
      )
    }
    composeRule.onNodeWithTag(REPORT_PATIENT_SELECTION).assertExists()
  }

  @Test
  fun testPatientSelectionForIndividual() {
    composeRule.setContent {
      PatientSelectionBox(
        reportType = "Individual",
        selectedPatient = PatientItem(),
        onReportTypeSelected = { it, _ -> listenerObjectSpy.onPatientSelectionChanged(it) },
        radioOptions = listOf(Pair("All", false), Pair("Individual", true))
      )
    }
    composeRule.onNodeWithTag(REPORT_PATIENT_SELECTION).assertExists()
    composeRule.onNodeWithTag(REPORT_PATIENT_ITEM).assertExists()
  }

  @Test
  fun testPatientSelectionChangeListener() {
    composeRule.setContent {
      PatientSelectionBox(
        reportType = "Individual",
        selectedPatient = PatientItem(),
        onReportTypeSelected = { reportType, _ ->
          listenerObjectSpy.onPatientSelectionChanged(reportType)
        },
        radioOptions = listOf(Pair("All", false), Pair("Individual", true))
      )
    }
    composeRule.onNodeWithTag(REPORT_CHANGE_PATIENT).assertExists()
    composeRule.onNodeWithTag(REPORT_CANCEL_PATIENT).assertExists()
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
    composeRule.onNodeWithTag(REPORT_CHANGE_PATIENT).assertExists()
  }
}
