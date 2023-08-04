/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.report.measure.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavController
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import io.mockk.mockk
import io.mockk.spyk
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.Faker
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.ReportTypeSelectorUiState
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult
import org.smartregister.fhircore.quest.ui.report.measure.screens.MeasureReportResultScreen
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportSubjectViewDataMapper

class MeasureReportResultScreenTest {

  @get:Rule(order = 0) val composeTestRule = createComposeRule()
  private lateinit var measureReportViewModel: MeasureReportViewModel
  private val fhirEngine: FhirEngine = mockk()
  private val fhirOperator: FhirOperator = mockk()
  private val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)
  private val dispatcherProvider: DefaultDispatcherProvider = mockk()
  private var measureReportSubjectViewDataMapper: MeasureReportSubjectViewDataMapper =
    mockk(relaxed = true)
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private var registerRepository: RegisterRepository = mockk(relaxed = true)
  private var defaultRepository: DefaultRepository = mockk(relaxed = true)
  private var resourceDataRulesExecutor: ResourceDataRulesExecutor = mockk(relaxed = true)
  private var measureReportRepository: MeasureReportRepository = mockk(relaxed = true)
  private val navController: NavController = mockk(relaxUnitFun = true)

  @Before
  fun setup() {
    measureReportViewModel =
      spyk(
        MeasureReportViewModel(
          fhirEngine = fhirEngine,
          fhirOperator = fhirOperator,
          sharedPreferencesHelper = sharedPreferencesHelper,
          dispatcherProvider = dispatcherProvider,
          measureReportSubjectViewDataMapper = measureReportSubjectViewDataMapper,
          configurationRegistry = configurationRegistry,
          registerRepository = registerRepository,
          defaultRepository = defaultRepository,
          resourceDataRulesExecutor = resourceDataRulesExecutor,
          measureReportRepository = measureReportRepository,
        ),
      )
  }

  @Test
  fun testMeasureReportResultScreenHasMeasureReportIndividualResultView() {
    val sampleSubjectViewData =
      mutableSetOf(
        MeasureReportSubjectViewData(
          type = ResourceType.Patient,
          logicalId = "member1",
          display = "Wonka Mark, M, 28",
          family = "Orion",
        ),
      )
    measureReportViewModel.reportTypeSelectorUiState.value =
      ReportTypeSelectorUiState(subjectViewData = sampleSubjectViewData)
    measureReportViewModel.measureReportIndividualResult.value =
      MeasureReportIndividualResult(isMatchedIndicator = true)
    composeTestRule.setContent {
      MeasureReportResultScreen(
        navController = navController,
        measureReportViewModel = measureReportViewModel,
      )
    }
    composeTestRule
      .onNodeWithTag(INDIVIDUAL_RESULT_VIEW_CONTAINER_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testMeasureReportResultScreenHasMeasurePopulationResultView() {
    val populationResult = MeasureReportPopulationResult()
    measureReportViewModel.measureReportPopulationResults.value = listOf(populationResult)
    composeTestRule.setContent {
      MeasureReportResultScreen(
        navController = navController,
        measureReportViewModel = measureReportViewModel,
      )
    }
    composeTestRule
      .onNodeWithTag(POPULATION_RESULT_VIEW_CONTAINER_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }
}
