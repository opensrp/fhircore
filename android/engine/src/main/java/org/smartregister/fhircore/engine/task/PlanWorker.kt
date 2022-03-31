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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.hasPastEnd
import org.smartregister.fhircore.engine.util.extension.hasStarted
import org.smartregister.fhircore.engine.util.extension.isLastTask
import timber.log.Timber

class PlanWorker(val appContext: Context, workerParams: WorkerParameters) :
  CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    Timber.i("Starting task scheduler")

    val fhirEngine = FhirEngineProvider.getInstance(appContext)

    // TODO also filter by date range for better performance
    fhirEngine
      .search<Task> {
        filter(
          Task.STATUS,
          { value = of(Task.TaskStatus.REQUESTED.toCode()) },
          { value = of(Task.TaskStatus.READY.toCode()) },
          { value = of(Task.TaskStatus.ACCEPTED.toCode()) },
          { value = of(Task.TaskStatus.INPROGRESS.toCode()) },
          { value = of(Task.TaskStatus.RECEIVED.toCode()) },
        )
      }
      .forEach { tsk ->
        if (tsk.hasPastEnd()) {
          tsk.status = Task.TaskStatus.FAILED

          fhirEngine.save(tsk)

          tsk.reasonReference.extractId().takeIf { it.isNotBlank() }?.let {
            val carePlan = fhirEngine.load(CarePlan::class.java, it)

            if (carePlan.isLastTask(tsk)) {
              carePlan.status = CarePlan.CarePlanStatus.COMPLETED
              fhirEngine.save(carePlan)
            }
          }
        } else if (tsk.hasStarted()) {
          tsk.status = Task.TaskStatus.READY
          fhirEngine.save(tsk)
        }
      }

    Timber.i("Done task scheduling")

    return Result.success()
  }

  companion object {
    const val WORK_ID = "PlanWorker"
  }
}
