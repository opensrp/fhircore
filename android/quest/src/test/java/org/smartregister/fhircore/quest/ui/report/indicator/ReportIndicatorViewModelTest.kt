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

package org.smartregister.fhircore.quest.ui.report.indicator

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.indicator.ReportIndicator
import org.smartregister.fhircore.engine.configuration.report.indicator.ReportIndicatorConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class ReportIndicatorViewModelTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var defaultRepository: DefaultRepository

  private lateinit var reportIndicatorViewModel: ReportIndicatorViewModel
  private val testReportId = "test-report-id"
  private val testStartDate = "2024-01-01"
  private val testEndDate = "2024-01-31"
  private val testPeriodLabel = "January 2024 Reports"

  @Before
  fun setUp() {
    hiltRule.inject()
    reportIndicatorViewModel = ReportIndicatorViewModel(defaultRepository)
  }

  @Test
  fun testInitialState() {
    assertTrue(reportIndicatorViewModel.reportIndicatorsMap.isEmpty())
    assertTrue(reportIndicatorViewModel.reportPeriodRange.value.isEmpty())
    assertFalse(reportIndicatorViewModel.isLoading.value)
    assertNull(reportIndicatorViewModel.selectedDateRange.value)
    assertNull(reportIndicatorViewModel.selectedPeriodLabel.value)
  }

  @Test
  fun testRetrieveIndicatorsWithZeroCount() = runTest {
    val testIndicator =
      ReportIndicator(
        id = "indicator-1",
        title = "Test Indicator",
        resourceConfig =
          ResourceConfig(
            resource = ResourceType.Encounter,
          ),
      )

    val testConfig =
      ReportIndicatorConfiguration(
        appId = "test-app",
        configType = "reportIndicator",
        id = testReportId,
        indicators = listOf(testIndicator),
      )

    val mockConfigRegistry = mockk<ConfigurationRegistry>(relaxed = true)
    coEvery {
      mockConfigRegistry.retrieveConfiguration<ReportIndicatorConfiguration>(
        ConfigType.ReportIndicator,
        testReportId,
      )
    } returns testConfig

    val mockDefaultRepo = mockk<DefaultRepository>(relaxed = true)
    coEvery { mockDefaultRepo.configurationRegistry } returns mockConfigRegistry
    coEvery { mockDefaultRepo.configRulesExecutor.computeConfigRules(any(), any()) } returns
      emptyMap()
    coEvery { mockDefaultRepo.count(any()) } returns 0L

    val viewModel = ReportIndicatorViewModel(mockDefaultRepo)

    viewModel.retrieveIndicators(
      testReportId,
      testStartDate,
      testEndDate,
      testPeriodLabel,
    )

    assertTrue(viewModel.reportIndicatorsMap.isEmpty())
  }

  @Test
  fun testRetrieveIndicatorsHandlesException() = runTest {
    val mockConfigRegistry = mockk<ConfigurationRegistry>(relaxed = true)
    coEvery {
      mockConfigRegistry.retrieveConfiguration<ReportIndicatorConfiguration>(
        ConfigType.ReportIndicator,
        testReportId,
      )
    } throws Exception("Test exception")

    val mockDefaultRepo = mockk<DefaultRepository>(relaxed = true)
    coEvery { mockDefaultRepo.configurationRegistry } returns mockConfigRegistry

    val viewModel = ReportIndicatorViewModel(mockDefaultRepo)

    viewModel.retrieveIndicators(
      testReportId,
      testStartDate,
      testEndDate,
      testPeriodLabel,
    )

    assertFalse(viewModel.isLoading.value)
    assertTrue(viewModel.reportIndicatorsMap.isEmpty())
  }

  @Test
  fun testLoadDateRangeFromEncountersSuccessfully() = runTest {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.JANUARY, 1)
    val startDate = calendar.time

    calendar.set(2024, Calendar.MARCH, 31)
    val endDate = calendar.time

    val minEncounter = Encounter().apply { period = Period().apply { start = startDate } }

    val maxEncounter = Encounter().apply { period = Period().apply { end = endDate } }

    val mockFhirEngine = mockk<FhirEngine>(relaxed = true)
    coEvery { mockFhirEngine.search<Encounter>(any()) } returnsMany
      listOf(
        listOf(SearchResult(minEncounter, null, null)),
        listOf(SearchResult(maxEncounter, null, null)),
      )

    val mockDefaultRepo = mockk<DefaultRepository>(relaxed = true)
    coEvery { mockDefaultRepo.fhirEngine } returns mockFhirEngine

    val viewModel = ReportIndicatorViewModel(mockDefaultRepo)

    viewModel.loadDateRangeFromEncounters()

    kotlinx.coroutines.delay(200)

    assertFalse(viewModel.reportPeriodRange.value.isEmpty())
    assertTrue(viewModel.reportPeriodRange.value.containsKey("2024"))
  }

  @Test
  fun testLoadDateRangeFromEncountersHandlesException() = runTest {
    val mockFhirEngine = mockk<FhirEngine>(relaxed = true)
    coEvery { mockFhirEngine.search<Encounter>(any()) } throws Exception("Test exception")

    val mockDefaultRepo = mockk<DefaultRepository>(relaxed = true)
    coEvery { mockDefaultRepo.fhirEngine } returns mockFhirEngine

    val viewModel = ReportIndicatorViewModel(mockDefaultRepo)

    viewModel.loadDateRangeFromEncounters()

    kotlinx.coroutines.delay(200)

    assertTrue(viewModel.reportPeriodRange.value.isEmpty())
  }

  @Test
  fun testGenerateMonthlyRangesWithValidDates() = runTest {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.JANUARY, 1)
    val startDate = calendar.time

    calendar.set(2024, Calendar.MARCH, 31)
    val endDate = calendar.time

    val minEncounter = Encounter().apply { period = Period().apply { start = startDate } }

    val maxEncounter = Encounter().apply { period = Period().apply { end = endDate } }

    val mockFhirEngine = mockk<FhirEngine>(relaxed = true)
    coEvery { mockFhirEngine.search<Encounter>(any()) } returnsMany
      listOf(
        listOf(SearchResult(minEncounter, null, null)),
        listOf(SearchResult(maxEncounter, null, null)),
      )

    val mockDefaultRepo = mockk<DefaultRepository>(relaxed = true)
    coEvery { mockDefaultRepo.fhirEngine } returns mockFhirEngine

    val viewModel = ReportIndicatorViewModel(mockDefaultRepo)

    viewModel.loadDateRangeFromEncounters()

    kotlinx.coroutines.delay(200)

    val ranges = viewModel.reportPeriodRange.value
    assertNotNull(ranges)
    assertTrue(ranges.containsKey("2024"))

    val months = ranges["2024"]
    assertNotNull(months)
    assertTrue(months!!.size >= 3)
  }

  @Test
  fun testGenerateMonthlyRangesInDescendingOrder() = runTest {
    val calendar = Calendar.getInstance()
    calendar.set(2023, Calendar.JANUARY, 1)
    val startDate = calendar.time

    calendar.set(2024, Calendar.DECEMBER, 31)
    val endDate = calendar.time

    val minEncounter = Encounter().apply { period = Period().apply { start = startDate } }

    val maxEncounter = Encounter().apply { period = Period().apply { end = endDate } }

    val mockFhirEngine = mockk<FhirEngine>(relaxed = true)
    coEvery { mockFhirEngine.search<Encounter>(any()) } returnsMany
      listOf(
        listOf(SearchResult(minEncounter, null, null)),
        listOf(SearchResult(maxEncounter, null, null)),
      )

    val mockDefaultRepo = mockk<DefaultRepository>(relaxed = true)
    coEvery { mockDefaultRepo.fhirEngine } returns mockFhirEngine

    val viewModel = ReportIndicatorViewModel(mockDefaultRepo)

    viewModel.loadDateRangeFromEncounters()

    kotlinx.coroutines.delay(200)

    val ranges = viewModel.reportPeriodRange.value
    val years = ranges.keys.toList()

    assertTrue(years.size >= 2)
    assertEquals("2025", years[0])
    assertEquals("2024", years[1])
    assertEquals("2023", years[2])
  }

  @Test
  fun testRetrieveIndicatorsClearsMapBeforeFetching() = runTest {
    val mockConfigRegistry = mockk<ConfigurationRegistry>(relaxed = true)
    val testConfig =
      ReportIndicatorConfiguration(
        appId = "test-app",
        configType = "reportIndicator",
        id = testReportId,
        indicators = emptyList(),
      )

    coEvery {
      mockConfigRegistry.retrieveConfiguration<ReportIndicatorConfiguration>(
        ConfigType.ReportIndicator,
        testReportId,
      )
    } returns testConfig

    val mockDefaultRepo = mockk<DefaultRepository>(relaxed = true)
    coEvery { mockDefaultRepo.configurationRegistry } returns mockConfigRegistry
    coEvery { mockDefaultRepo.configRulesExecutor.computeConfigRules(any(), any()) } returns
      emptyMap()

    val viewModel = ReportIndicatorViewModel(mockDefaultRepo)

    viewModel.reportIndicatorsMap["old-indicator"] =
      Pair(
        ReportIndicator(
          id = "old-indicator",
          title = "Old",
          resourceConfig = ResourceConfig(resource = ResourceType.Patient)
        ),
        10L,
      )

    viewModel.retrieveIndicators(
      testReportId,
      testStartDate,
      testEndDate,
      testPeriodLabel,
    )

    assertTrue(viewModel.reportIndicatorsMap.isEmpty())
  }
}
