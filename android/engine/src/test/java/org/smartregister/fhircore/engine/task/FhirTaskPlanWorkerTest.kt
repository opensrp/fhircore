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
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.hasPastEnd
import org.smartregister.fhircore.engine.util.extension.lastOffset

@HiltAndroidTest
class FhirTaskPlanWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()
  private lateinit var fhirTaskUtil: FhirTaskUtil
  private lateinit var context: Context
  private val fhirEngine: FhirEngine = mockk()
  private val defaultRepository: DefaultRepository = mockk()
  private val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    context = ApplicationProvider.getApplicationContext()
    every {
      sharedPreferencesHelper.read(FhirTaskStatusUpdateWorker.WORK_ID.lastOffset(), "0")
    } returns "100"
    every {
      sharedPreferencesHelper.write(FhirTaskStatusUpdateWorker.WORK_ID.lastOffset(), "101")
    } just runs
    every { defaultRepository.fhirEngine } returns fhirEngine

    fhirTaskUtil =
      spyk(
        FhirTaskUtil(
          appContext = ApplicationProvider.getApplicationContext(),
          defaultRepository = defaultRepository
        )
      )
  }

  @Test
  fun `FhirTaskPlanWorker doWork executes successfully`() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns
      listOf(Task().apply { status = Task.TaskStatus.REQUESTED })

    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
  }

  @Test
  fun `FhirTaskPlanWorker doWork executes successfully for requested, accepted, inProgress, and received`() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns
      listOf(
        Task().apply { status = Task.TaskStatus.REQUESTED },
        Task().apply { status = Task.TaskStatus.ACCEPTED },
        Task().apply { status = Task.TaskStatus.INPROGRESS },
        Task().apply { status = Task.TaskStatus.RECEIVED }
      )
    every {
      sharedPreferencesHelper.write(FhirTaskStatusUpdateWorker.WORK_ID.lastOffset(), "104")
    } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
  }

  @Test
  fun `FhirTaskPlanWorker doWork executes successfully when status is failed`() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns
      listOf(Task().apply { status = Task.TaskStatus.FAILED }.apply { hasPastEnd() })
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
  }

  @Test
  fun `FhirTaskPlanWorker doWork executes successfully when task is request but not ready`() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns
      listOf(Task().apply { status = Task.TaskStatus.REQUESTED })
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
  }

  @Test
  fun `FhirTaskPlanWorker doWork sets status when ready and requested`() {
    val task =
      Task().apply {
        status = Task.TaskStatus.REQUESTED
        executionPeriod = Period().apply { start = DateTime.now().minusDays(2).toDate() }
      }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.READY, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork does not set status when ready and not requested`() {
    val task =
      Task().apply {
        status = Task.TaskStatus.INPROGRESS
        executionPeriod = Period().apply { start = DateTime.now().minusDays(2).toDate() }
      }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify(inverse = true) { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.INPROGRESS, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork set the correct status when start date is in the future`() {
    val task =
      Task().apply {
        status = Task.TaskStatus.REQUESTED
        executionPeriod = Period().apply { start = DateTime.now().plusDays(1).toDate() }
      }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify(exactly = 0) { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.REQUESTED, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork set the correct status when start date is 2 years ago`() {
    val task =
      Task().apply {
        status = Task.TaskStatus.REQUESTED
        executionPeriod = Period().apply { start = DateTime.now().minusYears(2).toDate() }
      }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.READY, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork sets the correct status if start date 2 years ago and end date is before today`() {
    val task =
      Task().apply {
        status = Task.TaskStatus.REQUESTED
        executionPeriod =
          Period().apply {
            start = DateTime.now().minusYears(2).toDate()
            end = DateTime.now().minusMonths(10).toDate()
          }
      }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.READY, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork sets the correct status depending on the task status`() {
    val task =
      Task().apply {
        status = Task.TaskStatus.INPROGRESS
        executionPeriod =
          Period().apply {
            start = DateTime.now().minusYears(2).toDate()
            end = DateTime.now().minusMonths(10).toDate()
          }
      }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify(inverse = true) { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.INPROGRESS, task.status)
  }

  inner class FhirTaskPlanWorkerFactory(
    val fhirEngine: FhirEngine,
    val sharedPreferencesHelper: SharedPreferencesHelper,
    val configurationRegistry: ConfigurationRegistry
  ) : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters
    ): ListenableWorker? {
      return FhirTaskStatusUpdateWorker(
        appContext = appContext,
        workerParams = workerParameters,
        fhirTaskUtil = fhirTaskUtil,
        dispatcherProvider = coroutineTestRule.testDispatcherProvider
      )
    }
  }
}
