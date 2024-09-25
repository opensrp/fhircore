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
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.engine.util.extension.referenceValue

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 24-11-2022. */
@HiltAndroidTest
class FhirResourceExpireWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var parser: IParser
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var defaultRepository: DefaultRepository

  val task =
    Task().apply {
      id = UUID.randomUUID().toString()
      status = Task.TaskStatus.READY
      executionPeriod =
        Period().apply {
          start = Date().plusMonths(-1)
          end = Date().plusDays(-1)
        }
      restriction =
        Task.TaskRestrictionComponent().apply {
          period = Period().apply { end = DateTime().plusDays(-2).toDate() }
        }
    }
  private val serviceRequest =
    ServiceRequest().apply {
      id = UUID.randomUUID().toString()
      status = ServiceRequest.ServiceRequestStatus.COMPLETED
    }

  private lateinit var fhirResourceUtil: FhirResourceUtil
  private lateinit var fhirResourceExpireWorker: FhirResourceExpireWorker

  @Before
  fun setup() {
    hiltRule.inject()

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
          context = ApplicationProvider.getApplicationContext(),
        ),
      )

    fhirResourceUtil =
      FhirResourceUtil(
        ApplicationProvider.getApplicationContext(),
        defaultRepository,
        configurationRegistry,
      )

    initializeWorkManager()
    fhirResourceExpireWorker =
      TestListenableWorkerBuilder<FhirResourceExpireWorker>(
          ApplicationProvider.getApplicationContext(),
        )
        .setWorkerFactory(FhirResourceExpireJobWorkerFactory())
        .build()
    runBlocking { fhirEngine.create(serviceRequest, task) }
  }

  @Test
  fun doWorkShouldFetchTasksAndMarkAsExpired() {
    val result = runBlocking { fhirResourceExpireWorker.doWork() }

    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `FhirResourceExpireWorker doWork task expires when past end no reference to careplan`() =
    runTest {
      fhirEngine.create(task)
      coEvery { defaultRepository.update(any()) } just runs
      val result = fhirResourceExpireWorker.startWork().get()
      val taskSlot = slot<Task>()
      coVerify { defaultRepository.update(capture(taskSlot)) }
      assertEquals(result, (ListenableWorker.Result.success()))
      val updatedTask = taskSlot.captured
      assertEquals(Task.TaskStatus.CANCELLED, updatedTask.status)
    }

  @Test
  fun `FhirResourceExpireWorker doWork task expires when past end found CarePlan was not found`() =
    runTest {
      task.basedOn = listOf(Reference().apply { reference = "CarePlan/123" })

      fhirEngine.create(task)
      coEvery { defaultRepository.update(any()) } just runs
      coEvery { fhirEngine.get(ResourceType.CarePlan, any()) } throws IllegalArgumentException()
      val result = fhirResourceExpireWorker.startWork().get()
      val taskSlot = slot<Task>()
      coVerify { defaultRepository.update(capture(taskSlot)) }
      assertEquals(result, (ListenableWorker.Result.success()))
      val updatedTask = taskSlot.captured
      assertEquals(Task.TaskStatus.CANCELLED, updatedTask.status)
    }

  @Test
  fun `FhirResourceExpireWorker doWork task expires when past end found CarePlan no reference to current task ID`() =
    runTest {
      val carePlanId = "1234"
      val taskReferencedInCarePlan =
        Task().apply {
          id = UUID.randomUUID().toString()
          status = Task.TaskStatus.READY
          executionPeriod =
            Period().apply {
              start = Date().plusMonths(-1)
              end = Date().plusDays(-1)
            }
          restriction =
            Task.TaskRestrictionComponent().apply {
              period = Period().apply { end = DateTime().plusDays(-2).toDate() }
            }
        }
      val carePlan =
        CarePlan().apply {
          id = carePlanId
          activity =
            listOf(
              CarePlan.CarePlanActivityComponent().apply {
                outcomeReference = listOf(taskReferencedInCarePlan.asReference())
              },
            )
          status = CarePlan.CarePlanStatus.ACTIVE
        }
      task.addBasedOn(Reference().apply { reference = carePlan.referenceValue() })
      fhirEngine.create(task, carePlan)
      coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns carePlan
      val result = fhirResourceExpireWorker.startWork().get()

      assertEquals(result, (ListenableWorker.Result.success()))
      val updatedCarePlan = fhirEngine.get<CarePlan>(carePlanId)
      assertEquals(CarePlan.CarePlanStatus.ACTIVE, updatedCarePlan.status)
      val updatedTask = fhirEngine.get<Task>(task.id)
      assertEquals(Task.TaskStatus.CANCELLED, updatedTask.status)
    }

  @Test
  fun `FhirResourceExpireWorker doWork task expires when past end found CarePlan and this is last task`() =
    runTest {
      val carePlanId = "123"
      val carePlan =
        CarePlan().apply {
          id = carePlanId
          activity =
            listOf(
              CarePlan.CarePlanActivityComponent().apply {
                outcomeReference = listOf(Reference().apply { reference = task.referenceValue() })
              },
            )
          status = CarePlan.CarePlanStatus.ACTIVE
        }
      task.basedOn = listOf(carePlan.asReference())
      fhirEngine.create(task, carePlan)

      val result = fhirResourceExpireWorker.startWork().get()
      coVerify { fhirEngine.get(ResourceType.CarePlan, carePlanId) }

      assertEquals(result, (ListenableWorker.Result.success()))
      val updatedCarePlan = fhirEngine.get<CarePlan>(carePlanId)
      assertEquals(CarePlan.CarePlanStatus.COMPLETED, updatedCarePlan.status)
      val updatedTask = fhirEngine.get<Task>(task.id)
      assertEquals(Task.TaskStatus.CANCELLED, updatedTask.status)
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
      config,
    )
  }

  inner class FhirResourceExpireJobWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters,
    ): ListenableWorker {
      return FhirResourceExpireWorker(
        context = appContext,
        workerParams = workerParameters,
        defaultRepository = defaultRepository,
        fhirResourceUtil = fhirResourceUtil,
        dispatcherProvider = dispatcherProvider,
      )
    }
  }
}
