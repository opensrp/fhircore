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
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.app.fakes.FakeModel.getUserInfo
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.data.report.model.ResultItemPopulation
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper.Companion.MEASURE_RESOURCES_LOADED
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.encodeJson

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class ReportViewModelEmptySelectedPatientTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 2) val coroutinesTestRule = CoroutineTestRule()

  @get:Rule(order = 3) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)

  private lateinit var fhirEngine: FhirEngine
  private lateinit var reportRepository: ReportRepository
  private lateinit var ancPatientRepository: PatientRepository
  private lateinit var reportViewModel: ReportViewModel
  private val resultForIndividual =
    MutableLiveData(ResultItem(status = "True", isMatchedIndicator = true))
  private val resultForPopulation =
    MutableLiveData(listOf(ResultItemPopulation(title = "resultForPopulation")))
  private val fhirOperatorDecorator = mockk<FhirOperatorDecorator>()

  @Before
  fun setUp() {
    hiltRule.inject()

    every { sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, "") } returns
      getUserInfo().encodeJson()
    every { sharedPreferencesHelper.read(MEASURE_RESOURCES_LOADED, "") } returns ""
    every { sharedPreferencesHelper.write(MEASURE_RESOURCES_LOADED, "") } returns Unit

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
          fhirOperatorDecorator = fhirOperatorDecorator,
          sharedPreferencesHelper = sharedPreferencesHelper
        )
      )
    every { reportViewModel.startDate } returns MutableLiveData("25 Nov, 2021")
    every { reportViewModel.endDate } returns MutableLiveData("10 Dec, 2021")
    every { reportViewModel.resultForIndividual } returns
      this@ReportViewModelEmptySelectedPatientTest.resultForIndividual
    every { reportViewModel.resultForPopulation } returns
      this@ReportViewModelEmptySelectedPatientTest.resultForPopulation
  }

  @After
  fun tearDown() {
    reportViewModel
  }

  @Test
  fun testEvaluateMeasureForPopulation() {
    coEvery { fhirOperatorDecorator.loadLib(any()) } just runs
    coEvery { fhirEngine.save(any()) } returns Unit

    reportViewModel.evaluateMeasure(
      context = ApplicationProvider.getApplicationContext(),
      measureUrl = "measure/ancInd03",
      individualEvaluation = false,
      measureResourceBundleUrl = "measure/ancInd03"
    )
  }
}
