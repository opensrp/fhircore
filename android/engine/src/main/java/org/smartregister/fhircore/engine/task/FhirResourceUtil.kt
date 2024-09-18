/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.get
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.event.EventType
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.executionStartIsBeforeOrToday
import org.smartregister.fhircore.engine.util.extension.expiredConcept
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.isPastExpiry
import org.smartregister.fhircore.engine.util.extension.toCoding
import timber.log.Timber

@Singleton
class FhirResourceUtil
@Inject
constructor(
  @ApplicationContext val appContext: Context,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
) {

  /**
   * Fetches and returns tasks whose Task.status is either "requested", "ready", "accepted",
   * "in-progress" and "received" and Task.restriction.period.end is <= today. It uses the maximum .
   * The size of the tasks is between 0 to (tasksCount * 2).
   */
  suspend fun expireOverdueTasks(): List<Task> {
    val fhirEngine = defaultRepository.fhirEngine
    Timber.i("Fetch and expire overdue tasks")
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
            },
          )
        }
        .map { it.resource }
        .filter { it.isPastExpiry() }
        .also { Timber.i("Going to expire ${it.size} tasks") }
        .onEach { task ->
          task.status = TaskStatus.CANCELLED
          task.statusReason = expiredConcept()

          task.basedOn
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
                    // close related resources
                    closeRelatedResources(carePlan)
                  }
                }
                .onFailure {
                  Timber.e(
                    "$basedOn CarePlan was not found. In consistent data ${it.message}",
                  )
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

  /**
   * This function updates upcoming [Task] s (with statuses [TaskStatus.REQUESTED],
   * [TaskStatus.ACCEPTED] or [TaskStatus.RECEIVED]) to due (updates the status to
   * [TaskStatus.READY]). If the [Task] is dependent on another [Task] and it's parent [TaskStatus]
   * is NOT [TaskStatus.COMPLETED], the dependent [Task] status will not be updated to
   * [TaskStatus.READY]. A [Task] should only be due when the start date of the Tasks
   * [Task.executionPeriod] is before today and the status is [TaskStatus.REQUESTED] and the
   * pre-requisite [Task] s are completed.
   *
   * @param subject optional subject resources to use as an additional filter to tasks processed
   * @param taskResourcesToFilterBy optional set of Task resources whose resource ids will be used
   *   to filter tasks processed
   */
  suspend fun updateUpcomingTasksToDue(
    subject: Reference? = null,
    taskResourcesToFilterBy: List<Task>? = null,
  ) {
    Timber.i("Update upcoming Tasks to due...")

    val tasks =
      defaultRepository.fhirEngine
        .search<Task> {
          filter(
            Task.STATUS,
            { value = of(TaskStatus.REQUESTED.toCoding()) },
            { value = of(TaskStatus.ACCEPTED.toCoding()) },
            { value = of(TaskStatus.RECEIVED.toCoding()) },
          )
          filter(
            Task.PERIOD,
            {
              prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
              value = of(DateTimeType(Date()))
            },
          )
          if (!subject?.reference.isNullOrEmpty()) {
            filter(Task.SUBJECT, { value = subject?.reference })
          }
          taskResourcesToFilterBy?.let {
            val filters =
              it.map {
                val apply: TokenParamFilterCriterion.() -> Unit = { value = of(it.logicalId) }
                apply
              }
            if (filters.isNotEmpty()) {
              filter(Resource.RES_ID, *filters.toTypedArray())
            }
          }
        }
        .map { it.resource }

    Timber.i(
      "Found ${tasks.size} upcoming Tasks (with statuses REQUESTED, ACCEPTED or RECEIVED) to be updated",
    )

    tasks.forEach { task ->
      val previousStatus = task.status
      if (task.executionStartIsBeforeOrToday() && task.status == TaskStatus.REQUESTED) {
        task.status = TaskStatus.READY
      }

      if (task.hasPartOf() && !task.preRequisiteConditionSatisfied()) task.status = previousStatus

      if (task.status != previousStatus) {
        defaultRepository.update(task)
        Timber.d("Task with ID '${task.id}' status updated FROM $previousStatus TO ${task.status}")
      }
    }
  }

  /**
   * Check if current [Task] is part of another [Task] then return true if the [Task.TaskStatus] of
   * the parent [Task](that the current [Task] is part of) is [Task.TaskStatus.COMPLETED] or
   * [Task.TaskStatus.FAILED], otherwise return false.
   */
  private suspend fun Task.preRequisiteConditionSatisfied() =
    this.partOf
      .find { it.reference.startsWith(ResourceType.Task.name + "/") }
      ?.let {
        defaultRepository.fhirEngine
          .get<Task>(it.extractId())
          .status
          .isIn(TaskStatus.COMPLETED, TaskStatus.FAILED)
      } ?: false

  suspend fun closeRelatedResources(resource: Resource) {
    val appRegistry =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
        ConfigType.Application,
      )

    appRegistry.eventWorkflows
      .filter { it.eventType == EventType.RESOURCE_CLOSURE }
      .forEach { eventWorkFlow ->
        eventWorkFlow.eventResources.forEach { eventResource ->
          defaultRepository.updateResourcesRecursively(eventResource, resource, eventWorkFlow)
        }
      }
  }

  /**
   * This function fetches completed service requests from the local database. For each of the
   * service requests functionality for closing related resources is triggered The related resources
   * are configured in the application config file.
   */
  suspend fun closeResourcesRelatedToCompletedServiceRequests() {
    Timber.i("Fetch completed service requests and close related resources")
    defaultRepository.fhirEngine
      .search<ServiceRequest> {
        filter(
          ServiceRequest.STATUS,
          { value = of(ServiceRequest.ServiceRequestStatus.COMPLETED.toCode()) },
        )
      }
      .map { it.resource }
      .also { Timber.i("Handling ${it.size} completed service Requests") }
      .onEach { serviceRequest ->
        // close related resources
        closeRelatedResources(serviceRequest)
      }
  }

  suspend fun closeFhirResources() {
    val appRegistry =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
        ConfigType.Application,
      )

    appRegistry.eventWorkflows
      .filter { it.eventType == EventType.RESOURCE_CLOSURE }
      .forEach { eventWorkFlow ->
        eventWorkFlow.eventResources.forEach { eventResource ->
          defaultRepository.updateResourcesRecursively(
            resourceConfig = eventResource,
            eventWorkflow = eventWorkFlow,
          )
        }
      }
  }
}
