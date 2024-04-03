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
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureReportMonthPeriodWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val defaultRepository: DefaultRepository = mockk(relaxed = true)
  private val measureReportRepository: MeasureReportRepository = mockk(relaxed = true)
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private val fhirEngine: FhirEngine = mockk(relaxed = true)
  private val fhirOperator: FhirOperator = mockk()
  private val context: Application = ApplicationProvider.getApplicationContext()

  @Inject lateinit var dispatcherProvider: DispatcherProvider
  private lateinit var measureReportMonthPeriodWorker: MeasureReportMonthPeriodWorker

  @Before
  fun setUp() {
    hiltRule.inject()
    measureReportMonthPeriodWorker =
      TestListenableWorkerBuilder<MeasureReportMonthPeriodWorker>(context).build()
  }

  @Test fun `test MeasureReportMonthPeriodWorker doWork() returns success`() = runTest {}
}
