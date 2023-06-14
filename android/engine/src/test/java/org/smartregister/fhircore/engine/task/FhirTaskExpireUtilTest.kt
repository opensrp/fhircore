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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.get
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
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CarePlan
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
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.today

@HiltAndroidTest
class FhirTaskExpireUtilTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  @Inject lateinit var sharedPreferenceHelper: SharedPreferencesHelper
  private lateinit var fhirTaskExpireUtil: FhirTaskExpireUtil
  private lateinit var fhirEngine: FhirEngine
  private lateinit var defaultRepository: DefaultRepository

  @Before
  fun setup() {
    hiltAndroidRule.inject()
    fhirEngine = spyk(FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext()))
    defaultRepository = mockk()
    every { defaultRepository.fhirEngine } returns fhirEngine
    fhirTaskExpireUtil =
      spyk(FhirTaskExpireUtil(ApplicationProvider.getApplicationContext(), defaultRepository))
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
              Task.TaskRestrictionComponent().apply { period = Period().apply { end = today() } }
          }
        )
      )
    }

    coEvery { fhirEngine.search<Task>(any<Search>()) } returns taskList
    coEvery { defaultRepository.update(any()) } just runs

    val tasks = runBlocking { fhirTaskExpireUtil.expireOverdueTasks() }

    assertEquals(4, tasks.size)

    assertEquals(authoredOnToday, maxDate)

    taskList.toSet().subtract(tasks.toSet()).forEach {
      assertEquals(TaskStatus.INPROGRESS, it.status)
      coVerify(inverse = true) { defaultRepository.update(it) }
    }

    tasks.forEach {
      assertEquals(TaskStatus.CANCELLED, it.status)
      coVerify { defaultRepository.update(it) }
    }
  }

  @Test
  fun fetchOverdueTasksAndCompleteCarePlan() {
    val taskList = mutableListOf<Task>()

    for (i in 1..8) {
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
            authoredOn = twoDaysAgo
            restriction =
              Task.TaskRestrictionComponent().apply { period = Period().apply { end = today() } }
            basedOn.add(Reference().apply { reference = "CarePlan/123" })
          }
        )
      )
    }

    val carePlan =
      CarePlan().apply {
        id = "123"
        status = CarePlan.CarePlanStatus.ACTIVE
        activityFirstRep.detail.kind = CarePlan.CarePlanActivityKind.TASK
        activityFirstRep.outcomeReference.add(
          Reference().apply { reference = "Task/${taskList.first().logicalId}" }
    for (i in 1..4) {
      taskList.add(
        spyk(
          Task().apply {
            id = UUID.randomUUID().toString()
            status = TaskStatus.INPROGRESS
            authoredOn = twoDaysAhead
            restriction =
              Task.TaskRestrictionComponent().apply { period = Period().apply { end = today() } }
          }
        )
      }

    coEvery { fhirEngine.search<Task>(any<Search>()) } returns taskList
    coEvery { fhirEngine.get<CarePlan>(any()) } returns carePlan

    coEvery { defaultRepository.update(any()) } just runs
    coEvery { defaultRepository.create(true, any()) } returns emptyList()
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns taskList

    val tasks = runBlocking { fhirTaskExpireUtil.expireOverdueTasks() }

    assertEquals(4, tasks.size)
    val (maxDate, tasks) =
      runBlocking {
        taskList.forEach { defaultRepository.create(true, it) }
        fhirTaskExpireUtil.expireOverdueTasks(lastAuthoredOnDate = authoredOnToday)
      }

    assertEquals(12, tasks.size)
    assertEquals(twoDaysAhead.toString(), maxDate.toString())

    taskList.toSet().subtract(tasks.toSet()).forEach {
      assertEquals(TaskStatus.INPROGRESS, it.status)
      coVerify(inverse = true) { defaultRepository.update(it) }
    }

    tasks.forEach {
      assertEquals(TaskStatus.CANCELLED, it.status)
      coVerify { defaultRepository.update(it) }
    }

    assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)
  }

  @Test
  fun fetchOverdueTasksNoTasks() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns emptyList()

    val tasks = runBlocking { fhirTaskExpireUtil.expireOverdueTasks() }

    assertEquals(0, tasks.size)

    coVerify(inverse = true) { defaultRepository.update(any()) }
  }
}
