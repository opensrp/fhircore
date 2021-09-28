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

fun CarePlan.started() = period.start.before(Date())

fun CarePlan.ended() = period.end.before(Date())

fun CarePlan.due() = status.equals(CarePlan.CarePlanStatus.ACTIVE) && started() && !ended()

fun CarePlan.overdue() = status.equals(CarePlan.CarePlanStatus.ACTIVE) && ended()

fun CarePlan.CarePlanActivityDetailComponent.started(): Boolean =
  scheduledPeriod.start?.before(Date()) == true

fun CarePlan.CarePlanActivityDetailComponent.ended(): Boolean =
  scheduledPeriod.end?.before(Date()) == true

fun CarePlan.CarePlanActivityDetailComponent.due() =
  status.isIn(
    CarePlan.CarePlanActivityStatus.SCHEDULED,
    CarePlan.CarePlanActivityStatus.NOTSTARTED
  ) && started() && !ended()

fun CarePlan.CarePlanActivityDetailComponent.overdue() =
  status.isIn(
    CarePlan.CarePlanActivityStatus.SCHEDULED,
    CarePlan.CarePlanActivityStatus.NOTSTARTED
  ) && ended()
