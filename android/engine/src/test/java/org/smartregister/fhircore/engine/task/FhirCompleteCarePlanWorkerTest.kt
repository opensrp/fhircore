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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.lastOffset

@HiltAndroidTest
class FhirCompleteCarePlanWorkerTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()
  private val fhirEngine: FhirEngine = mockk(relaxed = true)
  private val fhirCarePlanGenerator: FhirCarePlanGenerator = mockk(relaxed = true)
  private val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var fhirCompleteCarePlanWorker: FhirCompleteCarePlanWorker

  @Before
  fun setUp() {
    hiltRule.inject()
    initializeWorkManager()
    fhirCompleteCarePlanWorker =
      TestListenableWorkerBuilder<FhirCompleteCarePlanWorker>(
          ApplicationProvider.getApplicationContext()
        )
        .setWorkerFactory(FhirCompleteCarePlanWorkerFactory())
        .build()
    every {
      sharedPreferencesHelper.read(FhirCompleteCarePlanWorker.WORK_ID.lastOffset(), "0")
    } returns "100"
    every {
      sharedPreferencesHelper.write(FhirCompleteCarePlanWorker.WORK_ID.lastOffset(), "101")
    } just runs
  }

  @Test
  fun doWorkShouldMarkCarePlanAsCompleteWhenAllTasksAreCancelled() {
    val carePlan =
      CarePlan().apply {
        id = "careplan-1"
        status = CarePlan.CarePlanStatus.ACTIVE
        activity =
          listOf(
            CarePlan.CarePlanActivityComponent().apply {
              outcomeReference =
                listOf(
                  Reference("Task/f10eec84-ef78-4bd1-bac4-6e68c7548f4c"),
                  Reference("Task/4f71e93f-dccd-48bf-becd-e4c93b51f8e2")
                )
            }
          )
      }
    fhirCompleteCarePlanWorker = spyk(fhirCompleteCarePlanWorker)
    coEvery { fhirCompleteCarePlanWorker.getCarePlans(any(), any()) } returns listOf(carePlan)

    val task1 =
      Task().apply {
        id = "f10eec84-ef78-4bd1-bac4-6e68c7548f4c"
        status = Task.TaskStatus.CANCELLED
      }
    val task2 =
      Task().apply {
        id = "4f71e93f-dccd-48bf-becd-e4c93b51f8e2"
        status = Task.TaskStatus.CANCELLED
      }
    coEvery { fhirCarePlanGenerator.getTask(any()) } returnsMany listOf(task1, task2)

    Assert.assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)

    val result = runBlocking { fhirCompleteCarePlanWorker.doWork() }
    Assert.assertEquals(ListenableWorker.Result.success(), result)

    coVerify { fhirCarePlanGenerator.getTask(task1.id) }
    coVerify { fhirCarePlanGenerator.getTask(task2.id) }

    val carePlanSlot = slot<CarePlan>()
    coVerify { fhirEngine.update(capture(carePlanSlot)) }
    Assert.assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlanSlot.captured.status)
  }

  @Test
  fun doWorkShouldMarkCarePlanAsCompleteWhenAllTasksAreAMixOfCancelledAndCompleted() {
    val carePlan =
      CarePlan().apply {
        id = "careplan-1"
        status = CarePlan.CarePlanStatus.ACTIVE
        activity =
          listOf(
            CarePlan.CarePlanActivityComponent().apply {
              outcomeReference =
                listOf(
                  Reference("Task/f10eec84-ef78-4bd1-bac4-6e68c7548f4c"),
                  Reference("Task/4f71e93f-dccd-48bf-becd-e4c93b51f8e2")
                )
            }
          )
      }
    fhirCompleteCarePlanWorker = spyk(fhirCompleteCarePlanWorker)
    coEvery { fhirCompleteCarePlanWorker.getCarePlans(any(), any()) } returns listOf(carePlan)
    val task1 =
      Task().apply {
        id = "f10eec84-ef78-4bd1-bac4-6e68c7548f4c"
        status = Task.TaskStatus.COMPLETED
      }
    val task2 =
      Task().apply {
        id = "4f71e93f-dccd-48bf-becd-e4c93b51f8e2"
        status = Task.TaskStatus.CANCELLED
      }
    coEvery { fhirCarePlanGenerator.getTask(any()) } returnsMany listOf(task1, task2)

    Assert.assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)

    val result = runBlocking { fhirCompleteCarePlanWorker.doWork() }
    Assert.assertEquals(ListenableWorker.Result.success(), result)

    coVerify { fhirCarePlanGenerator.getTask(task1.id) }
    coVerify { fhirCarePlanGenerator.getTask(task2.id) }

    val carePlanSlot = slot<CarePlan>()
    coVerify { fhirEngine.update(capture(carePlanSlot)) }
    Assert.assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlanSlot.captured.status)
  }

  @Test
  fun doWorkShouldNotUpdateCarePlanStatusWhenSomeOfTheTasksAreNotCancelledOrCompleted() {
    val carePlan =
      CarePlan().apply {
        id = "careplan-1"
        status = CarePlan.CarePlanStatus.ACTIVE
        activity =
          listOf(
            CarePlan.CarePlanActivityComponent().apply {
              outcomeReference =
                listOf(
                  Reference("Task/f10eec84-ef78-4bd1-bac4-6e68c7548f4c"),
                  Reference("Task/4f71e93f-dccd-48bf-becd-e4c93b51f8e2"),
                  Reference("Task/56a7824a-d76b-4a20-844d-e975b66fde61")
                )
            }
          )
      }
    fhirCompleteCarePlanWorker = spyk(fhirCompleteCarePlanWorker)
    coEvery { fhirCompleteCarePlanWorker.getCarePlans(any(), any()) } returns listOf(carePlan)
    val task1 =
      Task().apply {
        id = "f10eec84-ef78-4bd1-bac4-6e68c7548f4c"
        status = Task.TaskStatus.DRAFT
      }
    val task2 =
      Task().apply {
        id = "4f71e93f-dccd-48bf-becd-e4c93b51f8e2"
        status = Task.TaskStatus.CANCELLED
      }
    val task3 =
      Task().apply {
        id = "56a7824a-d76b-4a20-844d-e975b66fde61"
        status = Task.TaskStatus.COMPLETED
      }
    coEvery { fhirCarePlanGenerator.getTask(any()) } returnsMany listOf(task1, task2, task3)

    Assert.assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)

    val result = runBlocking { fhirCompleteCarePlanWorker.doWork() }
    Assert.assertEquals(ListenableWorker.Result.success(), result)

    coVerify { fhirCarePlanGenerator.getTask(task1.id) }
    coVerify { fhirCarePlanGenerator.getTask(task2.id) }
    coVerify { fhirCarePlanGenerator.getTask(task3.id) }

    coVerify(exactly = 0) { fhirEngine.update(any()) }
  }

  private fun initializeWorkManager() {
    val config: Configuration =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    // Initialize WorkManager for instrumentation tests.
    WorkManagerTestInitHelper.initializeTestWorkManager(
      ApplicationProvider.getApplicationContext(),
      config
    )
  }

  inner class FhirCompleteCarePlanWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters
    ): ListenableWorker {
      return FhirCompleteCarePlanWorker(
        context = appContext,
        workerParams = workerParameters,
        fhirEngine = fhirEngine,
        fhirCarePlanGenerator = fhirCarePlanGenerator,
        sharedPreferencesHelper = sharedPreferencesHelper,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = coroutineTestRule.testDispatcherProvider
      )
    }
  }
}
