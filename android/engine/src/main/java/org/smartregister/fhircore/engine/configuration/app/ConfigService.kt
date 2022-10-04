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

package org.smartregister.fhircore.engine.configuration.app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.smartregister.fhircore.engine.sync.SyncStrategy
import org.smartregister.fhircore.engine.task.FhirTaskPlanWorker

/** An interface that provides the application configurations. */
interface ConfigService {

  /** Provide [AuthConfiguration] for the application */
  fun provideAuthConfiguration(): AuthConfiguration

  /** Provide [SyncStrategy] for the application */
  fun provideSyncStrategy(): SyncStrategy

  fun scheduleFhirTaskPlanWorker(context: Context) {
    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(
        FhirTaskPlanWorker.WORK_ID,
        ExistingPeriodicWorkPolicy.REPLACE,
        PeriodicWorkRequestBuilder<FhirTaskPlanWorker>(12, TimeUnit.HOURS).build()
      )
  }
}
