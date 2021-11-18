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

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
class ReportFilterPageTest : RobolectricTest() {

  private lateinit var fhirEngine: FhirEngine
  private lateinit var repository: ReportRepository
  private lateinit var viewModel: ReportViewModel
  @get:Rule val composeRule = createComposeRule()
  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @Before
  fun setUp() {
    fhirEngine = spyk()
    repository =
      spyk(
        ReportRepository(fhirEngine, "testPatientID", ApplicationProvider.getApplicationContext())
      )
    viewModel =
      spyk(objToCopy = ReportViewModel(repository, coroutinesTestRule.testDispatcherProvider))
    every { viewModel.getSelectedReport() } returns ReportItem(title = "Test Report Title")
    every { viewModel.getPatientSelectionType() } returns ReportViewModel.PatientSelectionType.ALL
    composeRule.setContent { ReportFilterScreen(viewModel = viewModel) }
  }

  @Test
  fun testReportHomeScreenComponents() {
    // toolbar should have valid title and icon
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertTextEquals("Test Report Title")
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
  }
}
