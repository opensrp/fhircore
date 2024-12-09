/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.integration.ui.report.measure.components

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.testing.TestNavHostController
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.integration.Faker
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.ReportTypeSelectorUiState
import org.smartregister.fhircore.quest.ui.report.measure.components.INDIVIDUAL_RESULT_VIEW_CONTAINER_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.components.POPULATION_RESULT_VIEW_CONTAINER_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult
import org.smartregister.fhircore.quest.ui.report.measure.screens.MeasureReportResultScreen
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportSubjectViewDataMapper

@HiltAndroidTest
class MeasureReportResultScreenTest {

  @get:Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 2) val composeTestRule = createComposeRule()

  private lateinit var measureReportViewModel: MeasureReportViewModel

  @Inject lateinit var measureReportSubjectViewDataMapper: MeasureReportSubjectViewDataMapper

  @BindValue val configurationRegistry = Faker.buildTestConfigurationRegistry()

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var fhirOperator: FhirOperator

  @Inject lateinit var registerRepository: RegisterRepository

  @BindValue val sharedPreferencesHelper = configurationRegistry.sharedPreferencesHelper

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var rulesExecutor: RulesExecutor

  @Inject lateinit var measureReportRepository: MeasureReportRepository

  @Before
  fun setup() {
    hiltRule.inject()
    measureReportViewModel =
      MeasureReportViewModel(
        fhirEngine,
        fhirOperator,
        sharedPreferencesHelper,
        DefaultDispatcherProvider(),
        configurationRegistry,
        registerRepository,
        measureReportSubjectViewDataMapper,
        defaultRepository,
        rulesExecutor,
        measureReportRepository,
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
        navController = TestNavHostController(LocalContext.current),
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
        navController = TestNavHostController(LocalContext.current),
        measureReportViewModel = measureReportViewModel,
      )
    }
    composeTestRule
      .onNodeWithTag(POPULATION_RESULT_VIEW_CONTAINER_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }
}
