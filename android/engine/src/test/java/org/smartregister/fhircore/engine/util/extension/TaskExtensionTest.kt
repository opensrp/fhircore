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

package org.smartregister.fhircore.engine.util.extension

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Test

class TaskExtensionTest {

  private val testZoneId: ZoneId = ZoneId.of("UTC")

  @Test
  fun testHasPastEnd() {
    val task = Task().apply { executionPeriod.end = Date() }
    Assert.assertFalse(task.hasPastEnd())

    val anotherTask =
      Task().apply { executionPeriod.end = Date(LocalDate.parse("1972-12-12").toEpochDay()) }
    Assert.assertTrue(anotherTask.hasPastEnd())
  }

  @Test
  fun testHasStarted() {
    val task = Task().apply { executionPeriod.start = Date() }
    Assert.assertTrue(task.hasStarted())

    val anotherTask =
      Task().apply {
        executionPeriod.end =
          Date.from(LocalDate.now(testZoneId).plusDays(8).atStartOfDay(testZoneId).toInstant())
      }
    Assert.assertFalse(anotherTask.hasStarted())
  }

  @Test
  fun `task is ready if start & end dates are before today`() {
    val task1 =
      Task().apply {
        executionPeriod.start =
          Date.from(LocalDate.now(testZoneId).minusDays(2).atStartOfDay(testZoneId).toInstant())
      }
    task1.apply {
      executionPeriod.end =
        Date.from(LocalDate.now(testZoneId).minusDays(1).atStartOfDay(testZoneId).toInstant())
    }
    Assert.assertTrue(task1.isReady())

    val task2 =
      Task().apply {
        executionPeriod.start =
          Date.from(LocalDate.now(testZoneId).plusDays(3).atStartOfDay(testZoneId).toInstant())
      }
    Assert.assertFalse(task2.isReady())
  }

  @Test
  fun `executionStartIsBeforeOrToday returns true if date is before or today`() {
    val task1 =
      Task().apply {
        executionPeriod.start =
          Date.from(LocalDate.now(testZoneId).minusDays(2).atStartOfDay(testZoneId).toInstant())
      }

    Assert.assertTrue(task1.executionStartIsBeforeOrToday())

    task1.apply {
      executionPeriod.start =
        Date.from(LocalDate.now(testZoneId).plusDays(1).atStartOfDay(testZoneId).toInstant())
    }

    Assert.assertFalse(task1.executionStartIsBeforeOrToday())
  }

  @Test
  fun `executionEndIsBeforeOrToday returns true if date is before or today`() {
    val task1 =
      Task().apply {
        executionPeriod.end =
          Date.from(LocalDate.now(testZoneId).minusDays(2).atStartOfDay(testZoneId).toInstant())
      }

    Assert.assertTrue(task1.executionEndIsBeforeOrToday())

    task1.apply {
      executionPeriod.end =
        Date.from(LocalDate.now(testZoneId).plusDays(1).atStartOfDay(testZoneId).toInstant())
    }

    Assert.assertFalse(task1.executionEndIsBeforeOrToday())
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
}
