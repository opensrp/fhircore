/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.upload.UploadStrategy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

@HiltWorker
class AppSyncWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val syncListenerManager: SyncListenerManager,
  private val openSrpFhirEngine: FhirEngine,
  private val appTimeStampContext: AppTimeStampContext,
  private val sharedPreferencesHelper: SharedPreferencesHelper,
  private val dispatcherProvider: DispatcherProvider,
) : FhirSyncWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    return try {
      val result = super.doWork()
      result
    } catch (e: Exception) {
      Timber.w("AppSyncWorker failed")
      Result.failure()
    } finally {
      Timber.w("AppSyncWorker finished")

      CoroutineScope(dispatcherProvider.io()).launch {
        sharedPreferencesHelper.write("SYNC_WORKER_COMPLETE", "true")
        sharedPreferencesHelper.write(
          "SYNC_WORKER_COMPLETION_TIME",
          OffsetDateTime.now().toString(),
        )
      }
    }
  }

  override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver

  override fun getDownloadWorkManager(): DownloadWorkManager =
    OpenSrpDownloadManager(
      resourceSearchParams = runBlocking { syncListenerManager.loadResourceSearchParams() },
      context = appTimeStampContext,
    )

  override fun getFhirEngine(): FhirEngine = openSrpFhirEngine

  override fun getUploadStrategy(): UploadStrategy = UploadStrategy.AllChangesSquashedBundlePut
}
