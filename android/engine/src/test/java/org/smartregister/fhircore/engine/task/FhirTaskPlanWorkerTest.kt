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
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.hasPastEnd
import org.smartregister.fhircore.engine.util.extension.isReady

class FhirTaskPlanWorkerTest : RobolectricTest() {

  private lateinit var context: Context
  val fhirEngine: FhirEngine = mockk()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun `FhirTaskPlanWorker doWork executes successfully`() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns
      listOf(Task().apply { status = Task.TaskStatus.REQUESTED })
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
  }

  @Test
  fun `FhirTaskPlanWorker doWork executes successfully when status is inProgress`() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns
      listOf(Task().apply { status = Task.TaskStatus.INPROGRESS })
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
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
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
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
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
  }

  @Test
  fun `FhirTaskPlanWorker doWork fails when past end no CarePlan found`() {
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { fhirEngine.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    coVerify { fhirEngine.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork fails when past end CarePlan found no ID`() {
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
          basedOn = listOf(Reference().apply { reference = "CarePlan/" })
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { fhirEngine.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    coVerify { fhirEngine.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork fails when past end found CarePlan with ID`() {
    val carePlanId = "123"
    val task =
      Task()
        .apply {
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
          basedOn = listOf(Reference().apply { reference = "CarePlan/$carePlanId" })
        }
        .apply { isReady() }
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns listOf(task)
    coEvery { fhirEngine.update(task) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns CarePlan()
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    coVerify { fhirEngine.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.FAILED, task.status)
  }

  @Test
  fun `FhirTaskPlanWorker doWork fails when past end found CarePlan and this is last task`() {
    val carePlanId = "123"
    val task =
      Task()
        .apply {
          id = "456"
          status = Task.TaskStatus.REQUESTED
          executionPeriod = Period().apply { end = DateTime.now().minusDays(2).toDate() }
          basedOn = listOf(Reference().apply { reference = "CarePlan/$carePlanId" })
        }
        .apply { isReady() }
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
    coEvery { fhirEngine.update(task) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns carePlan
    coEvery { fhirEngine.update(carePlan) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    coVerify { fhirEngine.update(task) }
    coVerify { fhirEngine.get(ResourceType.CarePlan, carePlanId) }
    coVerify { fhirEngine.update(carePlan) }
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
    coEvery { fhirEngine.update(task) } just runs
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    coVerify { fhirEngine.update(task) }
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
    Assert.assertEquals(Task.TaskStatus.READY, task.status)
  }

  class FhirTaskPlanWorkerFactory(val fhirEngine: FhirEngine) : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters
    ): ListenableWorker? {
      return FhirTaskPlanWorker(appContext, workerParameters, fhirEngine)
    }
  }
}
