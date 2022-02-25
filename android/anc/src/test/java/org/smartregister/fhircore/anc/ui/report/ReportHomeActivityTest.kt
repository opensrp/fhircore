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
import android.os.Looper
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class ReportHomeActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val coroutinesTestRule = CoroutineTestRule()
  @Inject lateinit var reportRepository: ReportRepository
  @Inject lateinit var patientRepository: PatientRepository
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @BindValue lateinit var reportViewModel: ReportViewModel
  private lateinit var reportHomeActivity: ReportHomeActivity
  private lateinit var reportHomeActivitySpy: ReportHomeActivity
  private val fhirEngine: FhirEngine = spyk()
  private val fhirOperator: FhirOperator = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()
    reportViewModel =
      spyk(
        ReportViewModel(
          repository = reportRepository,
          dispatcher = coroutinesTestRule.testDispatcherProvider,
          patientRepository = patientRepository,
          fhirEngine = fhirEngine,
          fhirOperator = fhirOperator,
          sharedPreferencesHelper = sharedPreferencesHelper
        )
      )
    reportHomeActivity = Robolectric.buildActivity(ReportHomeActivity::class.java).create().get()
    reportHomeActivitySpy = spyk(objToCopy = reportHomeActivity)
    every { reportHomeActivitySpy.reportViewModel } returns reportViewModel
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
}
