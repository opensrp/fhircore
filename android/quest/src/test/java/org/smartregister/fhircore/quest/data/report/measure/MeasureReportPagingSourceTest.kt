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

package org.smartregister.fhircore.quest.data.report.measure

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.data.local.ContentCache
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.rulesengine.services.LocationService
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureReportPagingSourceTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var parser: IParser

  @Inject lateinit var locationService: LocationService

  @Inject lateinit var contentCache: ContentCache

  @Inject lateinit var fhirContext: FhirContext

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private val fhirEngine: FhirEngine = mockk()
  private val registerId = "register id"
  private lateinit var rulesFactory: RulesFactory
  private lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor
  private lateinit var measureReportConfiguration: MeasureReportConfiguration
  private lateinit var measureReportPagingSource: MeasureReportPagingSource
  private lateinit var registerRepository: RegisterRepository
  private lateinit var defaultRepository: DefaultRepository

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltAndroidRule.inject()
    defaultRepository = mockk(relaxed = true)
    rulesFactory =
      spyk(
        RulesFactory(
          context = ApplicationProvider.getApplicationContext(),
          configurationRegistry = configurationRegistry,
          fhirPathDataExtractor = fhirPathDataExtractor,
          dispatcherProvider = dispatcherProvider,
          locationService = locationService,
          fhirContext = fhirContext,
          defaultRepository = defaultRepository,
        ),
      )
    resourceDataRulesExecutor = ResourceDataRulesExecutor(rulesFactory)

    val appId = "appId"
    val id = "id"
    val fhirResource = FhirResourceConfig(ResourceConfig(resource = ResourceType.Patient))

    measureReportConfiguration = MeasureReportConfiguration(appId, id = id, registerId = registerId)
    val registerConfiguration = RegisterConfiguration(appId, id = id, fhirResource = fhirResource)
    registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          configRulesExecutor = mockk(),
          fhirPathDataExtractor = fhirPathDataExtractor,
          parser = parser,
          context = ApplicationProvider.getApplicationContext(),
          contentCache = contentCache,
        ),
      )

    measureReportPagingSource =
      MeasureReportPagingSource(
        measureReportConfiguration,
        registerConfiguration,
        registerRepository,
        resourceDataRulesExecutor,
      )
  }

  @Test
  fun testGetRefreshKey() {
    val reportConfiguration = ReportConfiguration()
    val page =
      PagingSource.LoadResult.Page<Int, ReportConfiguration>(
        listOf(reportConfiguration),
        null,
        null,
      )
    val pages = listOf(page)
    val pagingConfig = PagingConfig(1)
    val state = PagingState(pages, null, pagingConfig, 0)
    val refreshKey = measureReportPagingSource.getRefreshKey(state)
    Assert.assertNull(refreshKey)
  }

  @Test
  @kotlinx.serialization.ExperimentalSerializationApi
  fun testLoad() {
    val params = PagingSource.LoadParams.Refresh<Int>(null, 1, false)
    runBlocking(Dispatchers.Default) {
      val result = measureReportPagingSource.load(params)
      Assert.assertNotNull(result)
    }
  }

  @Test
  @kotlinx.serialization.ExperimentalSerializationApi
  fun testRetrieveSubjectsWithResults() {
    coEvery { fhirEngine.search<Patient>(any()) } returns
      listOf(SearchResult(resource = Patient(), null, null))
    runBlocking(Dispatchers.Default) {
      val data = measureReportPagingSource.retrieveSubjects()
      Assert.assertEquals(1, data.size)
    }
  }
}
