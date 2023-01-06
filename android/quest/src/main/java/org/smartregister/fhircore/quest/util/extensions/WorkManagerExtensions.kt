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

package org.smartregister.fhircore.quest.util.extensions

import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedule a periodic job that retry exponentially with initial backoff delay of 30 seconds */
inline fun <reified W : ListenableWorker> WorkManager.schedulePeriodically(
  workId: String,
  repeatInterval: Long = 15,
  timeUnit: TimeUnit = TimeUnit.MINUTES,
  existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE
) {
  enqueueUniquePeriodicWork(
    workId,
    existingPeriodicWorkPolicy,
    PeriodicWorkRequestBuilder<W>(repeatInterval, timeUnit)
      .setInitialDelay(repeatInterval, timeUnit)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .build()
  )
}
