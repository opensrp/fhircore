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

package org.smartregister.fhircore.engine.util.extension

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Test

class TaskExtensionTest {

  @Test
  fun testHasPastEnd() {
    val taskNoEnd = Task().apply { executionPeriod.start = Date() }
    Assert.assertFalse(taskNoEnd.hasPastEnd())

    val task = Task().apply { executionPeriod.end = Date() }
    Assert.assertFalse(task.hasPastEnd())

    val anotherTask =
      Task().apply { executionPeriod.end = Date(LocalDate.parse("1972-12-12").toEpochDay()) }
    Assert.assertTrue(anotherTask.hasPastEnd())
  }

  @Test
  fun testHasStarted() {
    val taskNoExecutionPeriod = Task()
    Assert.assertFalse(taskNoExecutionPeriod.hasStarted())

    val taskNoStart = Task().apply { executionPeriod.end = Date() }
    Assert.assertFalse(taskNoStart.hasStarted())

    val task = Task().apply { executionPeriod.start = Date() }
    Assert.assertTrue(task.hasStarted())

    val anotherTask =
      Task().apply {
        executionPeriod.end =
          Date.from(LocalDate.now().plusDays(8).atStartOfDay(ZoneId.systemDefault()).toInstant())
      }
    Assert.assertFalse(anotherTask.hasStarted())
  }

  @Test
  fun `task is ready if date today is between start and end dates`() {
    val task1 =
      Task().apply {
        executionPeriod.start =
          Date.from(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant())
        executionPeriod.end =
          Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
      }
    Assert.assertTrue(task1.isReady())

    val task2 =
      Task().apply {
        executionPeriod.start =
          Date.from(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant())
      }
    Assert.assertTrue(task2.isReady())

    val task3 =
      Task().apply {
        executionPeriod.start =
          Date.from(LocalDate.now().plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant())
      }
    Assert.assertFalse(task3.isReady())

    val task4 =
      Task().apply {
        executionPeriod.start =
          Date.from(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant())
        executionPeriod.end =
          Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
      }
    Assert.assertFalse(task4.isReady())
  }

  @Test
  fun `executionStartIsBeforeOrToday returns true if date is before or today`() {
    val task1 = Task()

    Assert.assertFalse(task1.executionStartIsBeforeOrToday())

    task1.executionPeriod.end = Date()

    Assert.assertFalse(task1.executionStartIsBeforeOrToday())

    task1.executionPeriod.start =
      Date.from(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant())

    Assert.assertTrue(task1.executionStartIsBeforeOrToday())

    task1.executionPeriod.start =
      Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())

    Assert.assertFalse(task1.executionStartIsBeforeOrToday())
  }

  @Test
  fun `executionEndIsAfterOrToday returns true if date is after or today`() {
    val task1 = Task()

    Assert.assertFalse(task1.executionEndIsAfterOrToday())

    task1.executionPeriod.start = Date()

    Assert.assertFalse(task1.executionEndIsAfterOrToday())

    task1.executionPeriod.end =
      Date.from(LocalDate.now().plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant())

    Assert.assertTrue(task1.executionEndIsAfterOrToday())

    task1.executionPeriod.end =
      Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())

    Assert.assertFalse(task1.executionEndIsAfterOrToday())
  }

  @Test
  fun testToCoding() {
    val task = Task().apply { status = Task.TaskStatus.ACCEPTED }
    val coding = task.status.toCoding()
    Assert.assertNotNull(coding)
    Assert.assertEquals(task.status.system, coding.system)
    Assert.assertEquals(task.status.toCode(), coding.code)
    Assert.assertEquals(task.status.display, coding.display)
  }

  @Test
  fun `isPastExpiry no restriction`() {
    val task = Task()
    Assert.assertFalse(task.isPastExpiry())
  }

  @Test
  fun `isPastExpiry restriction, no period`() {
    val task = Task().apply { restriction = Task.TaskRestrictionComponent() }
    Assert.assertFalse(task.isPastExpiry())
  }

  @Test
  fun `isPastExpiry restriction, period, no end`() {
    val task =
      Task().apply { restriction = Task.TaskRestrictionComponent().apply { period = Period() } }
    Assert.assertFalse(task.isPastExpiry())
  }

  @Test
  fun `isPastExpiry restriction, period, end before today`() {
    val task =
      Task().apply {
        restriction =
          Task.TaskRestrictionComponent().apply {
            period = Period().apply { end = Date().plusDays(1) }
          }
      }
    Assert.assertFalse(task.isPastExpiry())
  }

  @Test
  fun `isPastExpiry restriction, period, end after today`() {
    val task =
      Task().apply {
        restriction =
          Task.TaskRestrictionComponent().apply {
            period = Period().apply { end = Date().plusDays(-1) }
          }
      }
    Assert.assertTrue(task.isPastExpiry())
  }
}
