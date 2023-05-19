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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.SearchQuery
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.isPastExpiry
import org.smartregister.fhircore.engine.util.extension.parseDate
import timber.log.Timber

@Singleton
class FhirTaskExpireUtil
@Inject
constructor(@ApplicationContext val appContext: Context, val fhirEngine: FhirEngine) {

  /**
   * Fetches and returns tasks whose Task.status is either "requested", "ready", "accepted",
   * "in-progress" and "received" and Task.restriction.period.end is <= today. It uses the maximum .
   * The size of the tasks is between 0 to (tasksCount * 2). It is not guaranteed that the list of
   * tasks returned will be of size [tasksCount].
   */
  suspend fun expireOverdueTasks(
    lastAuthoredOnDate: Date?,
    tasksCount: Int = 40
  ): Pair<Date?, List<Task>> {
    Timber.i("Fetch and expire overdue tasks")
    val currentTime = System.currentTimeMillis()
    // val tasksResult =
    /* val tasks = fhirEngine
    .search<Task> {
      filter(
        Task.STATUS,
        { value = of(Task.TaskStatus.REQUESTED.toCoding()) },
        { value = of(Task.TaskStatus.READY.toCoding()) },
        { value = of(Task.TaskStatus.ACCEPTED.toCoding()) },
        { value = of(Task.TaskStatus.INPROGRESS.toCoding()) },
        { value = of(Task.TaskStatus.RECEIVED.toCoding()) },
      )
      if (lastAuthoredOnDate != null) {
        filter(
          Task.AUTHORED_ON,
          {
            prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS
            value = of(DateTimeType(lastAuthoredOnDate))
          }
        )
      }
      count = tasksCount
      sort(Task.AUTHORED_ON, Order.ASCENDING)
    }*/

    var lastAuthoredOnDateLocal: Date =
      lastAuthoredOnDate ?: "1970-01-01".parseDate(SDF_YYYY_MM_DD)!!

    val searchQuery =
      SearchQuery(
        """
            SELECT a.serializedResource
            FROM ResourceEntity a
           -- LEFT JOIN DateIndexEntity b
           -- ON a.resourceType = b.resourceType AND a.resourceUuid = b.resourceUuid AND b.index_name = 'authored-on'
            LEFT JOIN DateTimeIndexEntity c
            ON a.resourceType = c.resourceType AND a.resourceUuid = c.resourceUuid --AND c.index_name = 'authored-on'
            WHERE a.resourceType = 'Task'
            AND a.resourceUuid IN (
            SELECT resourceUuid FROM DateTimeIndexEntity
            WHERE resourceType = 'Task' AND index_name = 'authored-on' AND index_to >= ?
            )
            AND a.resourceUuid IN (
            SELECT resourceUuid FROM TokenIndexEntity
            WHERE resourceType = 'Task' AND index_name = 'status' 
            AND ((index_value = 'requested' AND IFNULL(index_system,'') = 'http://hl7.org/fhir/task-status') 
            OR (index_value = 'ready' AND IFNULL(index_system,'') = 'http://hl7.org/fhir/task-status') OR (index_value = 'accepted' AND IFNULL(index_system,'') = 'http://hl7.org/fhir/task-status')
             OR (index_value = 'in-progress' AND IFNULL(index_system,'') = 'http://hl7.org/fhir/task-status') OR (index_value = 'received' AND IFNULL(index_system,'') = 'http://hl7.org/fhir/task-status'))
            )
            ORDER BY c.index_from ASC
            LIMIT ?
        """.trimIndent(),
        listOf(lastAuthoredOnDateLocal.time, tasksCount)
      )

    val tasks = fhirEngine.search<Task>(searchQuery)
    Timber.e("${FhirTaskExpireWorker.WORK_ID} tasks fetched is ${tasks.size}")

    val tasksResult =
      tasks
        .filter {
          Timber.w(
            "${FhirTaskExpireWorker.WORK_ID} job task search executed in ${(System.currentTimeMillis() - currentTime) / 1000} second(s) on thread ${Thread.currentThread().name}"
          )
          it.isPastExpiry()
        }
        .onEach { task ->
          task.status = Task.TaskStatus.CANCELLED
          fhirEngine.update(task)
        }

    // Tasks are ordered obtain the authoredOn date of the last
    val maxDate = tasksResult.lastOrNull()?.authoredOn

    Timber.i("${tasksResult.size} FHIR Tasks status updated to CANCELLED (expired)")
    return Pair(maxDate, tasksResult)
  }
}
