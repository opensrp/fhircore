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

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
class ReportResultPageTest : RobolectricTest() {

  private lateinit var viewModel: ReportViewModel
  @get:Rule val composeRule = createComposeRule()
  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  private val testMeasureReportItem = MutableLiveData(ReportItem(title = "Report Result Title"))
  private val patientSelectionType = MutableLiveData("")
  private val selectedPatient = MutableLiveData(PatientItem(name = "Test Patient Name"))

  @Before
  fun setUp() {
    viewModel =
      mockk {
        every { selectedMeasureReportItem } returns this@ReportResultPageTest.testMeasureReportItem
        every { isReadyToGenerateReport } returns MutableLiveData(true)
        every { startDate } returns MutableLiveData("")
        every { endDate } returns MutableLiveData("")
        every { patientSelectionType } returns this@ReportResultPageTest.patientSelectionType
        every { selectedPatientItem } returns this@ReportResultPageTest.selectedPatient
      }
  }

  @Test
  fun testReportResultScreen() {
    composeRule.setContent { ReportResultScreen(viewModel = viewModel) }
    // toolbar should have valid title and icon
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertTextEquals("Report Result Title")
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
  }

  @Test
  fun testReportPageView() {
    composeRule.setContent {
      ReportResultPage(
        topBarTitle = "FilterResultReportTitle",
        onBackPress = {},
        reportMeasureItem = ReportItem(),
        startDate = "",
        endDate = "",
        isAllPatientSelection = true,
        selectedPatient = PatientItem()
      )
    }
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertTextEquals("FilterResultReportTitle")
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
    composeRule.onNodeWithTag(REPORT_DATE_RANGE_SELECTION).assertExists()
  }

  @Test
  fun testResultItemIndividual() {
    composeRule.setContent { ResultItemIndividual(selectedPatient = PatientItem()) }
    composeRule.onNodeWithTag(REPORT_RESULT_ITEM_INDIVIDUAL).assertExists()
    composeRule.onNodeWithTag(REPORT_RESULT_PATIENT_DATA).assertExists()
  }
}
