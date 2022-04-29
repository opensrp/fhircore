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
import androidx.compose.ui.test.onNodeWithText
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
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
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.data.report.model.ResultItemPopulation
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class ReportResultPageTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @get:Rule(order = 2) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 3) val coroutinesTestRule = CoroutineTestRule()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private lateinit var viewModel: ReportViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    val fhirEngine = mockk<FhirEngine>(relaxed = true)
    val fhirOperator = mockk<FhirOperator>(relaxed = true)
    val reportRepository = mockk<ReportRepository>()
    val ancPatientRepository = mockk<PatientRepository>()
    viewModel =
      spyk(
        ReportViewModel(
          repository = reportRepository,
          dispatcher = coroutinesTestRule.testDispatcherProvider,
          patientRepository = ancPatientRepository,
          fhirEngine = fhirEngine,
          fhirOperator = fhirOperator,
          sharedPreferencesHelper = sharedPreferencesHelper
        )
      )
  }

  @After
  fun tearDown() {
    viewModel.resetValues()
  }

  @Test
  fun testReportResultScreen() {
    viewModel.selectedMeasureReportItem.value = ReportItem(title = "Report Result Title")
    composeRule.setContent { ReportResultScreen(viewModel = viewModel) }
    // toolbar should have valid title and icon
    composeRule.onNodeWithText("Report Result Title").assertExists()
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
        selectedPatient = PatientItem(),
        resultForIndividual = ResultItem(),
        resultItemPopulation = emptyList()
      )
    }
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertTextEquals("FilterResultReportTitle")
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
    composeRule.onNodeWithTag(REPORT_DATE_RANGE_SELECTION).assertExists()
    composeRule.onNodeWithTag(REPORT_RESULT_MEASURE_DESCRIPTION).assertExists()
  }

  @Test
  fun testResultItemIndividual() {
    composeRule.setContent { ResultItemIndividual(selectedPatient = PatientItem()) }
    composeRule.onNodeWithTag(REPORT_RESULT_ITEM_INDIVIDUAL).assertExists()
    composeRule.onNodeWithTag(REPORT_RESULT_PATIENT_DATA).assertExists()
  }

  @Test
  fun testResultItemIndividualWithIndicator() {
    composeRule.setContent {
      ResultItemIndividual(selectedPatient = PatientItem(), indicatorDescription = "show")
    }
    composeRule.onNodeWithTag(INDICATOR_TEXT).assertExists()
  }

  @Test
  fun testResultPopulationData() {
    composeRule.setContent { ResultForPopulation(emptyList()) }
    composeRule.onNodeWithTag(REPORT_RESULT_POPULATION_DATA).assertExists()
  }

  @Test
  fun testResultPopulationBox() {
    composeRule.setContent { ResultPopulationBox(ResultItemPopulation()) }
    composeRule.onNodeWithTag(REPORT_RESULT_POPULATION_BOX).assertExists()
  }

  @Test
  fun testResultPopulation() {
    composeRule.setContent { ResultPopulationItem(ResultItem(percentage = "10")) }
    composeRule.onNodeWithTag(REPORT_RESULT_POPULATION_ITEM).assertExists()
  }
}
