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

import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.get
import com.google.android.fhir.search.search
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
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.today

@HiltAndroidTest
class FhirResourceUtilTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirEngine: FhirEngine
  private lateinit var fhirResourceUtil: FhirResourceUtil
  private lateinit var defaultRepository: DefaultRepository
  private lateinit var configurationRegistry: ConfigurationRegistry

  @Before
  fun setup() {
    hiltAndroidRule.inject()
    defaultRepository = mockk()
    configurationRegistry = Faker.buildTestConfigurationRegistry()
    every { defaultRepository.fhirEngine } returns fhirEngine
    fhirResourceUtil =
      spyk(
        FhirResourceUtil(
          ApplicationProvider.getApplicationContext(),
          defaultRepository,
          configurationRegistry,
        ),
      )
  }

  @Test
  fun fetchOverdueTasks() {
    val taskList = mutableListOf<SearchResult<Task>>()

    for (i in 1..4) {
      taskList.add(
        spyk(
          SearchResult(
            resource =
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
            null,
            null,
          ),
        ),
      )
    }

    coEvery { fhirEngine.search<Task>(any()) } returns taskList
    coEvery { defaultRepository.update(any()) } just runs

    val tasks = runBlocking { fhirResourceUtil.expireOverdueTasks() }

    assertEquals(2, tasks.size)

    tasks.forEach {
      assertEquals(TaskStatus.CANCELLED, it.status)
      coVerify { defaultRepository.update(it) }
    }
  }

  @Test
  fun fetchOverdueTasksAndCompleteCarePlan() {
    val taskList = mutableListOf<SearchResult<Task>>()

    for (i in 1..4) {
      taskList.add(
        spyk(
          SearchResult(
            resource =
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
                    period = Period().apply { end = today() }
                  }
                basedOn.add(Reference().apply { reference = "CarePlan/123" })
              },
            null,
            null,
          ),
        ),
      )
    }

    val carePlan =
      CarePlan().apply {
        id = "123"
        status = CarePlan.CarePlanStatus.ACTIVE
        activityFirstRep.detail.kind = CarePlan.CarePlanActivityKind.TASK
        activityFirstRep.outcomeReference.add(
          Reference().apply { reference = "Task/${taskList.first().resource.logicalId}" },
        )
      }

    coEvery { fhirEngine.search<Task>(any()) } returns taskList
    coEvery { fhirEngine.get<CarePlan>(any()) } returns carePlan

    coEvery { defaultRepository.update(any()) } just runs

    val tasks = runBlocking { fhirResourceUtil.expireOverdueTasks() }

    assertEquals(4, tasks.size)

    tasks.forEach {
      assertEquals(TaskStatus.CANCELLED, it.status)
      coVerify { defaultRepository.update(it) }
    }

    assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)
  }

  @Test
  fun fetchOverdueTasksNoTasks() {
    coEvery { fhirEngine.search<Task>(any()) } returns emptyList()

    val tasks = runBlocking { fhirResourceUtil.expireOverdueTasks() }

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

    // Add tasks to database instead of mocking; database is in memory and reset after every test
    runBlocking { fhirEngine.create(task) }

    coEvery { fhirEngine.get<Task>(any()).status.isIn(TaskStatus.COMPLETED) } returns true

    coEvery { defaultRepository.update(any()) } just runs

    assertEquals(TaskStatus.REQUESTED, task.status)

    runBlocking { fhirResourceUtil.updateUpcomingTasksToDue() }

    val taskSlot = slot<Task>()
    coVerify { defaultRepository.update(capture(taskSlot)) }

    assertEquals(TaskStatus.READY, taskSlot.captured.status)
  }

  @Test
  fun testUpdateTaskStatusesGivenTasks() = runTest {
    val parentTask =
      Task().apply {
        id = "parent-test-task-id"
        executionPeriod =
          Period().apply {
            start = Date().plusDays(-21)
            status = TaskStatus.COMPLETED
          }
      }
    val task =
      Task().apply {
        id = "test-task-id"
        partOf = listOf(Reference("Task/${parentTask.logicalId}"))
        executionPeriod =
          Period().apply {
            start = Date().plusDays(-5)
            status = TaskStatus.REQUESTED
          }
      }

    fhirEngine.create(parentTask, task)

    coEvery { defaultRepository.update(any()) } just runs

    fhirResourceUtil.updateUpcomingTasksToDue(taskResourcesToFilterBy = listOf(task))

    val taskSlot = slot<Task>()
    coVerify { defaultRepository.update(capture(taskSlot)) }

    assertEquals(TaskStatus.READY, taskSlot.captured.status)
  }

  @Test
  fun testUpdateUpcomingTasksToDueHandlesWhenTaskResourcesToFilterByIsEmpty() {
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

    coEvery { fhirEngine.get<Task>(any()).status.isIn(TaskStatus.COMPLETED) } returns true

    coEvery { defaultRepository.update(any()) } just runs

    assertEquals(TaskStatus.REQUESTED, task.status)

    runBlocking { fhirResourceUtil.updateUpcomingTasksToDue(null, emptyList()) }

    coVerify(inverse = true) { defaultRepository.update(task) }

    assertEquals(TaskStatus.REQUESTED, task.status)
  }
}
