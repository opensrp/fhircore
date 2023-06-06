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
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.isReady
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.toCoding
import timber.log.Timber

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
      Timber.i("Running Task status updater worker")

      val tasks =
        fhirEngine.search<Task> {
          filter(
            Task.STATUS,
            { value = of(Task.TaskStatus.REQUESTED.toCoding()) },
            // { value = of(Task.TaskStatus.READY.toCoding()) }, this would be handled by expiry job
            // { value = of(Task.TaskStatus.INPROGRESS.toCoding()) },
            { value = of(Task.TaskStatus.ACCEPTED.toCoding()) },
            { value = of(Task.TaskStatus.RECEIVED.toCoding()) },
          )
          filter(
            Task.PERIOD,
            {
              prefix = ParamPrefixEnum.STARTS_AFTER
              value = of(DateTimeType(Date().plusDays(-1)))
            }
          )
        }

      Timber.i("Found ${tasks.size} tasks to be updated")

      tasks.forEach { task ->
        if (task.isReady() && task.status == TaskStatus.REQUESTED && task.preReqConditionSatisfied()
        ) {
          Timber.i("${task.id} marked ready")

          task.status = Task.TaskStatus.READY
          fhirEngine.update(task)
        }
      }
      Result.success()
    }
  }

  private suspend fun Task.preReqConditionSatisfied() =
    this.partOf.find { it.reference.startsWith(ResourceType.Task.name + "/") }?.let {
      fhirEngine
        .get<Task>(it.extractId())
        .status
        .isIn(
          TaskStatus.CANCELLED,
          TaskStatus.COMPLETED,
          TaskStatus.FAILED,
          TaskStatus.ENTEREDINERROR
        )
    }
      ?: true

  companion object {
    const val WORK_ID = "FhirTaskPlanWorker"
  }
}
