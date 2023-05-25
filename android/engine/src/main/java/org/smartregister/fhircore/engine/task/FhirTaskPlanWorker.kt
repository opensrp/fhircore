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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.hasPastEnd
import org.smartregister.fhircore.engine.util.extension.isReady
import org.smartregister.fhircore.engine.util.extension.lastOffset
import org.smartregister.fhircore.engine.util.extension.toCoding
import org.smartregister.fhircore.engine.util.getLastOffset

/** This job runs periodically to update the statuses of Task resources. */
@HiltWorker
class FhirTaskPlanWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    return withContext(dispatcherProvider.io()) {
      val appConfig =
        configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
          ConfigType.Application
        )
      val batchSize = appConfig.taskBackgroundWorkerBatchSize
      val lastOffset =
        sharedPreferencesHelper.read(key = WORK_ID.lastOffset(), defaultValue = "0")!!.toInt()

      val tasks =
        fhirEngine.search<Task> {
          filter(
            Task.STATUS,
            { value = of(Task.TaskStatus.REQUESTED.toCoding()) },
            { value = of(Task.TaskStatus.READY.toCoding()) },
            { value = of(Task.TaskStatus.ACCEPTED.toCoding()) },
            { value = of(Task.TaskStatus.INPROGRESS.toCoding()) },
            { value = of(Task.TaskStatus.RECEIVED.toCoding()) },
          )
          from = if (lastOffset > 0) lastOffset + 1 else 0
          count = batchSize
        }

      tasks.forEach { task ->
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

      val updatedLastOffset =
        getLastOffset(items = tasks, lastOffset = lastOffset, batchSize = batchSize)
      sharedPreferencesHelper.write(
        key = WORK_ID.lastOffset(),
        value = updatedLastOffset.toString()
      )
      Result.success()
    }
  }

  private fun CarePlan.isLastTask(task: Task) =
    this.activity.lastOrNull()?.outcomeReference?.lastOrNull()?.extractId() == task.logicalId

  companion object {
    const val WORK_ID = "FhirTaskPlanWorker"
  }
}
