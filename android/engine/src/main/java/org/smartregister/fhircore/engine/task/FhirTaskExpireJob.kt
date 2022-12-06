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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit
import org.smartregister.fhircore.engine.di.FhirTaskExpireEntryPoint
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class FhirTaskExpireJob constructor(val context: Context, workerParams: WorkerParameters) :
  CoroutineWorker(context, workerParams) {

  var fhirEngine: FhirEngine = getFhirEngine(context)
  var fhirTaskExpireUtil: FhirTaskExpireUtil = getFhirTaskExpireUtil(context)

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

    const val FHIR_TASK_EXPIRE_JOB_VERSION = "fhir-task-expire-job-version"
    const val TAG = "FhirTaskExpire"

    fun schedule(
      context: Context,
      sharedPreferencesHelper: SharedPreferencesHelper,
      durationInMins: Long,
      version: Long = 1
    ) {
      val currVersion = sharedPreferencesHelper.read(FHIR_TASK_EXPIRE_JOB_VERSION, 0)
      var existingWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
      if (currVersion != version) {
        existingWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE
        sharedPreferencesHelper.write(FHIR_TASK_EXPIRE_JOB_VERSION, version)
      }

      val periodicWorkRequest =
        PeriodicWorkRequestBuilder<FhirTaskExpireJob>(
            durationInMins,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
          )
          .setInitialDelay(durationInMins, TimeUnit.MINUTES)
          .build()

      WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(TAG, existingWorkPolicy, periodicWorkRequest)
    }
  }

  private fun hiltEntryPoint(appContext: Context) =
    EntryPointAccessors.fromApplication(appContext, FhirTaskExpireEntryPoint::class.java)

  private fun getFhirEngine(appContext: Context): FhirEngine =
    hiltEntryPoint(appContext).fhirEngine()

  private fun getFhirTaskExpireUtil(appContext: Context): FhirTaskExpireUtil =
    hiltEntryPoint(appContext).fhirTaskExpireUtil()
}
