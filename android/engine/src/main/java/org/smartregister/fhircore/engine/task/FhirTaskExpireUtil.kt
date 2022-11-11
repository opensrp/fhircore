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

package org.smartregister.fhircore.engine.task

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.isPastExpiry
import org.smartregister.fhircore.engine.util.extension.toCoding
import timber.log.Timber

class FhirTaskExpireUtil(val appContext: Context) {


  internal fun getFhirEngine() : FhirEngine {
    return FhirEngineProvider.getInstance(appContext)
  }


  suspend fun fetchOverdueTasks() : List<Task> {
    Timber.i("Start fetching overdue tasks")

    val fhirEngine = getFhirEngine()

    // TODO: Limit & page the results for better performance
    val tasksPastExpiry = fhirEngine
      .search<Task> {
        filter(
          Task.STATUS,
          { value = of(Task.TaskStatus.REQUESTED.toCoding()) },
          { value = of(Task.TaskStatus.READY.toCoding()) },
          { value = of(Task.TaskStatus.ACCEPTED.toCoding()) },
          { value = of(Task.TaskStatus.INPROGRESS.toCoding()) },
          { value = of(Task.TaskStatus.RECEIVED.toCoding()) },
        )
        //filterBy(DataQuery())
        //count = size
      }
      .filter { it.isPastExpiry() }

    Timber.i("Done fetching overdue tasks")
    return tasksPastExpiry
  }

  suspend fun markTaskExpired(tasks: List<Task>) {

    val fhirEngine = getFhirEngine()

    // This allows the Fhir Engine to index the values
    tasks.forEach { task ->
      task.setStatus(Task.TaskStatus.CANCELLED)

      fhirEngine.update(task)
    }

    Timber.i("${tasks.size} tasks updated to cancelled")
  }

  companion object {
    const val WORK_ID = "PlanWorker"
  }
}
