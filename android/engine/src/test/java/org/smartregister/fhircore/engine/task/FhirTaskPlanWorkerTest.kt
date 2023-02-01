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
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.mockk
import org.hl7.fhir.r4.model.Task
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
    coEvery { fhirEngine.search<Task>(any()) } returns
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
    coEvery { fhirEngine.search<Task>(any()) } returns
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
    coEvery { fhirEngine.search<Task>(any()) } returns
            listOf(Task().apply { status = Task.TaskStatus.FAILED }.apply { hasPastEnd() })
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
  }

  @Test
  fun `FhirTaskPlanWorker doWork executes successfully when task isReady`() {
    coEvery { fhirEngine.search<Task>(any()) } returns
            listOf(Task().apply { status = Task.TaskStatus.REQUESTED }.apply { isReady() })
    val worker =
      TestListenableWorkerBuilder<FhirTaskPlanWorker>(context)
        .setWorkerFactory(FhirTaskPlanWorkerFactory(fhirEngine))
        .build()
    val result = worker.startWork().get()
    Assert.assertEquals(result, (ListenableWorker.Result.success()))
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
