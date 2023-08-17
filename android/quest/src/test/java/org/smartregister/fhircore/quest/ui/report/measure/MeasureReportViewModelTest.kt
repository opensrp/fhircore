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

package org.smartregister.fhircore.quest.ui.report.measure

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.util.Pair
import androidx.navigation.NavController
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineExceptionHandler
import kotlinx.coroutines.test.createTestCoroutineScope
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.SDF_MMMM
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportPagingSource
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportSubjectViewDataMapper

@HiltAndroidTest
class MeasureReportViewModelTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val configurationRegistry = Faker.buildTestConfigurationRegistry()

  @Inject lateinit var measureReportSubjectViewDataMapper: MeasureReportSubjectViewDataMapper

  @Inject lateinit var registerRepository: RegisterRepository

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor

  @Inject lateinit var measureReportRepository: MeasureReportRepository
  private val fhirEngine: FhirEngine = mockk()
  private val fhirOperator: FhirOperator = mockk()
  private val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)
  private val measureReportPagingSource = mockk<MeasureReportPagingSource>()
  private val navController: NavController = mockk(relaxUnitFun = true)
  private val invalidReportId = "invalidSupplyChainMeasureReport"
  private val reportId = "supplyChainMeasureReport"
  private val application: Context = ApplicationProvider.getApplicationContext()
  private lateinit var measureReportViewModel: MeasureReportViewModel
  private val testDispatcher = TestCoroutineDispatcher()
  private val testCoroutineScope =
    createTestCoroutineScope(
      TestCoroutineDispatcher() + TestCoroutineExceptionHandler() + testDispatcher,
    )

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { measureReportPagingSource.retrieveSubjects() } returns
      listOf(
        ResourceData(
          baseResourceId = Faker.buildPatient().id,
          baseResourceType = ResourceType.Patient,
          computedValuesMap = emptyMap(),
        ),
      )

    measureReportViewModel =
      spyk(
        MeasureReportViewModel(
          fhirEngine = fhirEngine,
          fhirOperator = fhirOperator,
          sharedPreferencesHelper = sharedPreferencesHelper,
          dispatcherProvider = mockk(),
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
    val reportConfiguration =
      ReportConfiguration(
        id = "measureId",
        title = "Measure 1",
        description = "Measure report for testing",
        url = "http://nourl.com",
        module = "Module1",
      )
    every { measureReportViewModel.evaluateMeasure(any(), any()) } just runs
    measureReportViewModel.reportTypeSelectorUiState.value =
      ReportTypeSelectorUiState(startDate = "21 Jan, 2022", endDate = "27 Jan, 2022")
    measureReportViewModel.onEvent(
      MeasureReportEvent.OnSelectMeasure(
        reportConfigurations = listOf(reportConfiguration),
        navController = navController,
        practitionerId = "practitioner-id",
      ),
    )

    // config updated for the view model
    val viewModelConfig = measureReportViewModel.reportConfigurations
    Assert.assertEquals(viewModelConfig.first().id, reportConfiguration.id)
    Assert.assertEquals(viewModelConfig.first().module, reportConfiguration.module)

    verify {
      measureReportViewModel.evaluateMeasure(
        navController = navController,
        practitionerId = "practitioner-id",
      )
    }
  }

  @Test
  fun testOnEventOnDateSelected() {
    val reportConfiguration =
      ReportConfiguration(
        id = "measureId",
        title = "Measure 1",
        description = "Measure report for testing",
        url = "http://nourl.com",
        module = "Module1",
      )
    val sampleSubjectViewData =
      mutableSetOf(
        MeasureReportSubjectViewData(
          type = ResourceType.Patient,
          logicalId = "member1",
          display = "Willy Mark, M, 28",
          family = "Orion",
        ),
      )

    measureReportViewModel.reportConfigurations.add(reportConfiguration)
    measureReportViewModel.reportTypeSelectorUiState.value =
      ReportTypeSelectorUiState("21 Jan, 2022", "21 Feb, 2022", false, sampleSubjectViewData)

    measureReportViewModel.onEvent(
      MeasureReportEvent.OnDateSelected(context = application, navController = navController),
      "2022-10-31".parseDate(SDF_YYYY_MM_DD),
    )

    val routeSlot = slot<String>()
    verify { navController.navigate(capture(routeSlot)) }
    Assert.assertEquals("reportMeasuresModule", routeSlot.captured)
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
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH),
      )
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()

  @Test
  fun testOnEventOnReportTypeChanged() {
    // Test with report type INDIVIDUAL
    measureReportViewModel.onEvent(
      MeasureReportEvent.OnReportTypeChanged(MeasureReportType.INDIVIDUAL, navController),
    )
    Assert.assertEquals(MeasureReportType.INDIVIDUAL, measureReportViewModel.reportTypeState.value)
    val routeSlot = slot<String>()
    verify { navController.navigate(capture(routeSlot)) }
    Assert.assertEquals(MeasureReportNavigationScreen.SubjectsList.route, routeSlot.captured)

    // Test with report type other than INDIVIDUAL
    measureReportViewModel.onEvent(
      MeasureReportEvent.OnReportTypeChanged(MeasureReportType.SUMMARY, navController),
    )
    Assert.assertEquals(
      0,
      measureReportViewModel.reportTypeSelectorUiState.value.subjectViewData.size,
    )
  }

  @Test
  fun testOnEventOnSubjectSelected() {
    val sampleSubjectViewData =
      MeasureReportSubjectViewData(
        type = ResourceType.Patient,
        logicalId = "member1",
        display = "Willy Mark, M, 28",
        family = "Orion",
      )
    measureReportViewModel.onEvent(MeasureReportEvent.OnSubjectSelected(sampleSubjectViewData))
    val subjectViewData =
      measureReportViewModel.reportTypeSelectorUiState.value.subjectViewData.firstOrNull()
    Assert.assertNotNull(sampleSubjectViewData.logicalId, subjectViewData?.logicalId)
    Assert.assertNotNull(sampleSubjectViewData.display, subjectViewData?.display)
    Assert.assertNotNull(sampleSubjectViewData.family, subjectViewData?.family)
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testFormatPopulationMeasureReport() = runTest {
    measureReport.type = MeasureReportType.SUMMARY
    val result = measureReportViewModel.formatPopulationMeasureReports(listOf(measureReport))

    assertEquals(1, result.size)
    assertEquals("report group 1", result.first().title)
    assertEquals(1, result.first().dataList.size)
  }

  private val measureReport =
    MeasureReport().apply {
      addGroup().apply {
        this.id = "report-group-1"
        this.addPopulation().apply {
          this.code.addCoding(
            MeasurePopulationType.NUMERATOR.let { Coding(it.system, it.toCode(), it.display) },
          )
          this.count = 3
        }

        this.addPopulation().apply {
          this.code.addCoding(
            MeasurePopulationType.DENOMINATOR.let { Coding(it.system, it.toCode(), it.display) },
          )
          this.count = 4
        }

        this.addStratifier().addStratum().apply {
          this.value = CodeableConcept().apply { text = "Stratum #1" }
          this.addPopulation().apply {
            this.code.addCoding(
              MeasurePopulationType.NUMERATOR.let { Coding(it.system, it.toCode(), it.display) },
            )
            this.count = 1
          }

          this.addPopulation().apply {
            this.code.addCoding(
              MeasurePopulationType.DENOMINATOR.let { Coding(it.system, it.toCode(), it.display) },
            )
            this.count = 2
          }
        }
      }
    }

  @Test
  fun testGetReportGenerationRange() {
    val result = measureReportViewModel.getReportGenerationRange("supplyChainMeasureReport")
    val currentMonth = Calendar.getInstance().time.formatDate(SDF_MMMM)
    val currentYear = Calendar.getInstance().time.formatDate(SDF_YYYY)
    assertEquals(currentYear, result.keys.first())
    assertEquals(currentMonth, result[result.keys.first()]?.get(0)?.month)
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testFormatMeasureReportsForSubject() = runTest {
    measureReport.type = MeasureReportType.SUMMARY
    measureReport.contained.clear()
    measureReport.contained.add(
      Observation().apply {
        code = CodeableConcept().apply { addCoding(Coding(null, "populationId", "Test Code")) }
        value = StringType("ABC")
      },
    )
    measureReport.contained.add(
      Observation().apply {
        code = CodeableConcept().apply { addCoding(Coding(null, "populationId", "Test Code")) }
        value = StringType("CDE")
      },
    )
    measureReport.contained.add(
      Observation().apply {
        code = CodeableConcept().apply { addCoding(Coding(null, "populationId", "Test Code")) }
        value = StringType("EFG")
      },
    )

    val result = measureReportViewModel.formatPopulationMeasureReports(listOf(measureReport))

    assertEquals(2, result.size)

    assertEquals("", result.first().count)
    assertEquals(0, result.first().dataList.size)
    assertEquals("Test Code", result.first().indicatorTitle)
    assertEquals("3", result.first().measureReportDenominator)

    assertEquals("", result.last().count)
    assertEquals(1, result.last().dataList.size)
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testFormatMeasureReportsStock() = runTest {
    measureReport.type = MeasureReportType.INDIVIDUAL
    measureReport.contained.clear()
    measureReport.contained.add(
      Observation().apply {
        code = CodeableConcept().apply { addCoding(Coding(null, "populationId", "Test Code 1")) }
        value = CodeableConcept().apply { addCoding().code = "2" }
      },
    )
    measureReport.contained.add(
      Observation().apply {
        code = CodeableConcept().apply { addCoding(Coding(null, "populationId", "Test Code 2")) }
        value = CodeableConcept().apply { addCoding().code = "4" }
      },
    )
    measureReport.contained.add(
      Observation().apply {
        code = CodeableConcept().apply { addCoding(Coding(null, "populationId", "Test Code 3")) }
        value = CodeableConcept().apply { addCoding().code = "6" }
      },
    )

    measureReport.subject = Reference().apply { reference = "Group/1" }

    coEvery { fhirEngine.get(ResourceType.Group, any()) } returns
      Group().apply { name = "Commodity 1" }

    val result = measureReportViewModel.formatPopulationMeasureReports(listOf(measureReport))

    assertEquals(1, result.size)

    assertEquals("", result.first().count)
    assertEquals(3, result.first().dataList.size)
    assertEquals("Commodity 1", result.first().indicatorTitle)
    assertEquals("0", result.first().measureReportDenominator)

    assertEquals("Test Code 1", result.first().dataList.elementAt(0).title)
    assertEquals("2", result.first().dataList.elementAt(0).count)

    assertEquals("Test Code 2", result.first().dataList.elementAt(1).title)
    assertEquals("4", result.first().dataList.elementAt(1).count)

    assertEquals("Test Code 3", result.first().dataList.elementAt(2).title)
    assertEquals("6", result.first().dataList.elementAt(2).count)
  }

  @Test
  fun reportMeasuresListThrowsExceptionIfReportIdNotFound() {
    assertFailsWith<java.util.NoSuchElementException> {
      measureReportViewModel.reportMeasuresList("")
    }
  }

  @Test
  fun reportMeasuresListThrowsExceptionIfSubjectRegisterMissing() {
    assertFailsWith<java.util.NoSuchElementException> {
      measureReportViewModel.reportMeasuresList(invalidReportId)
    }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun reportMeasuresListShouldReturnReportList() = runTest {
    assertNotNull(measureReportViewModel.reportMeasuresList(reportId).first())
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun testRetrieveSubjects() = runTest {
    val mockMeasureReportConfig =
      MeasureReportConfiguration(
        reportId,
        configType = ConfigType.MeasureReport.name,
        id = "1234",
        registerId = "test_register",
        reports =
          listOf(
            ReportConfiguration(
              id = "first id",
              title = "first report",
              description = "This my first report",
              url = "abc-@ona.io",
              module = "First module name",
            ),
          ),
      )
    val mockSubjectData: MutableStateFlow<Flow<PagingData<MeasureReportSubjectViewData>>> =
      MutableStateFlow(flow {})
    val pagedData: Flow<PagingData<MeasureReportSubjectViewData>> = flow {}

    every {
      configurationRegistry.retrieveConfiguration<MeasureReportConfiguration>(any(), any())
    } returns mockMeasureReportConfig
    val mockPagingSource: PagingSource<Int, MeasureReportSubjectViewData> = mockk()
    coEvery { mockPagingSource.load(any()) } returns
      PagingSource.LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

    val mockPager: Pager<Int, MeasureReportSubjectViewData> = mockk()
    every { (mockPager.flow) } returns (flow { emit(PagingData.empty()) })
    every { measureReportViewModel.subjectData } returns mockSubjectData

    measureReportViewModel.retrieveSubjects(reportId).collect {}
    every { measureReportViewModel.retrieveSubjects(any()) } returns pagedData

    val retrieveMeasureReport =
      MeasureReportViewModel::class
        .java
        .getDeclaredMethod("retrieveMeasureReportConfiguration", String::class.java)
    retrieveMeasureReport.isAccessible = true
    every { retrieveMeasureReport.invoke(null, any()) } returns mockMeasureReportConfig

    verify { retrieveMeasureReport.invoke("", reportId) }
    verify {
      configurationRegistry.retrieveConfiguration<RegisterConfiguration>(
        ConfigType.Register,
        mockMeasureReportConfig.registerId,
      )
    }

    verify { mockPager.flow }
    verify { mockSubjectData.value = mockPager.flow }

    testCoroutineScope.cleanupTestCoroutines()
  }
}
