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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.parseDate

@HiltWorker
class FhirTaskExpireWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine,
  val fhirTaskExpireUtil: FhirTaskExpireUtil,
  val sharedPreferences: SharedPreferencesHelper
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {
    val lastAuthoredOnDate =
      sharedPreferences
        .read(SharedPreferenceKey.OVERDUE_TASK_LAST_AUTHORED_ON_DATE.name, null)
        ?.parseDate(SDF_YYYY_MM_DD)

    var (maxDate, tasks) =
      fhirTaskExpireUtil.expireOverdueTasks(lastAuthoredOnDate = lastAuthoredOnDate)

    while (tasks.isNotEmpty()) {
      val resultPair = fhirTaskExpireUtil.expireOverdueTasks(lastAuthoredOnDate = maxDate)
      maxDate = resultPair.first
      tasks = resultPair.second
    }

    sharedPreferences.write(
      SharedPreferenceKey.OVERDUE_TASK_LAST_AUTHORED_ON_DATE.name,
      maxDate?.formatDate(SDF_YYYY_MM_DD)
    )
    return Result.success()
  }

  companion object {
    const val WORK_ID = "FhirTaskExpire"
  }
}
