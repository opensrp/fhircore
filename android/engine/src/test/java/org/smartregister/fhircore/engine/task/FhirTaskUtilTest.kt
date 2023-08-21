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

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.get
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.toCoding
import org.smartregister.fhircore.engine.util.extension.today

@HiltAndroidTest
class FhirTaskUtilTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  private lateinit var fhirTaskUtil: FhirTaskUtil
  private lateinit var fhirEngine: FhirEngine
  private lateinit var defaultRepository: DefaultRepository
  private lateinit var fhirResourceClosureUtil: FhirResourceClosureUtil

  @Before
  fun setup() {
    hiltAndroidRule.inject()
    fhirEngine = spyk(FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext()))
    defaultRepository = mockk()
    fhirResourceClosureUtil = mockk()
    every { defaultRepository.fhirEngine } returns fhirEngine
    fhirTaskUtil =
      spyk(
        FhirTaskUtil(
          ApplicationProvider.getApplicationContext(),
          defaultRepository,
          fhirResourceClosureUtil,
        ),
      )
  }

  @Test
  fun fetchOverdueTasks() {
    val taskList = mutableListOf<Task>()

    for (i in 1..4) {
      taskList.add(
        spyk(
          Task().apply {
            id = UUID.randomUUID().toString()
            status = TaskStatus.INPROGRESS
            executionPeriod =
              Period().apply {
                start = Date().plusDays(-10)
                end = Date().plusDays(-1)
              }
            restriction =
              Task.TaskRestrictionComponent().apply {
                period = Period().apply { end = today().plusDays(i % 2) }
              }
          },
        ),
      )
    }

    coEvery { fhirEngine.search<Task>(any<Search>()) } returns taskList
    coEvery { defaultRepository.update(any()) } just runs

    val tasks = runBlocking { fhirTaskUtil.expireOverdueTasks() }

    assertEquals(2, tasks.size)

    tasks.forEach {
      assertEquals(TaskStatus.CANCELLED, it.status)
      coVerify { defaultRepository.update(it) }
    }
  }

  @Test
  fun fetchOverdueTasksAndCompleteCarePlan() {
    val taskList = mutableListOf<Task>()

    for (i in 1..4) {
      taskList.add(
        spyk(
          Task().apply {
            id = UUID.randomUUID().toString()
            status = TaskStatus.INPROGRESS
            executionPeriod =
              Period().apply {
                start = Date().plusDays(-10)
                end = Date().plusDays(-1)
              }
            restriction =
              Task.TaskRestrictionComponent().apply { period = Period().apply { end = today() } }
            basedOn.add(Reference().apply { reference = "CarePlan/123" })
          },
        ),
      )
    }

    val carePlan =
      CarePlan().apply {
        id = "123"
        status = CarePlan.CarePlanStatus.ACTIVE
        activityFirstRep.detail.kind = CarePlan.CarePlanActivityKind.TASK
        activityFirstRep.outcomeReference.add(
          Reference().apply { reference = "Task/${taskList.first().logicalId}" },
        )
      }

    coEvery { fhirEngine.search<Task>(any<Search>()) } returns taskList
    coEvery { fhirEngine.get<CarePlan>(any()) } returns carePlan

    coEvery { defaultRepository.update(any()) } just runs

    val tasks = runBlocking { fhirTaskUtil.expireOverdueTasks() }

    assertEquals(4, tasks.size)

    tasks.forEach {
      assertEquals(TaskStatus.CANCELLED, it.status)
      coVerify { defaultRepository.update(it) }
    }

    assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)
  }

  @Test
  fun fetchOverdueTasksNoTasks() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns emptyList()

    val tasks = runBlocking { fhirTaskUtil.expireOverdueTasks() }

    assertEquals(0, tasks.size)

    coVerify(inverse = true) { defaultRepository.update(any()) }
  }

  @Test
  fun testUpdateTaskStatuses() {
    val task =
      Task().apply {
        id = "test-task-id"
        partOf = listOf(Reference("Task/parent-test-task-id"))
        executionPeriod =
          Period().apply {
            start = Date().plusDays(-5)
            status = TaskStatus.REQUESTED
          }
      }

    coEvery {
      fhirEngine.search<Task> {
        filter(
          Task.STATUS,
          { value = of(TaskStatus.REQUESTED.toCoding()) },
          { value = of(TaskStatus.ACCEPTED.toCoding()) },
          { value = of(TaskStatus.RECEIVED.toCoding()) },
        )
        filter(
          Task.PERIOD,
          {
            prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
            value = of(DateTimeType(Date().plusDays(-1)))
          },
        )
      }
    } returns listOf(task)

    coEvery { fhirEngine.get<Task>(any()).status.isIn(TaskStatus.COMPLETED) } returns true

    coEvery { defaultRepository.update(any()) } just runs

    assertEquals(TaskStatus.REQUESTED, task.status)

    runBlocking { fhirTaskUtil.updateUpcomingTasksToDue() }

    coVerify { defaultRepository.update(task) }

    assertEquals(TaskStatus.READY, task.status)
  }
}
