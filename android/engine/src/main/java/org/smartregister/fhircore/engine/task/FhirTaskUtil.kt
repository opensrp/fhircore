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

import android.content.Context
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.executionStartIsBeforeOrToday
import org.smartregister.fhircore.engine.util.extension.expiredConcept
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.isPastExpiry
import org.smartregister.fhircore.engine.util.extension.toCoding
import timber.log.Timber

@Singleton
class FhirTaskUtil
@Inject
constructor(@ApplicationContext val appContext: Context, val defaultRepository: DefaultRepository) {

  /**
   * Fetches and returns tasks whose Task.status is either "requested", "ready", "accepted",
   * "in-progress" and "received" and Task.restriction.period.end is <= today. It uses the maximum .
   * The size of the tasks is between 0 to (tasksCount * 2).
   */
  suspend fun expireOverdueTasks(): List<Task> {
    Timber.i("Fetch and expire overdue tasks")
    val fhirEngine = defaultRepository.fhirEngine
    val tasksResult =
      fhirEngine
        .search<Task> {
          filter(
            Task.STATUS,
            { value = of(TaskStatus.REQUESTED.toCoding()) },
            { value = of(TaskStatus.READY.toCoding()) },
            { value = of(TaskStatus.ACCEPTED.toCoding()) },
            { value = of(TaskStatus.INPROGRESS.toCoding()) },
            { value = of(TaskStatus.RECEIVED.toCoding()) },
          )

          filter(
            Task.PERIOD,
            {
              prefix = ParamPrefixEnum.ENDS_BEFORE
              value = of(DateTimeType(Date()))
            }
          )
        }
        .filter { it.isPastExpiry() }
        .also { Timber.i("Going to expire ${it.size} tasks") }
        .onEach { task ->
          task.status = TaskStatus.CANCELLED
          task.statusReason = expiredConcept()

          task
            .basedOn
            .find { it.reference.startsWith(ResourceType.CarePlan.name) }
            ?.extractId()
            ?.takeIf { it.isNotBlank() }
            ?.let { basedOn ->
              kotlin
                .runCatching {
                  val carePlan = fhirEngine.get<CarePlan>(basedOn)
                  if (carePlan.isLastTask(task)) {
                    carePlan.status = CarePlan.CarePlanStatus.COMPLETED
                    defaultRepository.update(carePlan)
                  }
                }
                .onFailure {
                  Timber.e("$basedOn CarePlan was not found. In consistent data ${it.message}")
                }
            }

          defaultRepository.update(task)
        }

    Timber.i("${tasksResult.size} FHIR Tasks status updated to CANCELLED (expired)")
    return tasksResult
  }

  private fun CarePlan.isLastTask(task: Task) =
    this.activity
      .find { !it.hasDetail() || it.detail.kind == CarePlan.CarePlanActivityKind.TASK }
      ?.outcomeReference
      ?.lastOrNull()
      ?.extractId() == task.logicalId

  suspend fun updateTaskStatuses(tasks: List<Task>? = null) {
    Timber.i("Update tasks statuses")

    val tasks =
      tasks
        ?: defaultRepository.fhirEngine.search {
          filter(
            Task.STATUS,
            { value = of(TaskStatus.REQUESTED.toCoding()) },
            { value = of(TaskStatus.ACCEPTED.toCoding()) },
            { value = of(TaskStatus.RECEIVED.toCoding()) }
          )
          filter(
            Task.PERIOD,
            {
              prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
              value = of(DateTimeType(Date()))
            }
          )
        }

    Timber.i("Found ${tasks.size} tasks to be updated")

    tasks.forEach { task ->
      val previousStatus = task.status
      if (task.executionStartIsBeforeOrToday() && task.status == TaskStatus.REQUESTED) {
        task.status = TaskStatus.READY
      }

      if (task.hasPartOf() && !task.preRequisiteConditionSatisfied()) task.status = previousStatus

      defaultRepository.update(task)
      Timber.d("Task with ID '${task.id}' status updated to ${task.status}")
    }
  }

  /**
   * Check if current [Task] is part of another [Task] then return true if the [Task.TaskStatus] of
   * the parent [Task](that the current [Task] is part of) is [Task.TaskStatus.COMPLETED], otherwise
   * return false.
   */
  private suspend fun Task.preRequisiteConditionSatisfied() =
    this.partOf.find { it.reference.startsWith(ResourceType.Task.name + "/") }?.let {
      defaultRepository.fhirEngine.get<Task>(it.extractId()).status.isIn(TaskStatus.COMPLETED)
    }
      ?: false
}
