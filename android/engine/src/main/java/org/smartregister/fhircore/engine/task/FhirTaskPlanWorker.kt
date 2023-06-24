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
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.executionStartIsBeforeOrToday
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.isIn
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
  val defaultRepository: DefaultRepository,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val fhirEngine = defaultRepository.fhirEngine
    return withContext(dispatcherProvider.io()) {
      Timber.i("Running Task status updater worker")

      val tasks =
        fhirEngine.search<Task> {
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
              value = of(DateTimeType(Date().plusDays(-1)))
            }
          )
        }

      Timber.i("Found ${tasks.size} tasks to be updated")

      tasks.forEach { task ->
        // expired tasks are handled by other service i.e. FhirTaskExpireWorker
        if (task.executionStartIsBeforeOrToday() &&
            task.status == TaskStatus.REQUESTED &&
            task.preReqConditionSatisfied()
        ) {
          Timber.i("Task ${task.id} marked ready")

          task.status = TaskStatus.READY
          defaultRepository.update(task)
        }
      }
      Result.success()
    }
  }

  /**
   * Check in the task is part of another task and if so check if the parent task is
   * completed,cancelled,failed or entered in error.
   */
  private suspend fun Task.preReqConditionSatisfied() =
    this.partOf.find { it.reference.startsWith(ResourceType.Task.name + "/") }?.let {
      defaultRepository
        .fhirEngine
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
