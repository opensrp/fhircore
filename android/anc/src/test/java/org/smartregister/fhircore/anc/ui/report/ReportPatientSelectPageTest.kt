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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
class ReportPatientSelectPageTest : RobolectricTest() {

  private lateinit var viewModel: ReportViewModel
  @get:Rule val composeRule = createComposeRule()
  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  private val patientSelectionType = MutableLiveData("")
  private val testMeasureReportItem = MutableLiveData(ReportItem(title = "Test Report Title"))
  private val selectionPatient = MutableLiveData(PatientItem(name = "Test Patient Name"))
  private val searchTextState = mutableStateOf(TextFieldValue(""))
  private val listenerObjectSpy =
    spyk(
      object {
        // Imitate click action by doing nothing
        fun previousPage() {}
        fun nextPage() {}
      }
    )

  @Before
  fun setUp() {
    val allRegisterData: MutableStateFlow<Flow<PagingData<PatientItem>>> =
      MutableStateFlow(emptyFlow())

    viewModel =
      mockk {
        every { selectedMeasureReportItem } returns
          this@ReportPatientSelectPageTest.testMeasureReportItem
        every { isReadyToGenerateReport } returns MutableLiveData(true)
        every { startDate } returns MutableLiveData("")
        every { endDate } returns MutableLiveData("")
        every { patientSelectionType } returns this@ReportPatientSelectPageTest.patientSelectionType
        every { selectedPatientItem } returns this@ReportPatientSelectPageTest.selectionPatient
        every { searchTextState } returns this@ReportPatientSelectPageTest.searchTextState
        every { registerDataViewModel } returns
          mockk {
            every { registerData } returns allRegisterData
            every { showResultsCount } returns MutableLiveData(false)
            every { showLoader } returns MutableLiveData(false)
            every { currentPage() } returns 1
            every { countPages() } returns 1
            every { previousPage() } returns listenerObjectSpy.previousPage()
            every { nextPage() } returns listenerObjectSpy.nextPage()
          }
      }
  }

  @Test
  fun testReportSelectPatientListPage() {
    composeRule.setContent { ReportSelectPatientScreen(viewModel = viewModel) }
    composeRule.onNodeWithTag(REPORT_SELECT_PATIENT_LIST).assertExists()
    composeRule.onNodeWithTag(REPORT_SEARCH_PATIENT).assertExists()
  }

  @Test
  fun testReportSelectPatientSearchView() {
    composeRule.setContent { SearchView(searchTextState, viewModel = viewModel) }
    composeRule.onNodeWithTag(REPORT_SEARCH_PATIENT).assertExists()
  }
}
