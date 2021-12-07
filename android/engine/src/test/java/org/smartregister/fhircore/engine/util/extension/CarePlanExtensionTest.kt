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

import java.util.Calendar
import java.util.Date
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateTimeType
import org.junit.Assert.assertEquals
import org.junit.Test

class CarePlanExtensionTest {

  @Test
  fun testCarePlanActivityDetailComponentShouldReturnTrueForPastDate() {
    val startDate = DateTimeType(Date())
    startDate.add(Calendar.DATE, -3)

    val carePlan = buildCarePlan(startDate = startDate)

    val result = carePlan.activityFirstRep.detail.started()

    assertEquals(true, result)
  }

  @Test
  fun testCarePlanActivityDetailComponentShouldReturnFalseForFutureDate() {
    val endDate = DateTimeType(Date())
    endDate.add(Calendar.DATE, 3)

    val carePlan = buildCarePlan(endDate = endDate)

    val result = carePlan.activityFirstRep.detail.ended()

    assertEquals(false, result)
  }

  @Test
  fun testCarePlanActivityDetailComponentShouldReturnTrueForDueActivities() {
    val startDate = DateTimeType(Date())
    startDate.add(Calendar.DATE, -3)

    val endDate = DateTimeType(Date())
    endDate.add(Calendar.DATE, 3)

    var carePlan =
      buildCarePlan(
        startDate = startDate,
        endDate = endDate,
        CarePlan.CarePlanActivityStatus.SCHEDULED
      )

    val result = carePlan.activityFirstRep.detail.due()

    assertEquals(true, result)
  }

  @Test
  fun testCarePlanActivityDetailComponentShouldReturnTrueForOverdueActivities() {
    val startDate = DateTimeType(Date())
    startDate.add(Calendar.DATE, -9)

    val endDate = DateTimeType(Date())
    endDate.add(Calendar.DATE, -3)

    val carePlan =
      buildCarePlan(
        startDate = startDate,
        endDate = endDate,
        CarePlan.CarePlanActivityStatus.SCHEDULED
      )

    val result = carePlan.activityFirstRep.detail.overdue()

    assertEquals(true, result)
  }

  private fun buildCarePlan(
    startDate: DateTimeType? = null,
    endDate: DateTimeType? = null,
    activityStatus: CarePlan.CarePlanActivityStatus? = null
  ): CarePlan {
    return CarePlan().apply {
      addActivity().apply {
        detail =
          CarePlan.CarePlanActivityDetailComponent().apply {
            status = activityStatus
            scheduledPeriod.start = startDate?.value
            scheduledPeriod.end = endDate?.value
          }
      }
    }
  }
}
