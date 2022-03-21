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

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.hl7.fhir.r4.model.MeasureReport
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.cql.FhirOperatorDecorator
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class ReportScreensTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @get:Rule(order = 2) val coroutinesTestRule = CoroutineTestRule()

  @get:Rule(order = 3) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @BindValue lateinit var reportViewModel: ReportViewModel

  @Inject lateinit var reportRepository: ReportRepository
  @Inject lateinit var patientRepository: PatientRepository
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private lateinit var registerDataViewModel: RegisterDataViewModel<Anc, PatientItem>
  private val fhirEngine: FhirEngine = spyk()
  private val fhirOperatorDecorator: FhirOperatorDecorator = mockk()

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

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    reportViewModel =
      spyk(
        ReportViewModel(
          repository = reportRepository,
          dispatcher = coroutinesTestRule.testDispatcherProvider,
          patientRepository = patientRepository,
          fhirEngine = fhirEngine,
          fhirOperatorDecorator = fhirOperatorDecorator,
          sharedPreferencesHelper = sharedPreferencesHelper
        )
      )
    val allRegisterData: MutableStateFlow<Flow<PagingData<PatientItem>>> =
      MutableStateFlow(emptyFlow())

    every {
      fhirOperatorDecorator.evaluateMeasure(any(), any(), any(), any(), any(), any())
    } returns
      MeasureReport().apply {
        status = MeasureReport.MeasureReportStatus.COMPLETE
        type = MeasureReport.MeasureReportType.INDIVIDUAL
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
        every { filterRegisterData(any(), any(), any()) } returns Unit
      }

    every { reportViewModel.selectedMeasureReportItem } returns
      MutableLiveData(ReportItem(name = "First ANC", reportType = "Individual"))
  }

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

  @Test
  fun testReportView() {
    every { reportViewModel.currentScreen } returns ReportViewModel.ReportScreen.RESULT

    composeRule.setContent {
      ReportView(reportViewModel = reportViewModel, registerDataViewModel = registerDataViewModel)
    }
  }

  @Test
  fun testReportViewHome() {
    every { reportViewModel.currentScreen } returns ReportViewModel.ReportScreen.HOME

    composeRule.setContent {
      ReportView(reportViewModel = reportViewModel, registerDataViewModel = registerDataViewModel)
    }
  }

  @Test
  fun testReportViewFilter() {
    every { reportViewModel.currentScreen } returns ReportViewModel.ReportScreen.FILTER

    composeRule.setContent {
      ReportView(reportViewModel = reportViewModel, registerDataViewModel = registerDataViewModel)
    }
  }

  @Test
  fun testReportViewPickPatient() {
    every { reportViewModel.currentScreen } returns ReportViewModel.ReportScreen.PICK_PATIENT

    composeRule.setContent {
      ReportView(reportViewModel = reportViewModel, registerDataViewModel = registerDataViewModel)
    }
  }

  @Test
  fun testReportResultScreen() {
    composeRule.setContent { ReportResultScreen(viewModel = reportViewModel) }
  }
}
