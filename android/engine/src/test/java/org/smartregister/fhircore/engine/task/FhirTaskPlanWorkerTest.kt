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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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

class FhirTaskPlanWorkerTest : RobolectricTest() {

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()
  private lateinit var context: Context
  val fhirEngine: FhirEngine = mockk()
  val defaultRepository: DefaultRepository = mockk()
  val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    every { sharedPreferencesHelper.read(FhirTaskPlanWorker.WORK_ID.lastOffset(), "0") } returns
      "100"
    every { sharedPreferencesHelper.write(FhirTaskPlanWorker.WORK_ID.lastOffset(), "101") } just
      runs
    every { defaultRepository.fhirEngine } returns fhirEngine
  }

  @Test
  fun `FhirTaskPlanWorker doWork executes successfully`() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns
      listOf(Task().apply { status = Task.TaskStatus.REQUESTED })

    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
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
    every { sharedPreferencesHelper.write(FhirTaskPlanWorker.WORK_ID.lastOffset(), "104") } just
      runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
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
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
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
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
  }

  @Test
  fun `FhirTaskPlanWorker doWork task fails when past end no reference`() {
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork task fails when past end reference not CarePlan`() {
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
          basedOn = listOf(Reference().apply { reference = "Patient" })
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork task fails when past end CarePlan found no ID`() {
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
          basedOn = listOf(Reference().apply { reference = "CarePlan/" })
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork task fails when past end found CarePlan with no ID`() {
    val carePlanId = "123"
    val carePlan = CarePlan().apply { id = carePlanId }
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
          basedOn = listOf(Reference().apply { reference = "CarePlan/$carePlanId" })
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns carePlan
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
    Assert.assertNotEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork task fails when past end found CarePlan with empty reference`() {
    val carePlanId = "123"
    val carePlan =
      CarePlan().apply {
        id = carePlanId
        activity = listOf(CarePlan.CarePlanActivityComponent())
      }
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
          basedOn = listOf(Reference().apply { reference = "CarePlan/$carePlanId" })
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns carePlan
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
    Assert.assertNotEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork task fails when past end found CarePlan not task ID`() {
    val carePlanId = "123"
    val carePlan =
      CarePlan().apply {
        id = carePlanId
        activity =
          listOf(
            CarePlan.CarePlanActivityComponent().apply {
              outcomeReference = listOf(Reference().apply { reference = "Task/456" })
            }
          )
      }
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
          basedOn = listOf(Reference().apply { reference = "CarePlan/$carePlanId" })
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns carePlan
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
    Assert.assertNotEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork task ready when past end found CarePlan and this is last task`() {
    val carePlanId = "123"
    val task =
      Task().apply {
        id = "456"
        status = Task.TaskStatus.REQUESTED
        executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
        basedOn = listOf(Reference().apply { reference = "CarePlan/$carePlanId" })
      }
    val carePlan =
      CarePlan().apply {
        activity =
          listOf(
            CarePlan.CarePlanActivityComponent().apply {
              outcomeReference = listOf(Reference().apply { reference = "Task/${task.id}" })
            }
          )
      }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { defaultRepository.update(task) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns carePlan
    coEvery { defaultRepository.update(carePlan) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(
          FhirTaskPlanWorkerFactory(fhirEngine, sharedPreferencesHelper, configurationRegistry)
        )
        .build()
    val result = worker.startWork().get()
    coVerify { defaultRepository.update(task) }
    coVerify { fhirEngine.get(ResourceType.CarePlan, carePlanId) }
    coVerify { defaultRepository.update(carePlan) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
    Assert.assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)
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
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
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
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
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
      return FhirTaskPlanWorker(
        appContext = appContext,
        workerParams = workerParameters,
        defaultRepository = defaultRepository,
        sharedPreferencesHelper = sharedPreferencesHelper,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = coroutineTestRule.testDispatcherProvider
      )
    }
  }
}
