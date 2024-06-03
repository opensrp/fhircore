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

package org.smartregister.fhircore.engine.data.local.purger

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.joda.time.LocalTime
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

@HiltWorker
class ResourcePurgerWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParameters: WorkerParameters,
  val fhirEngine: FhirEngine,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val dispatcherProvider: DispatcherProvider,
) : CoroutineWorker(appContext, workerParameters) {

  override suspend fun doWork(): Result {
    return withContext(dispatcherProvider.singleThread()) {
      val isOneTimeSync = inputData.getBoolean(ONE_TIME_SYNC_KEY, false)
      val optimalHour = LocalTime().hourOfDay
      Timber.i("Running $NAME...")
      if ((optimalHour < 6 || optimalHour > 17) || isOneTimeSync) {
        sharedPreferencesHelper.write(
          SharedPreferenceKey.LAST_PURGE_KEY.name,
          System.currentTimeMillis(),
        )
        ResourcePurger(fhirEngine).invoke()
      }
      Result.success()
    }
  }

  companion object {
    const val NAME = "ResourcePurgerWorker"
    const val ONE_TIME_SYNC_KEY = "one_time_sync"
  }
}
