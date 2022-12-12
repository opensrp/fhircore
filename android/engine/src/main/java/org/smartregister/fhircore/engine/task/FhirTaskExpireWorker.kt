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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltWorker
class FhirTaskExpireWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine,
  val fhirTaskExpireUtil: FhirTaskExpireUtil
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {
    var dateTasks = fhirTaskExpireUtil.fetchOverdueTasks()
    var maxDate = dateTasks.first
    var tasks = dateTasks.second

    while (tasks.size > 0) {
      fhirTaskExpireUtil.markTaskExpired(tasks)
      dateTasks = fhirTaskExpireUtil.fetchOverdueTasks(from = maxDate)
      maxDate = dateTasks.first
      tasks = dateTasks.second
    }

    return Result.success()
  }

  companion object {
    const val TAG = "FhirTaskExpire"

    fun schedule(
      workManager: WorkManager,
      sharedPreferencesHelper: SharedPreferencesHelper,
      durationInMins: Long,
      version: Long = 1
    ) {
      val currVersion =
        sharedPreferencesHelper.read(SharedPreferenceKey.FHIR_TASK_EXPIRE_WORKER_VERSION.name, 0)
      var existingWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
      if (currVersion != version) {
        existingWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE
        sharedPreferencesHelper.write(
          SharedPreferenceKey.FHIR_TASK_EXPIRE_WORKER_VERSION.name,
          version
        )
      }

      val periodicWorkRequest =
        PeriodicWorkRequestBuilder<FhirTaskExpireWorker>(
            repeatInterval = durationInMins,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5,
            flexTimeIntervalUnit = TimeUnit.MINUTES
          )
          .setInitialDelay(durationInMins, TimeUnit.MINUTES)
          .build()

      workManager.enqueueUniquePeriodicWork(TAG, existingWorkPolicy, periodicWorkRequest)
    }
  }
}
