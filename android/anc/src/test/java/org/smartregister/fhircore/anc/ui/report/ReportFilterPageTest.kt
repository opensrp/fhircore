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
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
class ReportFilterPageTest : RobolectricTest() {

  private lateinit var viewModel: ReportViewModel
  @get:Rule val composeRule = createComposeRule()
  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  private val patientSelectionType = MutableLiveData("")
  private val testMeasureReportItem = MutableLiveData(ReportItem(title = "Test Report Title"))
  private val selectionPatient =
    MutableLiveData(
      PatientItem(
        name = "Test Patient Name",
        patientIdentifier = "1209875",
        familyName = "Test patient name"
      )
    )
  private val listenerObjectSpy =
    spyk(
      object {
        // Imitate click action by doing nothing
        fun onGenerateReportClick() {}
      }
    )

  @Before
  fun setUp() {
    viewModel =
      mockk {
        every { selectedMeasureReportItem } returns this@ReportFilterPageTest.testMeasureReportItem
        every { isReadyToGenerateReport } returns MutableLiveData(true)
        every { startDate } returns MutableLiveData("")
        every { endDate } returns MutableLiveData("")
        every { patientSelectionType } returns this@ReportFilterPageTest.patientSelectionType
        every { selectedPatientItem } returns this@ReportFilterPageTest.selectionPatient
        every { getSelectedPatient() } returns this@ReportFilterPageTest.selectionPatient
      }
  }

  @Test
  fun testReportFilterScreen() {
    composeRule.setContent { ReportFilterScreen(viewModel = viewModel) }
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertTextEquals("Test Report Title")
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
  }

  @Test
  fun testReportFilterPage() {
    composeRule.setContent {
      ReportFilterPage(
        topBarTitle = "FilterPageReportTitle",
        onBackPress = {},
        startDate = "",
        endDate = "",
        selectedPatient = selectionPatient.value
      )
    }
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertTextEquals("FilterPageReportTitle")
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
    composeRule.onNodeWithTag(REPORT_FILTER_PAGE).assertExists()
    composeRule.onNodeWithTag(REPORT_DATE_RANGE_SELECTION).assertExists()
    composeRule.onNodeWithTag(REPORT_GENERATE_BUTTON).assertExists()
    composeRule.onNodeWithTag(REPORT_GENERATE_BUTTON).performClick()
    verify { listenerObjectSpy.onGenerateReportClick() }
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
    composeRule.onNodeWithTag(REPORT_GENERATE_BUTTON).performClick()
    verify { listenerObjectSpy.onGenerateReportClick() }
  }
}
