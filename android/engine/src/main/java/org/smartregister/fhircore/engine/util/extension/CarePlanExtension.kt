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

import com.google.android.fhir.logicalId
import java.util.Date
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Task

/** If no period.start specified plan is always available unless status suggests so */
fun CarePlan.started() = period?.start?.before(Date()) ?: true

/** If no period.end specified plan never ends unless status suggests so */
fun CarePlan.ended() = period?.end?.before(Date()) ?: false

fun CarePlan.due() = status.equals(CarePlan.CarePlanStatus.ACTIVE) && started() && !ended()

fun CarePlan.overdue() = status.equals(CarePlan.CarePlanStatus.ACTIVE) && ended()

fun CarePlan.milestonesDue() = this.activity.filter { it.due() }

fun CarePlan.milestonesOverdue() = this.activity.filter { it.overdue() }

/** If no scheduledPeriod.start specified activity detail is always available */
fun CarePlan.CarePlanActivityDetailComponent.started(): Boolean =
  scheduledPeriod?.start?.before(Date()) ?: true

/** If no scheduledPeriod.end specified activity detail never ends */
fun CarePlan.CarePlanActivityDetailComponent.ended(): Boolean =
  scheduledPeriod?.end?.before(Date()) ?: false

fun CarePlan.CarePlanActivityComponent.due() =
  if (!hasDetail()) true
  else
    detail.status.isIn(
      CarePlan.CarePlanActivityStatus.SCHEDULED,
      CarePlan.CarePlanActivityStatus.NOTSTARTED
    ) && detail.started() && !detail.ended()

fun CarePlan.CarePlanActivityComponent.overdue() =
  if (!hasDetail()) false
  else
    detail.status.isIn(
      CarePlan.CarePlanActivityStatus.SCHEDULED,
      CarePlan.CarePlanActivityStatus.NOTSTARTED
    ) && detail.ended()

fun CarePlan.CarePlanStatus.toCoding() = Coding(this.system, this.toCode(), this.display)

fun CarePlan.isLastTask(task: Task) =
  this.activity.last()?.outcomeReference?.last()?.extractId() == task.logicalId
