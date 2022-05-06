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

import android.app.Activity
import android.content.Context
import android.os.Looper
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
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.anc.app.fakes.FakeModel
import org.smartregister.fhircore.anc.app.fakes.Faker
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.anc.util.AncJsonSpecificationProvider
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class ReportHomeActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val coroutinesTestRule = CoroutineTestRule()

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("anc", mockk())
  @Inject lateinit var jsonSpecificationProvider: AncJsonSpecificationProvider

  @Inject lateinit var reportRepository: ReportRepository
  @Inject lateinit var patientRepository: PatientRepository
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @BindValue lateinit var reportViewModel: ReportViewModel
  private lateinit var registerDataViewModel: RegisterDataViewModel<Anc, PatientItem>
  private lateinit var reportHomeActivity: ReportHomeActivity
  private lateinit var reportHomeActivitySpy: ReportHomeActivity
  private val fhirEngine: FhirEngine = spyk()
  private val fhirOperatorDecorator: FhirOperatorDecorator = mockk()

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
        every { showResultsCount } returns MutableLiveData(false)
        every { showLoader } returns MutableLiveData(false)
        every { currentPage() } returns 1
        every { countPages() } returns 1
        every { filterRegisterData(any(), any(), any()) } returns Unit
      }

    reportHomeActivity =
      spyk(Robolectric.buildActivity(ReportHomeActivity::class.java).create().resume().get())
    reportHomeActivitySpy = spyk(objToCopy = reportHomeActivity)
    every { reportHomeActivitySpy.reportViewModel } returns reportViewModel
    every { reportViewModel.selectedMeasureReportItem } returns
      MutableLiveData(ReportItem(name = "First ANC", reportType = "Individual"))
  }

  override fun tearDown() {
    shadowOf(Looper.getMainLooper()).idle()
    reportHomeActivitySpy.finish()
    reportHomeActivity.finish()
  }

  override fun getActivity(): Activity {
    return reportHomeActivity
  }

  @Test
  fun testActivityNotNull() {
    Assert.assertNotNull(reportHomeActivity)
  }

  @Test
  fun testShowDatePicker() {
    reportHomeActivitySpy.showDateRangePicker()
    // Date range was set when the dialog is displayed
    Assert.assertNotNull(reportViewModel.dateRange.value)
  }

  @Test
  fun testOnBackPressShouldCallFinish() {
    reportHomeActivity.reportViewModel.onBackPress()
    Assert.assertTrue(reportHomeActivitySpy.isFinishing)
  }

  @Test
  fun testOnDateRangeClickShouldShowDateRangePicker() {
    reportHomeActivity.reportViewModel.onDateRangeClick()
    // Date picker is displayed onDateRangeClick and date range was set when the dialog is displayed
    Assert.assertNotNull(reportViewModel.dateRange.value)
  }

  @Test
  fun testFilterRegisterData() {
    reportHomeActivity.reportViewModel.filterValue.postValue(
      Pair(RegisterFilterType.SEARCH_FILTER, "")
    )
    Assert.assertNotNull(reportHomeActivity.registerDataViewModel.showResultsCount.value)
  }

  @Test
  fun testFilterRegisterDataWithNullValue() {
    reportHomeActivity.reportViewModel.filterValue.postValue(
      Pair(RegisterFilterType.SEARCH_FILTER, null)
    )
    Assert.assertNotNull(reportHomeActivity.registerDataViewModel.showResultsCount.value)
  }

  @Test
  fun testGenerateReportForIndividual() {
    every { reportViewModel.currentReportType } returns MutableLiveData("Individual")
    every {
      fhirOperatorDecorator.evaluateMeasure(any(), any(), any(), any(), any(), any())
    } returns FakeModel.getMeasureReport(typeMR = MeasureReport.MeasureReportType.INDIVIDUAL)

    reportHomeActivity.reportViewModel.onGenerateReportClicked.postValue(true)
    Assert.assertNotNull(reportHomeActivity.reportViewModel.selectedMeasureReportItem.value!!.name)
    Assert.assertEquals(
      "Individual",
      reportHomeActivity.reportViewModel.selectedMeasureReportItem.value!!.reportType
    )
  }

  @Test
  fun testGenerateReportForAll() {
    every { reportViewModel.currentReportType } returns MutableLiveData("All")
    every { reportViewModel.selectedMeasureReportItem } returns
      MutableLiveData(ReportItem(name = "First ANC", reportType = "All"))
    every {
      fhirOperatorDecorator.evaluateMeasure(any(), any(), any(), any(), any(), any())
    } returns FakeModel.getMeasureReport(typeMR = MeasureReport.MeasureReportType.SUBJECTLIST)

    reportHomeActivity.reportViewModel.onGenerateReportClicked.postValue(true)
    Assert.assertNotNull(reportHomeActivity.reportViewModel.selectedMeasureReportItem.value!!.name)
    Assert.assertEquals(
      "All",
      reportHomeActivity.reportViewModel.selectedMeasureReportItem.value!!.reportType
    )
  }
}
