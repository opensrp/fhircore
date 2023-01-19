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

package org.smartregister.fhircore.engine.task

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class FhirTaskPlanWorkerTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  lateinit var fhirEngine: FhirEngine
  private lateinit var fhirTaskPlanWorker: FhirTaskPlanWorker
  val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setup() {
    hiltRule.inject()
    fhirEngine = mockk()
    initializeWorkManager()
    fhirTaskPlanWorker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskExpireJobWorkerFactory())
        .build()
  }

  @Test
  fun doWorkShouldSucceed() = runTest {
    val pastDate = DateTime().minusDays(4).toDate()
    val tomorrow = DateTime().plusDays(1).toDate()
    val tasks =
      mutableListOf(
        Task().apply {
          status = Task.TaskStatus.READY
          executionPeriod =
            Period().apply {
              start = pastDate
              end = tomorrow
            }
        },
        Task().apply {
          status = Task.TaskStatus.READY
          executionPeriod =
            Period().apply {
              start = pastDate
              end = pastDate
            }
        }
      )

    coEvery { fhirEngine.search<Task>(any()) } returns tasks
    coEvery { fhirEngine.update(any()) } just runs

    val result = fhirTaskPlanWorker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
  }

  private fun initializeWorkManager() {
    val config: Configuration =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    // Initialize WorkManager for instrumentation tests.
    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
  }

  inner class FhirTaskExpireJobWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters
    ): ListenableWorker {
      return FhirTaskPlanWorker(appContext, workerParameters, fhirEngine)
    }
  }
}
