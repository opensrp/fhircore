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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.util.DispatcherProvider

/** This job runs periodically to update the update upcoming Tasks statuses to due. */
@HiltWorker
class FhirTaskStatusUpdateWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirResourceUtil: FhirResourceUtil,
  val dispatcherProvider: DispatcherProvider,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    return withContext(dispatcherProvider.io()) {
      fhirResourceUtil.updateUpcomingTasksToDue()
      Result.success()
    }
  }

  companion object {
    const val WORK_ID = "FhirTaskStatusUpdateWorker"
  }
}
