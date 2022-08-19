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

import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.util.DateUtils
import org.smartregister.fhircore.engine.util.DateUtils.isToday
import org.smartregister.fhircore.engine.util.DateUtils.today

const val ACTIVE_ANC_REGEX = "^(.*[\\s-.])?(ANC|Pregnan[tcy]{1,2})([\\s-.].*)?$"

fun Task.hasPastEnd() =
  this.hasExecutionPeriod() &&
    this.executionPeriod.hasEnd() &&
    this.executionPeriod.end.before(DateUtils.yesterday())

fun Task.hasStarted() =
  this.hasExecutionPeriod() &&
    this.executionPeriod.hasStart() &&
    with(this.executionPeriod.start) { this.before(today()) || this.isToday() }

fun Task.TaskStatus.toCoding() = Coding(this.system, this.toCode(), this.display)

fun Task.isActiveAnc() =
  this.status.isIn(Task.TaskStatus.READY, Task.TaskStatus.REQUESTED, Task.TaskStatus.INPROGRESS) &&
    this.description.matches(Regex(ACTIVE_ANC_REGEX))
