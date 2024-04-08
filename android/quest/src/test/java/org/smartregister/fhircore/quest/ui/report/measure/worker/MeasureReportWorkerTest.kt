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

package org.smartregister.fhircore.quest.ui.report.measure.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Measure
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureReportWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val context: Application = ApplicationProvider.getApplicationContext()
  private var defaultRepository: DefaultRepository = mockk(relaxed = true)
  private val configurationRegistry: ConfigurationRegistry = mockk(relaxed = true)

  @Inject lateinit var dispatcherProvider: DispatcherProvider
  private val fhirEngine: FhirEngine = mockk(relaxed = true)
  private val fhirOperator: FhirOperator = mockk(relaxed = true)
  private lateinit var measureReportWorker: MeasureReportWorker

  @Inject lateinit var parser: IParser

  @Before
  fun setUp() {
    hiltRule.inject()
    measureReportWorker =
      TestListenableWorkerBuilder<MeasureReportWorker>(
          context,
        )
        .setWorkerFactory(MeasureReportWorkerFactory())
        .build()
    defaultRepository =
      spyk(
        DefaultRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          configRulesExecutor = mockk(),
          fhirPathDataExtractor = mockk(),
          parser = parser,
        ),
      )
    every { defaultRepository.fhirEngine } returns fhirEngine
  }

  @Test
  fun `MeasureReportWorker doWork executes successfully`() = runTest {
    coEvery { fhirEngine.search<Measure>(any()) } returns listOf()
    val result = measureReportWorker.doWork()
    Assert.assertEquals(result, ListenableWorker.Result.success())
  }

  fun `MeasureReportWork doWork executes successfully with different scenario's`() = runTest {
    coEvery { fhirEngine.search<Measure>(any()) } returns
      listOf(
        SearchResult(
          resource = Measure().apply {},
          included = null,
          revIncluded = null,
        ),
      )

    val result = measureReportWorker.doWork()
    Assert.assertEquals(result, ListenableWorker.Result.success())
    TODO("Assert different measure ")
  }

  inner class MeasureReportWorkerFactory() : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters,
    ): ListenableWorker {
      return MeasureReportWorker(
        appContext = context,
        workerParams = workerParameters,
        defaultRepository = defaultRepository,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = mockk(),
        fhirOperator = fhirOperator,
        fhirEngine = fhirEngine,
        workManager = mockk(),
      )
    }
  }
}
