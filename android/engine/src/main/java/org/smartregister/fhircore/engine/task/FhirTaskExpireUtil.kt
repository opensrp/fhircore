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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.util.extension.isPastExpiry
import org.smartregister.fhircore.engine.util.extension.toCoding
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
    val tasksResult =
      fhirEngine
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
                value = of(DateType(lastAuthoredOnDate))
              }
            )
          }
          count = tasksCount
          sort(Task.AUTHORED_ON, Order.ASCENDING)
        }
        .asSequence()
        .filter { it.isPastExpiry() }
        .toList()
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
