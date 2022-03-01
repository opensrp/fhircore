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
import androidx.core.util.Pair
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.MeasureReport
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
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
internal class ReportViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 2) val coroutinesTestRule = CoroutineTestRule()

  @get:Rule(order = 3) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private lateinit var fhirEngine: FhirEngine
  private lateinit var reportRepository: ReportRepository
  private lateinit var ancPatientRepository: PatientRepository
  private lateinit var reportViewModel: ReportViewModel
  private val testReportItem = ReportItem(title = "TestReportItem")
  private val selectedPatient =
    MutableLiveData(PatientItem(patientIdentifier = "Select Patient", name = "Select Patient"))
  private val resultForIndividual =
    MutableLiveData(ResultItem(status = "True", isMatchedIndicator = true))
  private val resultForPopulation =
    MutableLiveData(listOf(ResultItemPopulation(title = "resultForPopulation")))
  private val fhirOperator = mockk<FhirOperator>()

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = mockk(relaxed = true)
    reportRepository = mockk()
    ancPatientRepository = mockk()
    reportViewModel =
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
    every { reportViewModel.resultForIndividual } returns
      this@ReportViewModelTest.resultForIndividual
    every { reportViewModel.resultForPopulation } returns
      this@ReportViewModelTest.resultForPopulation
    every { reportViewModel.selectedPatientItem } returns this@ReportViewModelTest.selectedPatient
  }

  @After
  fun tearDown() {
    reportViewModel
  }

  @Test
  fun testShouldVerifyBackClickListener() {
    reportViewModel.onBackPress()
    Assert.assertEquals(true, reportViewModel.backPress.value)
  }

  @Test
  fun testShouldVerifyChangeDatePickerPressListener() {
    reportViewModel.onDateRangeClick()
    Assert.assertEquals(true, reportViewModel.showDatePicker.value)
  }

  @Test
  fun testShouldVerifyBackFromFilterClickListener() {
    reportViewModel.onBackPress(ReportViewModel.ReportScreen.FILTER)
    Assert.assertEquals(ReportViewModel.ReportScreen.FILTER, reportViewModel.currentScreen)
  }

  @Test
  fun testShouldVerifyBackFromPatientSelection() {
    reportViewModel.onBackPress(ReportViewModel.ReportScreen.PICK_PATIENT)
    Assert.assertEquals(ReportViewModel.ReportScreen.PICK_PATIENT, reportViewModel.currentScreen)
  }

  @Test
  fun testShouldVerifyBackFromResultClickListener() {
    reportViewModel.onBackPress(ReportViewModel.ReportScreen.RESULT)
    Assert.assertEquals(ReportViewModel.ReportScreen.RESULT, reportViewModel.currentScreen)
  }

  @Test
  fun testShouldVerifyReportItemClickListener() {
    val expectedReportItem = testReportItem
    reportViewModel.onReportMeasureItemClicked(testReportItem)
    Assert.assertEquals(expectedReportItem, reportViewModel.selectedMeasureReportItem.value)
    Assert.assertEquals(ReportViewModel.ReportScreen.FILTER, reportViewModel.currentScreen)
  }

  @Test
  fun testShouldVerifyPatientSelectionChanged() {
    val reportType = "All"
    reportViewModel.onReportTypeSelected(reportType, true)
    Assert.assertEquals(reportType, reportViewModel.currentReportType.value)
  }

  @Test
  fun testShouldVerifyGenerateReportClickListener() {
    reportViewModel.onGenerateReportClicked()
    Assert.assertTrue(reportViewModel.onGenerateReportClicked.value!!)
  }

  @Test
  fun testVerifyOnDatePickedForStartAndEndDate() {
    //  2021-11-11 16:04:43.212 E/aw: onDatePicked-> start=1637798400000 end=1639094400000
    //  25 Nov, 2021  -  10 Dec, 2021
    //  val dateSelection = androidx.core.util.Pair(1637798400000, 1639094400000)
    val expectedStartDate = "25 Nov, 2021"
    val expectedEndDate = "10 Dec, 2021"

    reportViewModel.currentReportType.value = "All"
    reportViewModel.setDateRange(Pair(1637798400000L, 1639094400000))
    Assert.assertEquals(expectedStartDate, reportViewModel.startDate.value)
    Assert.assertEquals(expectedEndDate, reportViewModel.endDate.value)
    Assert.assertEquals(true, reportViewModel.generateReport.value)
  }

  @Test
  fun testReportResultForIndividual() {
    val expectedResult = ResultItem(status = "True", isMatchedIndicator = true)
    Assert.assertEquals(expectedResult, reportViewModel.resultForIndividual.value)
  }

  @Test
  fun testReportResultForPopulation() {
    val expectedResult = listOf(ResultItemPopulation(title = "resultForPopulation"))
    Assert.assertEquals(expectedResult, reportViewModel.resultForPopulation.value)
  }

  @Test
  fun testGetSelectedPatient() {
    reportViewModel.updateSelectedPatient(PatientItem())
    Assert.assertNotNull(reportViewModel.getSelectedPatient().value)
  }

  @Test
  fun testEvaluateMeasureForIndividual() {
    reportViewModel.evaluateMeasure(
      context = ApplicationProvider.getApplicationContext(),
      measureUrl = "measure/ancInd03",
      individualEvaluation = true,
      measureResourceBundleUrl = "measure/ancInd03"
    )
    Assert.assertNotNull(reportViewModel.resultForIndividual.value)
    Assert.assertFalse(reportViewModel.showProgressIndicator.value!!)
  }

  @Test
  fun testEvaluateMeasureForPopulation() {
    reportViewModel.evaluateMeasure(
      context = ApplicationProvider.getApplicationContext(),
      measureUrl = "measure/ancInd03",
      individualEvaluation = false,
      measureResourceBundleUrl = "measure/ancInd03"
    )
    Assert.assertNotNull(reportViewModel.resultForPopulation.value)
    Assert.assertFalse(reportViewModel.showProgressIndicator.value!!)
    Assert.assertNotNull(
      sharedPreferencesHelper.read(SharedPreferencesHelper.MEASURE_RESOURCES_LOADED, "")
    )
  }

  @Test
  fun testFormatPopulationMeasureReport() {
    Assert.assertNotNull(reportViewModel.formatPopulationMeasureReport(getTestMeasureReport()))
  }

  private fun getTestMeasureReport(): MeasureReport {
    return MeasureReport().apply {
      addGroup().apply { stratifier = listOf(addStratifier().apply { id = "123" }) }
    }
  }

  @Test
  fun auxGenerateReportTestForIndividual() {
    every { reportViewModel.selectedPatientItem } returns this@ReportViewModelTest.selectedPatient
    reportViewModel.onReportTypeSelected("Individual", true)
    Assert.assertEquals(ReportViewModel.ReportScreen.PICK_PATIENT, reportViewModel.currentScreen)
  }

  @Ignore("should work but")
  @Test
  fun testStringReplaceDashed() {
    val testString: String = "hel-lo"
    val resultString = "" // testString.replaceDashes()
    val expectedString = "HELLO"
    Assert.assertEquals(expectedString, resultString)
  }

  @Test
  fun testOnReportTypeSelected() {
    reportViewModel.onReportTypeSelected("ancInd01", true)
    Assert.assertNotNull(reportViewModel.filterValue.value)
    Assert.assertEquals(reportViewModel.currentScreen, ReportViewModel.ReportScreen.PICK_PATIENT)
  }

  @Test
  fun testOnClear() {
    reportViewModel.resetValues()
    Assert.assertEquals("", reportViewModel.currentReportType.value)
  }
}
