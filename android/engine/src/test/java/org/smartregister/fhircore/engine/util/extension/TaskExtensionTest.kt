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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TaskExtensionTest {
  private var task = Task()
  private val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  @Before
  fun setUp() {
    task =
      iParser.parseResource(
        Task::class.java,
        "{\n" +
          "   \"resourceType\":\"Task\",\n" +
          "   \"id\":\"a9100c01-c84b-404f-9d24-9b830463a152\",\n" +
          "   \"identifier\":[\n" +
          "      {\n" +
          "         \"use\":\"official\",\n" +
          "         \"value\":\"a20e88b4-4beb-4b31-86cd-572e1445e5f3\"\n" +
          "      }\n" +
          "   ],\n" +
          "   \"basedOn\":[\n" +
          "      {\n" +
          "         \"reference\":\"CarePlan/28d7542c-ba08-4f16-b6a2-19e8b5d4c229\"\n" +
          "      }\n" +
          "   ],\n" +
          "   \"partOf\":{\n" +
          "      \"reference\":\"Task/650203d2-f327-4eb4-a9fd-741e0ce29c3f\"\n" +
          "   },\n" +
          "   \"status\":\"requested\",\n" +
          "   \"intent\":\"plan\",\n" +
          "   \"priority\":\"routine\",\n" +
          "   \"code\":{\n" +
          "      \"coding\":[\n" +
          "         {\n" +
          "            \"system\":\"http://snomed.info/sct\",\n" +
          "            \"code\":\"33879002\",\n" +
          "            \"display\":\"Administration of vaccine to produce active immunity (procedure)\"\n" +
          "         }\n" +
          "      ]\n" +
          "   },\n" +
          "   \"description\":\"OPV 1 at 6 wk vaccine\",\n" +
          "   \"for\":{\n" +
          "      \"reference\":\"Patient/3e3d698a-4edb-48f9-9330-2f1adc0635d1\"\n" +
          "   },\n" +
          "   \"executionPeriod\":{\n" +
          "      \"start\":\"2021-11-12T00:00:00+00:00\",\n" +
          "      \"end\":\"2026-11-11T00:00:00+00:00\"\n" +
          "   },\n" +
          "   \"authoredOn\":\"2023-03-28T10:46:59+00:00\",\n" +
          "   \"requester\":{\n" +
          "      \"reference\":\"Practitioner/3812\"\n" +
          "   },\n" +
          "   \"owner\":{\n" +
          "      \"reference\":\"Practitioner/3812\"\n" +
          "   },\n" +
          "   \"reasonCode\":{\n" +
          "      \"coding\":[\n" +
          "         {\n" +
          "            \"system\":\"http://snomed.info/sct\",\n" +
          "            \"code\":\"111164008\",\n" +
          "            \"display\":\"Poliovirus vaccine\"\n" +
          "         }\n" +
          "      ],\n" +
          "      \"text\":\"OPV\"\n" +
          "   },\n" +
          "   \"reasonReference\":{\n" +
          "      \"reference\":\"Questionnaire/9b1aa23b-577c-4fb2-84e3-591e6facaf82\"\n" +
          "   },\n" +
          "   \"input\":[\n" +
          "      {\n" +
          "         \"type\":{\n" +
          "            \"coding\":[\n" +
          "               {\n" +
          "                  \"system\":\"http://snomed.info/sct\",\n" +
          "                  \"code\":\"900000000000457003\",\n" +
          "                  \"display\":\"Reference set attribute (foundation metadata concept)\"\n" +
          "               }\n" +
          "            ]\n" +
          "         },\n" +
          "         \"value\":{\n" +
          "            \"reference\":\"Task/650203d2-f327-4eb4-a9fd-741e0ce29c3f\"\n" +
          "         }\n" +
          "      },\n" +
          "      {\n" +
          "         \"type\":{\n" +
          "            \"coding\":[\n" +
          "               {\n" +
          "                  \"system\":\"http://snomed.info/sct\",\n" +
          "                  \"code\":\"371154000\",\n" +
          "                  \"display\":\"Dependent (qualifier value)\"\n" +
          "               }\n" +
          "            ]\n" +
          "         },\n" +
          "         \"value\":28\n" +
          "      }\n" +
          "   ],\n" +
          "   \"output\":[\n" +
          "      {\n" +
          "         \"type\":{\n" +
          "            \"coding\":[\n" +
          "               {\n" +
          "                  \"system\":\"http://snomed.info/sct\",\n" +
          "                  \"code\":\"41000179103\",\n" +
          "                  \"display\":\"Immunization record (record artifact)\"\n" +
          "               }\n" +
          "            ]\n" +
          "         },\n" +
          "         \"value\":{\n" +
          "            \"reference\":\"Encounter/14e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
          "         }\n" +
          "      }\n" +
          "   ]\n" +
          "} "
      )
  }

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

  @Test
  fun testTaskIsUpcoming() {
    task.apply {
      status = Task.TaskStatus.REQUESTED
      executionPeriod.start = today().plusDays(1)
      val expected = task.isUpcoming()
      Assert.assertTrue(expected)
    }
  }

  @Test
  fun testTaskIsOverDueWithStatusTaskStatusInProgress() {
    task.apply {
      status = Task.TaskStatus.INPROGRESS
      executionPeriod.end = today().plusDays(-1)
      val expected = task.isOverDue()
      Assert.assertTrue(expected)
    }
  }

  @Test
  fun testTaskIsOverDueWithStatusTaskStatusReady() {
    task.apply {
      status = Task.TaskStatus.READY
      executionPeriod.end = today().plusDays(-10)
      val expected = task.isOverDue()
      Assert.assertTrue(expected)
    }
  }

  @Test
  fun testTaskIsDue() {
    task.apply {
      status = Task.TaskStatus.READY
      val expected = task.isDue()
      Assert.assertTrue(expected)
    }
  }
}
