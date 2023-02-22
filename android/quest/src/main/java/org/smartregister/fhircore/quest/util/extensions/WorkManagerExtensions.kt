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

package org.smartregister.fhircore.quest.util.extensions

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.util.concurrent.TimeUnit

/** Schedule a periodic job that retry exponentially with initial backoff delay of 30 seconds */
inline fun <reified W : ListenableWorker> WorkManager.schedulePeriodically(
  workId: String,
  repeatInterval: Long = 15,
  duration: Duration? = null,
  timeUnit: TimeUnit = TimeUnit.MINUTES,
  existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
  requiresNetwork: Boolean = true
) {

  val constraint =
    Constraints.Builder()
      .setRequiredNetworkType(
        if (requiresNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
      )
      .build()

  val workRequestBuilder =
    if (duration == null) PeriodicWorkRequestBuilder<W>(repeatInterval, timeUnit)
    else PeriodicWorkRequestBuilder<W>(duration)
  enqueueUniquePeriodicWork(
    workId,
    existingPeriodicWorkPolicy,
    workRequestBuilder
      .setInitialDelay(repeatInterval, timeUnit)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .setConstraints(constraint)
      .build()
  )
}
