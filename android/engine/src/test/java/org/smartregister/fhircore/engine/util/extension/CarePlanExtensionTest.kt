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

import java.util.Date
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarePlanExtensionTest {

  @Test
  fun testCarePlanActivityComponent_Due_ShouldReturn_True_For_Active_WhenPeriod_NotSpecified() {
    val noPeriodScheduled =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED
      )
    val noPeriodNotStarted =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.NOTSTARTED
      )
    val startDatePassedWithScheduled =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED,
        activityPeriod = Period().apply { start = pastDate(1) }
      )
    val startDatePassedWithNotStarted =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.NOTSTARTED,
        activityPeriod = Period().apply { start = pastDate(1) }
      )

    assertTrue(noPeriodScheduled.activityFirstRep.due())
    assertTrue(noPeriodNotStarted.activityFirstRep.due())
    assertTrue(startDatePassedWithScheduled.activityFirstRep.due())
    assertTrue(startDatePassedWithNotStarted.activityFirstRep.due())
  }

  @Test
  fun testCarePlanActivityComponent_Due_ShouldReturn_False_For_NonActive_WithPeriod_InRange() {
    val inprogress =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.INPROGRESS,
        activityPeriod = Period().apply { start = pastDate(1) }
      )
    val cancelled =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.CANCELLED
      )
    val completed =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.COMPLETED,
        activityPeriod = Period().apply { start = pastDate(1) }
      )
    val onhold =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.ONHOLD,
        activityPeriod = Period().apply { start = pastDate(1) }
      )
    val stopped =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.STOPPED,
        activityPeriod = Period().apply { start = pastDate(1) }
      )

    assertFalse(inprogress.activityFirstRep.due())
    assertFalse(completed.activityFirstRep.due())
    assertFalse(cancelled.activityFirstRep.due())
    assertFalse(onhold.activityFirstRep.due())
    assertFalse(stopped.activityFirstRep.due())
  }

  @Test
  fun testCarePlanActivityComponent_Due_ShouldReturn_True_For_Active_WithPeriod_InRange() {
    val inRange =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED,
        activityPeriod =
          Period().apply {
            start = pastDate(1)
            end = futureDate(1)
          }
      )

    assertTrue(inRange.activityFirstRep.due())
  }

  @Test
  fun testCarePlanActivityComponent_Due_ShouldReturn_False_For_Active_WithPeriod_Passed() {
    val inRange =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED,
        activityPeriod =
          Period().apply {
            start = pastDate(2)
            end = pastDate(1)
          }
      )

    assertFalse(inRange.activityFirstRep.due())
  }

  @Test
  fun testCarePlanActivityComponent_Due_ShouldReturn_False_For_Active_WithPeriod_InFuture() {
    val inRange =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED,
        activityPeriod =
          Period().apply {
            start = futureDate(1)
            end = futureDate(2)
          }
      )

    assertFalse(inRange.activityFirstRep.due())
  }

  @Test
  fun testCarePlanActivityComponent_Overdue_ShouldReturn_False_For_NonActive_WithPeriod_InRange() {
    val completed =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.COMPLETED,
        activityPeriod =
          Period().apply {
            start = pastDate(1)
            end = futureDate(1)
          }
      )

    assertFalse(completed.activityFirstRep.overdue())
  }

  @Test
  fun testCarePlanActivityComponent_Overdue_ShouldReturn_False_For_Active_WithPeriod_NotSpecified() {
    val scheduled =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED
      )

    assertFalse(scheduled.activityFirstRep.overdue())
  }

  @Test
  fun testCarePlanActivityComponent_Overdue_ShouldReturn_True_For_Active_WithPeriod_InRange() {
    val scheduled =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED,
        activityPeriod =
          Period().apply {
            start = pastDate(1)
            end = futureDate(1)
          }
      )

    assertFalse(scheduled.activityFirstRep.overdue())
  }

  @Test
  fun testCarePlanActivityComponent_Overdue_ShouldReturn_True_For_Active_WithPeriod_Passed() {
    val scheduled =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED,
        activityPeriod =
          Period().apply {
            start = pastDate(2)
            end = pastDate(1)
          }
      )

    assertTrue(scheduled.activityFirstRep.overdue())
  }

  @Test
  fun testCarePlanActivityComponent_Overdue_ShouldReturn_False_For_Active_WithPeriod_InFuture() {
    val scheduled =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        activityStatus = CarePlan.CarePlanActivityStatus.SCHEDULED,
        activityPeriod =
          Period().apply {
            start = futureDate(1)
            end = futureDate(2)
          }
      )

    assertFalse(scheduled.activityFirstRep.overdue())
  }

  @Test
  fun testCarePlan_Due_ShouldReturn_True_For_Active_WhenPeriod_NotSpecified() {
    val noPeriod = buildCarePlan(CarePlan.CarePlanStatus.ACTIVE)
    val started =
      buildCarePlan(CarePlan.CarePlanStatus.ACTIVE, Period().apply { start = pastDate(1) })

    assertTrue(noPeriod.due())
    assertTrue(started.due())
  }

  @Test
  fun testCarePlan_Due_ShouldReturn_True_For_Active_WithPeriod_InRange() {
    val carePlan =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        careplanPeriod =
          Period().apply {
            start = pastDate(1)
            end = futureDate(1)
          }
      )

    assertTrue(carePlan.due())
  }

  @Test
  fun testCarePlan_Due_ShouldReturn_False_For_NonActive_WithPeriod_InRange() {
    val completed = buildCarePlan(careplanStatus = CarePlan.CarePlanStatus.COMPLETED)
    val revoked =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.REVOKED,
        careplanPeriod = Period().apply { start = pastDate(1) }
      )
    val onhold =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ONHOLD,
        careplanPeriod =
          Period().apply {
            start = pastDate(1)
            end = futureDate(1)
          }
      )

    assertFalse(completed.due())
    assertFalse(revoked.due())
    assertFalse(onhold.due())
  }

  @Test
  fun testCarePlan_Due_ShouldReturn_False_For_Active_WithPeriod_Passed() {
    val passed =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        careplanPeriod =
          Period().apply {
            start = pastDate(2)
            end = pastDate(1)
          }
      )

    assertFalse(passed.due())
  }

  @Test
  fun testCarePlan_Due_ShouldReturn_False_For_Active_WithPeriod_InFuture() {
    val future =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        careplanPeriod =
          Period().apply {
            start = futureDate(1)
            end = futureDate(2)
          }
      )

    assertFalse(future.due())
  }

  @Test
  fun testCarePlan_Overdue_ShouldReturn_False_For_NonActive() {
    val completed = buildCarePlan(careplanStatus = CarePlan.CarePlanStatus.COMPLETED)
    val revoked =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.REVOKED,
        careplanPeriod = Period().apply { pastDate(1) }
      )
    val onhold =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ONHOLD,
        careplanPeriod =
          Period().apply {
            start = pastDate(2)
            end = pastDate(1)
          }
      )

    assertFalse(completed.overdue())
    assertFalse(revoked.overdue())
    assertFalse(onhold.overdue())
  }

  @Test
  fun testCarePlan_Overdue_ShouldReturn_False_For_Active_WithPeriod_InRange() {
    val noPeriod = buildCarePlan(careplanStatus = CarePlan.CarePlanStatus.ACTIVE)
    val started =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        careplanPeriod = Period().apply { start = pastDate(1) }
      )
    val inRange =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        careplanPeriod =
          Period().apply {
            start = pastDate(1)
            end = futureDate(1)
          }
      )

    assertFalse(noPeriod.overdue())
    assertFalse(started.overdue())
    assertFalse(inRange.overdue())
  }

  @Test
  fun testCarePlan_Overdue_ShouldReturn_True_For_Active_WithPeriod_Passed() {
    val passed =
      buildCarePlan(
        careplanStatus = CarePlan.CarePlanStatus.ACTIVE,
        careplanPeriod =
          Period().apply {
            start = pastDate(2)
            end = pastDate(1)
          }
      )

    assertTrue(passed.overdue())
  }

  @Test
  fun testCarePlan_Started_ShouldReturn_True_When_PeriodStartNotSpecified() {
    val noPeriodStart =
      buildCarePlan(CarePlan.CarePlanStatus.ACTIVE, Period().apply { end = futureDate(2) })
    assertTrue(noPeriodStart.started())
  }

  @Test
  fun testCarePlan_MilestonesDue_ShouldReturn_CorrectData() {
    val due1 =
      CarePlan.CarePlanActivityComponent().apply {
        this.detail.status = CarePlan.CarePlanActivityStatus.SCHEDULED
      }
    val due2 =
      CarePlan.CarePlanActivityComponent().apply {
        this.detail.status = CarePlan.CarePlanActivityStatus.SCHEDULED
        this.detail.scheduled = datePeriod(pastDate(1), futureDate(1))
      }
    val passed =
      CarePlan.CarePlanActivityComponent().apply {
        this.detail.status = CarePlan.CarePlanActivityStatus.SCHEDULED
        this.detail.scheduled = datePeriod(pastDate(2), pastDate(1))
      }
    val future =
      CarePlan.CarePlanActivityComponent().apply {
        this.detail.status = CarePlan.CarePlanActivityStatus.SCHEDULED
        this.detail.scheduled = datePeriod(futureDate(1), futureDate(2))
      }

    val careplan =
      CarePlan().apply {
        this.status = CarePlan.CarePlanStatus.ACTIVE
        this.activity = listOf(passed, due1, due2, future)
      }

    val result = careplan.milestonesDue()

    assertEquals(2, result.size)
    assertEquals(due1, result[0])
    assertEquals(due2, result[1])
  }

  @Test
  fun testCarePlan_MilestonesOverdue_ShouldReturn_CorrectData() {
    val due1 =
      CarePlan.CarePlanActivityComponent().apply {
        this.detail.status = CarePlan.CarePlanActivityStatus.SCHEDULED
      }
    val due2 =
      CarePlan.CarePlanActivityComponent().apply {
        this.detail.status = CarePlan.CarePlanActivityStatus.SCHEDULED
        this.detail.scheduled = datePeriod(pastDate(1), futureDate(1))
      }
    val passed =
      CarePlan.CarePlanActivityComponent().apply {
        this.detail.status = CarePlan.CarePlanActivityStatus.SCHEDULED
        this.detail.scheduled = datePeriod(pastDate(2), pastDate(1))
      }
    val future =
      CarePlan.CarePlanActivityComponent().apply {
        this.detail.status = CarePlan.CarePlanActivityStatus.SCHEDULED
        this.detail.scheduled = datePeriod(futureDate(1), futureDate(2))
      }

    val careplan =
      CarePlan().apply {
        this.status = CarePlan.CarePlanStatus.ACTIVE
        this.activity = listOf(passed, due1, due2, future)
      }

    val result = careplan.milestonesOverdue()

    assertEquals(1, result.size)
    assertEquals(passed, result[0])
  }

  private fun pastDate(days: Int) = Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * days))

  private fun futureDate(days: Int) =
    Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24 * days))

  private fun datePeriod(start: Date?, end: Date?) =
    Period().apply {
      this.start = start
      this.end = end
    }

  private fun buildCarePlan(
    careplanStatus: CarePlan.CarePlanStatus,
    careplanPeriod: Period? = null,
    activityPeriod: Period? = null,
    activityStatus: CarePlan.CarePlanActivityStatus? = null
  ): CarePlan {
    return CarePlan().apply {
      this.status = careplanStatus
      this.period = careplanPeriod
      addActivity().apply {
        detail =
          CarePlan.CarePlanActivityDetailComponent().apply {
            this.status = activityStatus
            this.scheduled = activityPeriod
          }
      }
    }
  }
}
