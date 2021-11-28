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
import androidx.lifecycle.MutableLiveData
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.data.report.model.ResultItemPopulation
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource

@ExperimentalCoroutinesApi
internal class ReportViewModelTest {

  private lateinit var fhirEngine: FhirEngine
  private lateinit var reportRepository: ReportRepository
  private lateinit var ancPatientRepository: PatientRepository
  private lateinit var reportViewModel: ReportViewModel
  @MockK lateinit var parser: IParser
  @MockK lateinit var fhirResourceDataSource: FhirResourceDataSource
  @MockK lateinit var resource: Resource
  @MockK lateinit var entryList: List<Bundle.BundleEntryComponent>
  @MockK lateinit var bundle: Bundle

  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()
  private val testReportItem = ReportItem(title = "TestReportItem")
  private val patientSelectionType = MutableLiveData("All")
  private val isChangingStartDate = MutableLiveData(true)
  private val isChangingEndDate = MutableLiveData(true)
  private val resultForIndividual =
    MutableLiveData(ResultItem(status = "True", isMatchedIndicator = true))
  private val resultForPopulation =
    MutableLiveData(listOf(ResultItemPopulation(title = "resultForPopulation")))

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    fhirEngine = mockk(relaxed = true)
    reportRepository = mockk()
    ancPatientRepository = mockk()
    reportViewModel =
      spyk(
        ReportViewModel(
          reportRepository,
          ancPatientRepository,
          coroutinesTestRule.testDispatcherProvider
        )
      )
    every { reportViewModel.patientSelectionType } returns
      this@ReportViewModelTest.patientSelectionType
    every { reportViewModel.resultForIndividual } returns
      this@ReportViewModelTest.resultForIndividual
    every { reportViewModel.resultForPopulation } returns
      this@ReportViewModelTest.resultForPopulation
  }

  @Test
  fun testShouldVerifyBackClickListener() {
    reportViewModel.onBackPress()
    Assert.assertEquals(true, reportViewModel.backPress.value)
  }

  @Test
  fun testFetchCQLLibraryData() {
    val auxCQLLibraryData = "Library JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLLibraryData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLLibraryData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLLibraryData, libraryDataLiveData)
  }

  @Test
  fun testFetchCQLFhirHelperData() {
    val auxCQLHelperData = "Helper JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLHelperData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLFhirHelperData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLHelperData, libraryDataLiveData)
  }

  @Test
  fun testFetchCQLValueSetData() {
    val auxCQLValueSetData = "ValueSet JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { parser.encodeResourceToString(bundle) } returns auxCQLValueSetData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLValueSetData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLValueSetData, libraryDataLiveData)
  }

  @Test
  fun testFetchCQLPatientData() {
    val auxCQLValueSetData = "Patient Data JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { parser.encodeResourceToString(bundle) } returns auxCQLValueSetData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLPatientData(parser, fhirResourceDataSource, "1").value!!
    Assert.assertEquals(auxCQLValueSetData, libraryDataLiveData)
  }

  @Test
  fun testFetchCQLMeasureEvaluateLibraryAndValueSets() {
    val auxCQLLibraryAndValueSetData = "{\"parameters\":\"parameters\"}"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLLibraryAndValueSetData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLMeasureEvaluateLibraryAndValueSets(
          parser,
          fhirResourceDataSource,
          "https://hapi.fhir.org/baseR4/Library?_id=ANCDataElements,WHOCommon,ANCConcepts,ANCContactDataElements,FHIRHelpers,ANCStratifiers,ANCIND01,ANCCommon,ANCBaseDataElements,FHIRCommon,ANCBaseConcepts",
          "https://hapi.fhir.org/baseR4/Measure?_id=ANCIND01",
          ""
        )
        .value!!
    Assert.assertNotNull(libraryDataLiveData)
  }

  @Test
  fun testShouldVerifyStartDatePressListener() {
    reportViewModel.onStartDatePress()
    Assert.assertEquals(true, reportViewModel.isChangingStartDate.value)
    Assert.assertEquals(true, reportViewModel.showDatePicker.value)
  }

  @Test
  fun testShouldVerifyEndDatePressListener() {
    reportViewModel.onEndDatePress()
    Assert.assertEquals(true, reportViewModel.showDatePicker.value)
    Assert.assertEquals(false, reportViewModel.isChangingStartDate.value)
  }

  @Test
  fun testShouldVerifyBackFromFilterClickListener() {
    reportViewModel.onBackPressFromFilter()
    Assert.assertEquals(
      ReportViewModel.ReportScreen.HOME,
      reportViewModel.reportState.currentScreen
    )
  }

  @Test
  fun testShouldVerifyBackFromPatientSelection() {
    reportViewModel.onBackPressFromPatientSearch()
    Assert.assertEquals(
      ReportViewModel.ReportScreen.FILTER,
      reportViewModel.reportState.currentScreen
    )
  }

  @Test
  fun testShouldVerifyBackFromResultClickListener() {
    reportViewModel.onBackPressFromResult()
    Assert.assertEquals(
      ReportViewModel.ReportScreen.FILTER,
      reportViewModel.reportState.currentScreen
    )
  }

  @Test
  fun testGetSelectionDate() {
    Assert.assertNotNull(reportViewModel.getSelectionDate())
  }

  @Test
  fun testLoadDummyResultData() {
    Assert.assertNotNull(reportViewModel.loadDummyResultForPopulation())
  }

  @Test
  fun testShouldVerifyReportItemClickListener() {
    val expectedReportItem = testReportItem
    reportViewModel.onReportMeasureItemClicked(testReportItem)
    Assert.assertEquals(expectedReportItem, reportViewModel.selectedMeasureReportItem.value)
    Assert.assertEquals(
      ReportViewModel.ReportScreen.FILTER,
      reportViewModel.reportState.currentScreen
    )
  }

  @Test
  fun testShouldVerifyPatientSelectionChanged() {
    val expectedSelection = ReportViewModel.PatientSelectionType.ALL
    reportViewModel.onPatientSelectionTypeChanged("All")
    Assert.assertEquals(expectedSelection, reportViewModel.patientSelectionType.value)
  }

  @Test
  fun testShouldVerifyGenerateReportClickListener() {
    reportViewModel.onGenerateReportPress()
    Assert.assertEquals(
      ReportViewModel.ReportScreen.RESULT,
      reportViewModel.reportState.currentScreen
    )
  }

  @Test
  fun testVerifyOnDatePickedForStartAndEndDate() {
    //  2021-11-11 16:04:43.212 E/aw: onDatePicked-> start=1637798400000 end=1639094400000
    //  25 Nov, 2021  -  10 Dec, 2021
    //  val dateSelection = androidx.core.util.Pair(1637798400000, 1639094400000)
    val expectedStartDate = "25 Nov, 2021"
    val expectedEndDate = "10 Dec, 2021"

    every { reportViewModel.isChangingStartDate } returns
      this@ReportViewModelTest.isChangingStartDate
    every { reportViewModel.startDate.value } returns "25 Nov, 2021"
    every { reportViewModel.endDate.value } returns "10 Dec, 2021"

    reportViewModel.onDatePicked(1637798400000)
    Assert.assertEquals(expectedStartDate, reportViewModel.startDate.value)

    every { reportViewModel.isChangingStartDate } returns this@ReportViewModelTest.isChangingEndDate
    reportViewModel.onDatePicked(1639094400000)
    Assert.assertEquals(expectedEndDate, reportViewModel.endDate.value)

    Assert.assertEquals(true, reportViewModel.isReadyToGenerateReport.value)
    Assert.assertEquals(
      ReportViewModel.ReportScreen.FILTER,
      reportViewModel.reportState.currentScreen
    )
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
}
