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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.hasPastEnd
import org.smartregister.fhircore.engine.util.extension.isReady
import org.smartregister.fhircore.engine.util.extension.toCoding
import timber.log.Timber

@HiltWorker
class FhirTaskPlanWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    Timber.i("Starting task scheduler")

    // TODO also filter by date range for better performance
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
      }
      .forEach { task ->
        if (task.hasPastEnd()) {
          task.status = Task.TaskStatus.FAILED
          fhirEngine.update(task)
          task
            .basedOn
            .find { it.reference.startsWith(ResourceType.CarePlan.name) }
            ?.extractId()
            ?.takeIf { it.isNotBlank() }
            ?.let {
              val carePlan = fhirEngine.get<CarePlan>(it)
              if (carePlan.isLastTask(task)) {
                carePlan.status = CarePlan.CarePlanStatus.COMPLETED
                fhirEngine.update(carePlan)
              }
            }
        } else if (task.isReady() && task.status == Task.TaskStatus.REQUESTED) {
          task.status = Task.TaskStatus.READY
          fhirEngine.update(task)
        }
      }

    Timber.i("Done task scheduling")
    return Result.success()
  }

  private fun CarePlan.isLastTask(task: Task) =
    this.activity.last()?.outcomeReference?.last()?.extractId() == task.logicalId

  companion object {
    const val WORK_ID = "FhirTaskPlanWorker"
  }
}
