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
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.runs
import io.mockk.spyk
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.today

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 10-11-2022. */
@HiltAndroidTest
class FhirTaskExpireUtilTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  @Inject lateinit var sharedPreferenceHelper: SharedPreferencesHelper
  private lateinit var fhirTaskExpireUtil: FhirTaskExpireUtil
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setup() {
    hiltAndroidRule.inject()
    fhirEngine = spyk(FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext()))
    fhirTaskExpireUtil =
      spyk(FhirTaskExpireUtil(ApplicationProvider.getApplicationContext(), fhirEngine))
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun fetchOverdueTasks() {
    val authoredOnToday = Date()
    val taskList = mutableListOf<Task>()

    for (i in 1..4) {
      taskList.add(
        spyk(
          Task().apply {
            id = UUID.randomUUID().toString()
            status = TaskStatus.INPROGRESS
            authoredOn = authoredOnToday
            restriction =
              Task.TaskRestrictionComponent().apply { period = Period().apply { end = today() } }
          }
        )
      )
    }

    val twoDaysFromToday = DateTime().plusDays(2)

    for (i in 1..8) {
      taskList.add(
        spyk(
          Task().apply {
            id = UUID.randomUUID().toString()
            status = TaskStatus.INPROGRESS
            authoredOn = authoredOnToday
            restriction =
              Task.TaskRestrictionComponent().apply {
                period = Period().apply { end = twoDaysFromToday.toDate() }
              }
          }
        )
      )
    }

    coEvery { fhirEngine.search<Task>(any<Search>()) } returns taskList
    coEvery { fhirEngine.update(any()) } just runs

    val (maxDate, tasks) =
      runBlocking { fhirTaskExpireUtil.expireOverdueTasks(lastAuthoredOnDate = null) }

    assertEquals(4, tasks.size)
    assertEquals(authoredOnToday, maxDate)

    taskList.toSet().subtract(tasks.toSet()).forEach {
      assertEquals(TaskStatus.INPROGRESS, it.status)
      coVerify(inverse = true) { fhirEngine.update(it) }
    }

    tasks.forEach {
      assertEquals(TaskStatus.CANCELLED, it.status)
      coVerify { fhirEngine.update(it) }
    }
  }

  @Test
  fun fetchOverdueTasksWithLastAuthoredOnDate() {
    val authoredOnToday = Date()
    val twoDaysAgo = authoredOnToday.plusDays(-2)
    val twoDaysAhead = authoredOnToday.plusDays(2)
    val taskList = mutableListOf<Task>()

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
      )
    }

    for (i in 1..8) {
      taskList.add(
        spyk(
          Task().apply {
            id = UUID.randomUUID().toString()
            status = TaskStatus.INPROGRESS
            authoredOn = twoDaysAgo
            restriction =
              Task.TaskRestrictionComponent().apply { period = Period().apply { end = today() } }
          }
        )
      )
    }

    coEvery { fhirEngine.update(any()) } just runs

    val (maxDate, tasks) =
      runBlocking {
        taskList.forEach { fhirEngine.create(it) }
        fhirTaskExpireUtil.expireOverdueTasks(lastAuthoredOnDate = authoredOnToday)
      }

    assertEquals(4, tasks.size)
    assertEquals(twoDaysAhead.toString(), maxDate.toString())

    taskList.toSet().subtract(tasks.toSet()).forEach {
      assertEquals(TaskStatus.INPROGRESS, it.status)
      coVerify(inverse = true) { fhirEngine.update(it) }
    }

    tasks.forEach {
      assertEquals(TaskStatus.CANCELLED, it.status)
      coVerify { fhirEngine.update(it) }
    }
  }

  @Test
  fun fetchOverdueTasksNoTasks() {
    coEvery { fhirEngine.search<Task>(any<Search>()) } returns emptyList()

    val (maxDate, tasks) =
      runBlocking { fhirTaskExpireUtil.expireOverdueTasks(lastAuthoredOnDate = null) }

    assertEquals(0, tasks.size)
    assertEquals(null, maxDate)

    coVerify(inverse = true) { fhirEngine.update(any()) }
  }
}
