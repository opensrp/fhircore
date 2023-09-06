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

package org.smartregister.fhircore.quest.data.report.measure

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.today
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureReportRepositoryTest : RobolectricTest() {
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor
  private val fhirEngine: FhirEngine = mockk()

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  private lateinit var measureReportConfiguration: MeasureReportConfiguration
  private lateinit var measureReportRepository: MeasureReportRepository
  private val registerId = "register id"
  private lateinit var rulesFactory: RulesFactory
  private lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor
  private lateinit var registerRepository: RegisterRepository

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltAndroidRule.inject()
    rulesFactory =
      spyk(
        RulesFactory(
          context = ApplicationProvider.getApplicationContext(),
          configurationRegistry = configurationRegistry,
          fhirPathDataExtractor = fhirPathDataExtractor,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
        ),
      )
    resourceDataRulesExecutor = ResourceDataRulesExecutor(rulesFactory)

    val appId = "appId"
    val id = "id"
    val fhirResource = FhirResourceConfig(ResourceConfig(resource = ResourceType.Patient))

    measureReportConfiguration = MeasureReportConfiguration(appId, id = id, registerId = registerId)
    registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = DefaultDispatcherProvider(),
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          configRulesExecutor = mockk(),
          fhirPathDataExtractor = mockk(),
        ),
      )

    measureReportRepository =
      MeasureReportRepository(
        fhirEngine,
        DefaultDispatcherProvider(),
        mockk(),
        configurationRegistry,
        mockk(),
        mockk(),
        registerRepository,
        FhirOperator(FhirContext.forR4(), fhirEngine),
        mockk(),
      )
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testEvaluatePopulationMeasureHandlesBadMeasureUrl() {
    runBlocking(Dispatchers.Default) {
      val measureReport =
        measureReportRepository.evaluatePopulationMeasure(
          measureUrl = "bad-measure-url",
          startDateFormatted = today().firstDayOfMonth().formatDate(SDF_YYYY_MM_DD),
          endDateFormatted = today().lastDayOfMonth().formatDate(SDF_YYYY_MM_DD),
          subjects = emptyList(),
          existing = emptyList(),
          practitionerId = null,
        )
      assertEquals(measureReport.size, 0)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveSubjectsWithResultsEmptySubjectXFhir() {
    val reportConfiguration = ReportConfiguration()
    coEvery { fhirEngine.search<Patient>(any<Search>()) } returns listOf(Patient())

    runBlocking(Dispatchers.Default) {
      val data = measureReportRepository.fetchSubjects(reportConfiguration)
      assertEquals(0, data.size)
    }

    coVerify(inverse = true) { fhirEngine.search<Patient>(any<Search>()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveSubjectsWithResultsInvalidSubjectXFhir() {
    val reportConfiguration = ReportConfiguration(subjectXFhirQuery = "not-a-resource-type")
    coEvery { fhirEngine.search<Patient>(any<Search>()) } returns listOf(Patient())

    runBlocking(Dispatchers.Default) {
      val data = measureReportRepository.fetchSubjects(reportConfiguration)
      assertEquals(0, data.size)
    }

    coVerify(inverse = true) { fhirEngine.search<Patient>(any<Search>()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveSubjectsWithResultsNonEmptySubjectXFhir() {
    val reportConfiguration = ReportConfiguration(subjectXFhirQuery = "Patient")
    coEvery { fhirEngine.search<Patient>(any<Search>()) } returns listOf(Patient())

    runBlocking(Dispatchers.Default) {
      val data = measureReportRepository.fetchSubjects(reportConfiguration)
      assertEquals(1, data.size)
    }

    coVerify { fhirEngine.search<Patient>(any<Search>()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveSubjectsWithResultsNonEmptySubjectXFhirWithGroupUpdates() {
    val reportConfiguration = ReportConfiguration(subjectXFhirQuery = "Patient")
    coEvery { fhirEngine.search<Group>(any<Search>()) } returns listOf(Group())
    coEvery { fhirEngine.update(any<Group>()) } just runs

    runBlocking(Dispatchers.Default) {
      val data = measureReportRepository.fetchSubjects(reportConfiguration)
      assertEquals(1, data.size)
    }

    coVerify { fhirEngine.search<Patient>(any<Search>()) }
    coVerify { fhirEngine.update(any<Group>()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveSubjectsWithResultsNonEmptySubjectXFhirWithNonEmptyGroupDoesNotUpdate() {
    val reportConfiguration = ReportConfiguration(subjectXFhirQuery = "Patient")
    coEvery { fhirEngine.search<Group>(any<Search>()) } returns
      listOf(
        Group()
          .addMember(Group.GroupMemberComponent().setEntity(Reference().setReference("Patient/1"))),
      )
    coEvery { fhirEngine.update(any<Group>()) } just runs

    runBlocking(Dispatchers.Default) {
      val data = measureReportRepository.fetchSubjects(reportConfiguration)
      assertEquals(1, data.size)
    }

    coVerify { fhirEngine.search<Patient>(any<Search>()) }
    coVerify(inverse = true) { fhirEngine.update(any<Group>()) }
  }
}
