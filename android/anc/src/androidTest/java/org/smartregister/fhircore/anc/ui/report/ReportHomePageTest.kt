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

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.ui.TestDispatcherProvider
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class ReportHomePageTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private val app = ApplicationProvider.getApplicationContext<Application>()
  private lateinit var repository: ReportRepository
  private lateinit var ancPatientRepository: PatientRepository
  private lateinit var viewModel: ReportViewModel
  private val fhirEngine: FhirEngine = spyk()
  private val fhirOperator = mockk<FhirOperator>()

  @Before
  fun setUp() {
    hiltRule.inject()
    repository = spyk(ReportRepository(fhirEngine, ApplicationProvider.getApplicationContext()))

    ancPatientRepository =
      spyk(
        PatientRepository(
          context = app,
          fhirEngine = fhirEngine,
          domainMapper = AncItemMapper(app),
          dispatcherProvider = TestDispatcherProvider.instance
        )
      )
    viewModel =
      spyk(
        objToCopy =
          ReportViewModel(
            repository = repository,
            dispatcher = TestDispatcherProvider.instance,
            patientRepository = ancPatientRepository,
            fhirOperator = fhirOperator,
            fhirEngine = fhirEngine,
            sharedPreferencesHelper = sharedPreferencesHelper
          )
      )
  }

  @Test
  fun testReportHomeScreenComponents() {
    composeRule.setContent { ReportHomeScreen(viewModel = viewModel) }
    // toolbar should have valid title and icon
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertTextEquals(app.getString(R.string.reports))
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
  }
}
