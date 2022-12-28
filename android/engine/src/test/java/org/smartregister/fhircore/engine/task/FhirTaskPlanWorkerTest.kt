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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.plusDays

@HiltAndroidTest
class FhirTaskPlanWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var fhirEngine: FhirEngine
  private lateinit var fhirTaskPlanWorker: FhirTaskPlanWorker
  lateinit var context: Context

  @Before
  fun setup() {
    hiltRule.inject()

    context = ApplicationProvider.getApplicationContext()
    initializeWorkManager()
    fhirTaskPlanWorker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanJobWorkerFactory())
        .build()
  }

  @Test
  fun doWorkShouldVerifyAllTaskAndCarePlanStatus() = runBlocking {
    fhirEngine.create(
      Task().apply {
        id = "1"
        status = Task.TaskStatus.REQUESTED
        executionPeriod =
          Period().apply {
            start = Date().plusDays(-10)
            end = Date().plusDays(-8)
          }
      },
      CarePlan().apply {
        id = "999"
        status = CarePlan.CarePlanStatus.ACTIVE
        addActivity().apply { addOutcomeReference().apply { reference = "Task/2" } }
      },
      Task().apply {
        id = "2"
        status = Task.TaskStatus.REQUESTED
        executionPeriod =
          Period().apply {
            start = Date().plusDays(-10)
            end = Date().plusDays(-8)
          }
        basedOn = listOf(Reference().apply { reference = ResourceType.CarePlan.name + "/999" })
      },
      Task().apply {
        id = "3"
        status = Task.TaskStatus.REQUESTED
        executionPeriod =
          Period().apply {
            start = Date()
            end = Date().plusDays(10)
          }
      }
    )

    val result = fhirTaskPlanWorker.doWork()
    val updatedTask1 = fhirEngine.get(ResourceType.Task, "1") as Task
    val updatedTask2 = fhirEngine.get(ResourceType.Task, "2") as Task
    val updatedCarePlan = fhirEngine.get(ResourceType.CarePlan, "999") as CarePlan
    val updatedTask3 = fhirEngine.get(ResourceType.Task, "3") as Task

    assertEquals(Task.TaskStatus.FAILED, updatedTask1.status)
    assertEquals(Task.TaskStatus.FAILED, updatedTask2.status)
    assertEquals(CarePlan.CarePlanStatus.COMPLETED, updatedCarePlan.status)
    assertEquals(Task.TaskStatus.READY, updatedTask3.status)

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

  inner class FhirTaskPlanJobWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters
    ): ListenableWorker {
      return FhirTaskPlanWorker(appContext, workerParameters, fhirEngine)
    }
  }
}
