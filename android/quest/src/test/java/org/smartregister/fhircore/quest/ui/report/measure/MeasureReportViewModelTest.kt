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

package org.smartregister.fhircore.quest.ui.report.measure

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import javax.inject.Inject
import kotlin.test.assertEquals
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.MeasureReport
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.util.mappers.MeasureReportPatientViewDataMapper

@HiltAndroidTest
class MeasureReportViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createComposeRule()
  @get:Rule(order = 2) val coroutinesTestRule = CoroutineTestRule()

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var fhirOperator: FhirOperator

  @Inject lateinit var measureReportPatientViewDataMapper: MeasureReportPatientViewDataMapper

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)

  @Inject lateinit var measureReportRepository: MeasureReportRepository

  private lateinit var measureReportViewModel: MeasureReportViewModel

  @Before
  fun setUp() {
    hiltRule.inject()

    measureReportViewModel =
      MeasureReportViewModel(
        fhirEngine = fhirEngine,
        fhirOperator = fhirOperator,
        sharedPreferencesHelper = sharedPreferencesHelper,
        dispatcherProvider = mockk(),
        measureReportRepository = measureReportRepository,
        measureReportPatientViewDataMapper = measureReportPatientViewDataMapper
      )
  }

  @Test
  fun testDefaultDateRangeState() {
    Assert.assertNotNull(measureReportViewModel.defaultDateRangeState())
    Assert.assertNotNull(measureReportViewModel.defaultDateRangeState().first)
    Assert.assertNotNull(measureReportViewModel.defaultDateRangeState().second)
  }

  @Test
  fun testToggleProgressIndicatorVisibility() {
    measureReportViewModel.toggleProgressIndicatorVisibility(showProgressIndicator = false)
    Assert.assertFalse(measureReportViewModel.reportTypeSelectorUiState.value.showProgressIndicator)
  }

  @Test
  fun testResetState() {
    measureReportViewModel.resetState()
    Assert.assertFalse(measureReportViewModel.reportTypeSelectorUiState.value.showProgressIndicator)
    Assert.assertNotNull(measureReportViewModel.dateRange)
    Assert.assertEquals(
      MeasureReport.MeasureReportType.SUMMARY,
      measureReportViewModel.reportTypeState.value
    )
    Assert.assertEquals(TextFieldValue(""), measureReportViewModel.searchTextState.value)
  }

  @Test
  fun testFormatPopulationMeasureReport() {
    val result = measureReportViewModel.formatPopulationMeasureReport(measureReport)

    assertEquals(1, result.size)
    assertEquals("3/4", result.first().count)
    assertEquals("report group 1", result.first().title)

    val disaggregation = result.first().dataList
    assertEquals(1, result.first().dataList.size)
    assertEquals("1/3", disaggregation.first().count)
    assertEquals("Stratum #1", disaggregation.first().title)
    assertEquals("33", disaggregation.first().percentage)
  }

  private val measureReport =
    MeasureReport().apply {
      addGroup().apply {
        this.id = "report-group-1"
        this.addPopulation().apply {
          this.code.addCoding(
            MeasurePopulationType.NUMERATOR.let { Coding(it.system, it.toCode(), it.display) }
          )
          this.count = 3
        }

        this.addPopulation().apply {
          this.code.addCoding(
            MeasurePopulationType.DENOMINATOR.let { Coding(it.system, it.toCode(), it.display) }
          )
          this.count = 4
        }

        this.addStratifier().addStratum().apply {
          this.value = CodeableConcept().apply { text = "Stratum #1" }
          this.addPopulation().apply {
            this.code.addCoding(
              MeasurePopulationType.NUMERATOR.let { Coding(it.system, it.toCode(), it.display) }
            )
            this.count = 1
          }

          this.addPopulation().apply {
            this.code.addCoding(
              MeasurePopulationType.DENOMINATOR.let { Coding(it.system, it.toCode(), it.display) }
            )
            this.count = 2
          }
        }
      }
    }
}
