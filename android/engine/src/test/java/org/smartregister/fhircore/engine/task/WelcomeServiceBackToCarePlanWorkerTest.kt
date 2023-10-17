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
import com.google.android.fhir.SearchResult
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.asReference

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class WelcomeServiceBackToCarePlanWorkerTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val fhirEngine = mockk<FhirEngine>()
  private lateinit var welcomeServiceCarePlanWorker: WelcomeServiceBackToCarePlanWorker
  val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    hiltRule.inject()

    initWorkManager()
    welcomeServiceCarePlanWorker =
      TestListenableWorkerBuilder<WelcomeServiceBackToCarePlanWorker>(context)
        .setWorkerFactory(
          object : WorkerFactory() {
            override fun createWorker(
              appContext: Context,
              workerClassName: String,
              workerParameters: WorkerParameters
            ): ListenableWorker {
              return WelcomeServiceBackToCarePlanWorker(appContext, workerParameters, fhirEngine)
            }
          }
        )
        .build()
  }

  private fun initWorkManager() {
    val config =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
  }

  @Test
  fun doWorkReturnsResultSuccessWhenPatientHasNoWelcomeServiceInActiveCarePlan() = runTest {
    val patient0 = Faker.buildPatient("patient0", "doe", "john")
    val task0 =
      Task().apply {
        status = Task.TaskStatus.READY
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://d-tree.org"
              code = WelcomeServiceBackToCarePlanWorker.INTERRUPTED_TREAT_CODE
            }
          )
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
      }
    val carePlan0 =
      CarePlan().apply {
        status = CarePlan.CarePlanStatus.ACTIVE
        period = Period().apply { start = Date() }
        subject = patient0.asReference()
      }
    coEvery { fhirEngine.search<Resource>(any()) } answers
      {
        val searchObj = firstArg() as Search
        when (searchObj.type) {
          ResourceType.CarePlan ->
            listOf(SearchResult(carePlan0, included = null, revIncluded = null))
          ResourceType.Task -> listOf(SearchResult(task0, included = null, revIncluded = null))
          else -> listOf()
        }
      }

    coEvery { fhirEngine.get(ResourceType.Patient, patient0.logicalId) } returns patient0
    coEvery { fhirEngine.create(any()) } returns emptyList()
    coEvery { fhirEngine.update(carePlan0) } just runs

    val result = welcomeServiceCarePlanWorker.doWork()
    Assert.assertEquals(ListenableWorker.Result.success(), result)
    val carePlanActivity = carePlan0.activity.single()
    Assert.assertEquals(
      1,
      carePlanActivity.detail.code.coding.count {
        it.code == WelcomeServiceBackToCarePlanWorker.WELCOME_SERVICE_QUESTIONNAIRE_ID
      }
    )
    coVerify {
      fhirEngine.create(
        withArg {
          it as Task
          Assert.assertEquals(
            it.logicalId,
            IdType(carePlanActivity.outcomeReference.first().reference).idPart
          )
          Assert.assertEquals("Welcome Service", carePlanActivity.outcomeReference.first().display)
        }
      )
    }
  }

  @Test
  fun doWorkDoesNotDuplicateWelcomeServiceForPatientWithWelcomeServiceInActiveCarePlan() = runTest {
    val patient0 = Faker.buildPatient("patient0", "doe", "john")
    val task0 =
      Task().apply {
        status = Task.TaskStatus.READY
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://d-tree.org"
              code = WelcomeServiceBackToCarePlanWorker.INTERRUPTED_TREAT_CODE
            }
          )
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
      }
    val carePlan0 =
      CarePlan().apply {
        status = CarePlan.CarePlanStatus.ACTIVE
        period = Period().apply { start = Date() }
        subject = patient0.asReference()

        val taskId = UUID.randomUUID().toString()
        val taskDescription = "Welcome Service"

        val task = welcomeServiceTask(taskId, taskDescription, patient0)
        addActivity().apply {
          addOutcomeReference(task.asReference().apply { display = taskDescription })

          detail =
            CarePlan.CarePlanActivityDetailComponent().apply {
              status = CarePlan.CarePlanActivityStatus.INPROGRESS
              kind = CarePlan.CarePlanActivityKind.TASK
              description = taskDescription
              code =
                CodeableConcept(
                  Coding(
                    "https://d-tree.org",
                    WelcomeServiceBackToCarePlanWorker.WELCOME_SERVICE_QUESTIONNAIRE_ID,
                    taskDescription
                  )
                )
              scheduled = period.copy().apply { start = DateTimeType.now().value }
              addPerformer(author)
            }
        }
      }

    coEvery { fhirEngine.search<Resource>(any()) } answers
      {
        val searchObj = firstArg() as Search

        when (searchObj.type) {
          ResourceType.CarePlan ->
            listOf(SearchResult(carePlan0, included = null, revIncluded = null))
          ResourceType.Task -> listOf(SearchResult(task0, included = null, revIncluded = null))
          else -> listOf()
        }
      }

    coEvery { fhirEngine.get(ResourceType.Patient, patient0.logicalId) } returns patient0
    coEvery { fhirEngine.create(any()) } returns emptyList()
    coEvery { fhirEngine.update(carePlan0) } just runs

    val result = welcomeServiceCarePlanWorker.doWork()
    Assert.assertEquals(
      1,
      carePlan0.activity.count { act ->
        act.detail.code.coding.any {
          it.code == WelcomeServiceBackToCarePlanWorker.WELCOME_SERVICE_QUESTIONNAIRE_ID
        }
      }
    )
    Assert.assertEquals(ListenableWorker.Result.success(), result)
    coVerify(exactly = 0) { fhirEngine.create(any()) }
    coVerify(exactly = 0) { fhirEngine.update(any()) }
  }
}
