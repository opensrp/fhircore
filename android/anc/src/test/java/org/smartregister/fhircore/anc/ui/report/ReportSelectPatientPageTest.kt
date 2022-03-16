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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.register.components.AncRow
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType

@ExperimentalCoroutinesApi
class ReportSelectPatientPageTest : RobolectricTest() {
  @get:Rule(order = 1) val composeRule = createComposeRule()

  @get:Rule(order = 2) var instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var reportViewModel: ReportViewModel

  private lateinit var registerDataViewModel: RegisterDataViewModel<Anc, PatientItem>

  private val selectedPatient = MutableLiveData(PatientItem(name = "Test Patient Name"))

  private val searchTextState = mutableStateOf(TextFieldValue(""))

  private val searchTextStateWithText = mutableStateOf(TextFieldValue("hello"))

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

    reportViewModel =
      mockk {
        every { selectedMeasureReportItem } returns MutableLiveData(ReportItem())
        every { startDate } returns MutableLiveData("")
        every { endDate } returns MutableLiveData("")
        every { filterValue } returns MutableLiveData<Pair<RegisterFilterType, Any?>>()
        every { currentScreen } returns ReportViewModel.ReportScreen.FILTER
        every { selectedPatientItem } returns this@ReportSelectPatientPageTest.selectedPatient
        every { searchTextState } returns this@ReportSelectPatientPageTest.searchTextState
        every { onBackPress(ReportViewModel.ReportScreen.FILTER) } returns Unit
        every { currentReportType } returns MutableLiveData("")
      }

    registerDataViewModel =
      mockk {
        every { registerData } returns allRegisterData
        every { showHeader } returns MutableLiveData(true)
        every { showFooter } returns MutableLiveData(true)
        every { showResultsCount } returns MutableLiveData(false)
        every { showLoader } returns MutableLiveData(false)
        every { currentPage() } returns 1
        every { countPages() } returns 1
        every { previousPage() } returns listenerObjectSpy.previousPage()
        every { nextPage() } returns listenerObjectSpy.nextPage()
      }
  }

  @Test
  @Ignore("failing on PR though passes locally")
  fun testReportSelectPatientListPage() {
    composeRule.setContent {
      ReportSelectPatientScreen(
        viewModel = reportViewModel,
        registerDataViewModel = registerDataViewModel
      )
    }
    composeRule.onNodeWithTag(REPORT_SELECT_PATIENT_LIST).assertExists()
    composeRule.onNodeWithTag(REPORT_SEARCH_PATIENT).assertExists()
  }

  @Test
  @Ignore("failing on PR though passes locally")
  fun testReportSelectPatientSearchView() {
    composeRule.setContent { SearchView(searchTextState, viewModel = reportViewModel) }
    composeRule.onNodeWithTag(REPORT_SEARCH_PATIENT).assertExists().assertTextContains("")
    composeRule.onNodeWithTag(REPORT_SEARCH_PATIENT).assertExists().performTextInput("input")
  }

  @Test
  fun testReportSelectPatientSearchViewWithText() {
    composeRule.setContent { SearchView(searchTextStateWithText, viewModel = reportViewModel) }
    composeRule.onNodeWithTag(REPORT_SEARCH_PATIENT_CANCEL).assertExists().performClick()
  }

  @Test
  fun testReportSelectPatientSearchViewWithBackArrow() {
    composeRule.setContent { SearchView(searchTextStateWithText, viewModel = reportViewModel) }
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertExists()
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).performClick()
    Assert.assertEquals(ReportViewModel.ReportScreen.FILTER, reportViewModel.currentScreen)
    Assert.assertEquals("", reportViewModel.currentReportType.value)
  }

  @Test
  fun testReportSelectPatientSearchHint() {
    composeRule.setContent { SearchHint() }
    composeRule.onNodeWithTag(REPORT_SEARCH_HINT).assertExists()
  }

  @Test
  fun testPatientListItem() {
    val expectedPatient = selectedPatient.value ?: PatientItem(name = "Test SelectPatient")
    composeRule.setContent {
      AncRow(
        patientItem = expectedPatient,
        clickListener = { _, _ -> },
        showAncVisitButton = true,
        displaySelectContentOnly = true
      )
    }
    composeRule.onNodeWithTag(PATIENT_ANC_VISIT).assertExists()
  }
}
