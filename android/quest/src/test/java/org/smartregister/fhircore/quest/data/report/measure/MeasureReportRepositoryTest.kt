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

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import java.util.NoSuchElementException
import javax.inject.Inject
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureReportRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val coroutineRule = CoroutineTestRule()
  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private val fhirEngine: FhirEngine = mockk()
  private val registerId = "register id"
  private lateinit var rulesFactory: RulesFactory
  private lateinit var rulesExecutor: RulesExecutor
  private lateinit var measureReportConfiguration: MeasureReportConfiguration
  private lateinit var measureReportRepository: MeasureReportRepository

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    rulesFactory =
      spyk(
        RulesFactory(
          context = ApplicationProvider.getApplicationContext(),
          configurationRegistry = configurationRegistry,
          fhirPathDataExtractor = fhirPathDataExtractor,
          dispatcherProvider = coroutineRule.testDispatcherProvider
        )
      )
    rulesExecutor = RulesExecutor(rulesFactory)

    val appId = "appId"
    val id = "id"
    val fhirResource = FhirResourceConfig(ResourceConfig(resource = "Patient"))

    measureReportConfiguration = MeasureReportConfiguration(appId, id = id, registerId = registerId)
    val registerConfiguration = RegisterConfiguration(appId, id = id, fhirResource = fhirResource)
    val registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = DefaultDispatcherProvider(),
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          fhirPathDataExtractor = fhirPathDataExtractor
        )
      )

    measureReportRepository =
      MeasureReportRepository(
        measureReportConfiguration,
        registerConfiguration,
        registerRepository,
        rulesExecutor
      )
  }

  @Test
  fun testGetRefreshKey() {
    val measureReportConfig = MeasureReportConfig()
    val page =
      PagingSource.LoadResult.Page<Int, MeasureReportConfig>(
        listOf(measureReportConfig),
        null,
        null
      )
    val pages = listOf(page)
    val pagingConfig = PagingConfig(1)
    val state = PagingState(pages, null, pagingConfig, 0)
    val refreshKey = measureReportRepository.getRefreshKey(state)
    Assert.assertNull(refreshKey)
  }

  @Test
  fun testLoad() {
    val params = PagingSource.LoadParams.Refresh<Int>(null, 1, false)
    runBlocking(Dispatchers.Default) {
      val result = measureReportRepository.load(params)
      Assert.assertNotNull(result)
    }
  }

  @Test
  fun testRetrievePatients() {
    runBlocking(Dispatchers.Default) {
      assertFailsWith<NoSuchElementException> {
        val data = measureReportRepository.retrievePatients(0)
      }
    }
  }
}
