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

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.util.Pair
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.test.assertEquals
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.SDF_MMMM
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportPatientViewDataMapper

@HiltAndroidTest
class MeasureReportViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutinesTestRule = CoroutineTestRule()

  private val application: Context = ApplicationProvider.getApplicationContext()

  var fhirEngine: FhirEngine = mockk()

  var fhirOperator: FhirOperator = mockk()

  @Inject lateinit var measureReportPatientViewDataMapper: MeasureReportPatientViewDataMapper

  @Inject lateinit var registerRepository: RegisterRepository

  @Inject lateinit var defaultRepository: DefaultRepository

  val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)

  val measureReportRepository = mockk<MeasureReportRepository>()

  private lateinit var measureReportViewModel: MeasureReportViewModel

  private val navController: NavController = mockk(relaxUnitFun = true)

  @BindValue val configurationRegistry = Faker.buildTestConfigurationRegistry()

  private val reportId = "defaultMeasureReport"

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { measureReportRepository.retrievePatients(0) } returns
      listOf(
        ResourceData(
          baseResourceId = Faker.buildPatient().id,
          baseResourceType = ResourceType.Patient,
          computedValuesMap = emptyMap(),
          listResourceDataMap = emptyMap(),
        )
      )

    measureReportViewModel =
      spyk(
        MeasureReportViewModel(
          fhirEngine = fhirEngine,
          fhirOperator = fhirOperator,
          sharedPreferencesHelper = sharedPreferencesHelper,
          dispatcherProvider = mockk(),
          measureReportPatientViewDataMapper = measureReportPatientViewDataMapper,
          configurationRegistry = configurationRegistry,
          registerRepository = registerRepository,
          defaultRepository = defaultRepository
        )
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
    Assert.assertEquals(MeasureReportType.SUMMARY, measureReportViewModel.reportTypeState.value)
    Assert.assertEquals(TextFieldValue(""), measureReportViewModel.searchTextState.value)
  }

  @Test
  fun testOnEventOnSelectMeasure() {
    val measureReportConfig =
      MeasureReportConfig(
        id = "measureId",
        title = "Measure 1",
        description = "Measure report for testing",
        url = "http://nourl.com",
        module = "Module1"
      )
    measureReportViewModel.onEvent(
      MeasureReportEvent.OnSelectMeasure(
        measureReportConfig = listOf(measureReportConfig),
        navController = navController
      )
    )
    val routeSlot = slot<String>()

    // config updated for the view model
    val viewModelConfig = measureReportViewModel.measureReportConfigList
    Assert.assertEquals(viewModelConfig.first().id, measureReportConfig.id)
    Assert.assertEquals(viewModelConfig.first().module, measureReportConfig.module)

    verify { navController.navigate(capture(routeSlot)) }

    Assert.assertEquals("reportTypeSelector?screenTitle=Module1", routeSlot.captured)
  }

  @Test
  fun testOnEventOnSelectGenerateReport() {
    val measureReportConfig =
      MeasureReportConfig(
        id = "measureId",
        title = "Measure 1",
        description = "Measure report for testing",
        url = "http://nourl.com",
        module = "Module1"
      )
    val dateRange =
      Pair(dateTimestamp("2020-01-01T14:34:18.000Z"), dateTimestamp("2020-12-31T14:34:18.000Z"))
    val samplePatientViewData =
      MeasureReportPatientViewData(
        logicalId = "member1",
        name = "Willy Mark",
        gender = "M",
        age = "28",
        family = "Orion"
      )

    measureReportViewModel.measureReportConfigList.add(measureReportConfig)
    measureReportViewModel.reportTypeSelectorUiState.value =
      ReportTypeSelectorUiState("21 Jan, 2022", "21 Feb, 2022", false, samplePatientViewData)

    measureReportViewModel.onEvent(
      MeasureReportEvent.GenerateReport(context = application, navController = navController),
      "2022-10-31".parseDate(SDF_YYYY_MM_DD)
    )

    verify { measureReportViewModel.evaluateMeasure(navController) }
  }

  @Test
  fun testOnEventOnDateRangeSelected() {
    val newDateRange =
      Pair(dateTimestamp("2020-01-01T14:34:18.000Z"), dateTimestamp("2020-12-31T14:34:18.000Z"))

    measureReportViewModel.onEvent(MeasureReportEvent.OnDateRangeSelected(newDateRange))
    Assert.assertEquals(measureReportViewModel.dateRange.value, newDateRange)
    val reportTypeSelectorUiState = measureReportViewModel.reportTypeSelectorUiState.value

    Assert.assertEquals("1 Jan, 2020", reportTypeSelectorUiState.startDate)
    Assert.assertEquals("31 Dec, 2020", reportTypeSelectorUiState.endDate)
  }

  private fun dateTimestamp(startTimeString: String) =
    LocalDateTime.parse(
        startTimeString,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
      )
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()

  @Test
  fun testOnEventOnReportTypeChanged() {
    // Test with report type INDIVIDUAL
    measureReportViewModel.onEvent(
      MeasureReportEvent.OnReportTypeChanged(MeasureReportType.INDIVIDUAL, navController)
    )
    Assert.assertEquals(MeasureReportType.INDIVIDUAL, measureReportViewModel.reportTypeState.value)
    val routeSlot = slot<String>()
    verify { navController.navigate(capture(routeSlot)) }
    Assert.assertEquals(MeasureReportNavigationScreen.PatientsList.route, routeSlot.captured)

    // Test with report type other than INDIVIDUAL
    measureReportViewModel.onEvent(
      MeasureReportEvent.OnReportTypeChanged(MeasureReportType.SUMMARY, navController)
    )
    Assert.assertNull(measureReportViewModel.reportTypeSelectorUiState.value.patientViewData)
  }

  @Test
  fun testOnEventOnPatientSelected() {
    val samplePatientViewData =
      MeasureReportPatientViewData(
        logicalId = "member1",
        name = "Willy Mark",
        gender = "M",
        age = "28",
        family = "Orion"
      )
    measureReportViewModel.onEvent(MeasureReportEvent.OnPatientSelected(samplePatientViewData))
    val patientViewData = measureReportViewModel.reportTypeSelectorUiState.value.patientViewData
    Assert.assertNotNull(samplePatientViewData.logicalId, patientViewData?.logicalId)
    Assert.assertNotNull(samplePatientViewData.name, patientViewData?.name)
    Assert.assertNotNull(samplePatientViewData.gender, patientViewData?.gender)
    Assert.assertNotNull(samplePatientViewData.age, patientViewData?.age)
    Assert.assertNotNull(samplePatientViewData.family, patientViewData?.family)
  }

  @Test
  fun testOnEventOnSearchTextChanged() {
    measureReportViewModel.onEvent(
      MeasureReportEvent.OnSearchTextChanged(reportId = reportId, searchText = "Mandela")
    )
    Assert.assertNotNull(measureReportViewModel.patientsData.value)
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

  @Test
  fun testGetReportGenerationRange() {
    val result = measureReportViewModel.getReportGenerationRange("defaultMeasureReport")
    val currentMonth = Calendar.getInstance().time.formatDate(SDF_MMMM)
    val currentYear = Calendar.getInstance().time.formatDate(SDF_YYYY)
    assertEquals(currentYear, result.keys.first())
    assertEquals(currentMonth, result[result.keys.first()]?.get(0)?.month)
  }
}
