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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.cql.FhirOperatorDecorator
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class ReportFilterPageTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @get:Rule(order = 2) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 3) val coroutinesTestRule = CoroutineTestRule()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private lateinit var viewModel: ReportViewModel

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
    hiltAndroidRule.inject()
    val fhirEngine = mockk<FhirEngine>(relaxed = true)
    val fhirOperatorDecorator = mockk<FhirOperatorDecorator>(relaxed = true)
    val reportRepository = mockk<ReportRepository>()
    val ancPatientRepository = mockk<PatientRepository>()
    viewModel =
      spyk(
        ReportViewModel(
          repository = reportRepository,
          dispatcher = coroutinesTestRule.testDispatcherProvider,
          patientRepository = ancPatientRepository,
          fhirEngine = fhirEngine,
          fhirOperatorDecorator = fhirOperatorDecorator,
          sharedPreferencesHelper = sharedPreferencesHelper
        )
      )
  }

  @After
  fun tearDown() {
    viewModel.resetValues()
  }

  @Test
  fun testReportFilterScreen() {
    viewModel.selectedMeasureReportItem.value = ReportItem(title = "Test Report Title")
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
        selectedPatient = selectionPatient.value,
        generateReport = true,
        onDateRangeClick = {},
        reportType = "All",
        onReportTypeSelected = { _, _ -> },
        onGenerateReportClicked = listenerObjectSpy::onGenerateReportClick
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
  fun testReportFilterPageWithIndicator() {
    composeRule.setContent {
      ReportFilterPage(
        topBarTitle = "FilterPageReportTitle",
        onBackPress = {},
        startDate = "",
        endDate = "",
        selectedPatient = selectionPatient.value,
        generateReport = true,
        onDateRangeClick = {},
        reportType = "All",
        onReportTypeSelected = { _, _ -> },
        onGenerateReportClicked = {},
        showProgressIndicator = true
      )
    }
    composeRule.onNodeWithTag(PROGRESS_BAR_TEXT).assertTextEquals("Please wait…")
    composeRule.onNodeWithTag(PROGRESS_BAR).assertExists()
    composeRule.onNodeWithTag(PROGRESS_BAR_COLUMN).assertExists()
    composeRule.onNodeWithTag(PROGRESS_BAR_TEXT).assertExists()
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
