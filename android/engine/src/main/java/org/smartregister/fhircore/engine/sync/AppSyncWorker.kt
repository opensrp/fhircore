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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import com.google.android.fhir.sync.upload.HttpCreateMethod
import com.google.android.fhir.sync.upload.HttpUpdateMethod
import com.google.android.fhir.sync.upload.UploadStrategy
import com.ibm.icu.util.Calendar
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.util.NotificationConstants
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import retrofit2.HttpException
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
  private val configService: ConfigService,
  private val customResourceSyncService: CustomResourceSyncService,
) : FhirSyncWorker(appContext, workerParams), OnSyncListener {
  private val notificationManager =
    appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    syncListenerManager.registerSyncListener(this)
  }

  override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver

  override fun getDownloadWorkManager(): DownloadWorkManager =
    OpenSrpDownloadManager(
      resourceSearchParams = runBlocking { syncListenerManager.loadResourceSearchParams() },
      context = appTimeStampContext,
    )

  override suspend fun doWork(): Result {
    kotlin
      .runCatching {
        saveSyncStartTimestamp()
        setForeground(getForegroundInfo())
        customResourceSyncService.runCustomResourceSync()
      }
      .onSuccess {
        return super.doWork()
      }
      .onFailure { exception ->
        when (exception) {
          is HttpException -> {
            val response = exception.response()
            if (response != null && (400..503).contains(response.code())) {
              Timber.e("HTTP exception ${response.code()} -> ${response.errorBody()}")
            }
          }
          else -> Timber.e(exception)
        }
        syncListenerManager.emitSyncStatus(
          SyncState(
            counter = SYNC_COUNTER_1,
            currentSyncJobStatus = CurrentSyncJobStatus.Failed(OffsetDateTime.now()),
          ),
        )
        return result()
      }
    return Result.success()
  }

  private fun result(): Result =
    if (inputData.getInt(MAX_RETRIES, 3) > runAttemptCount) Result.retry() else Result.failure()

  private fun saveSyncStartTimestamp() {
    syncListenerManager.sharedPreferencesHelper.write(
      SharedPreferenceKey.SYNC_START_TIMESTAMP.name,
      Calendar.getInstance().timeInMillis,
    )
  }

  private fun saveSyncEndTimestamp() {
    syncListenerManager.sharedPreferencesHelper.write(
      SharedPreferenceKey.SYNC_END_TIMESTAMP.name,
      Calendar.getInstance().timeInMillis,
    )
  }

  override fun getFhirEngine(): FhirEngine = openSrpFhirEngine

  override fun getUploadStrategy(): UploadStrategy =
    UploadStrategy.forBundleRequest(
      methodForCreate = HttpCreateMethod.PUT,
      methodForUpdate = HttpUpdateMethod.PATCH,
      squash = true,
      bundleSize = 500,
    )

  override suspend fun getForegroundInfo(): ForegroundInfo {
    val channel =
      NotificationChannel(
        NotificationConstants.ChannelId.DATA_SYNC,
        NotificationConstants.ChannelName.DATA_SYNC,
        NotificationManager.IMPORTANCE_LOW,
      )
    notificationManager.createNotificationChannel(channel)

    val notification: Notification =
      buildNotification(progress = 0, isSyncUpload = false, isInitial = true)

    val foregroundInfo =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ForegroundInfo(
          NotificationConstants.NotificationId.DATA_SYNC,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
      } else {
        ForegroundInfo(NotificationConstants.NotificationId.DATA_SYNC, notification)
      }
    return foregroundInfo
  }

  private fun getSyncProgress(completed: Int, total: Int) =
    completed * 100 / if (total > 0) total else 1

  override fun onSync(syncState: SyncState) {
    when (val syncJobStatus = syncState.currentSyncJobStatus) {
      is CurrentSyncJobStatus.Running -> {
        if (syncJobStatus.inProgressSyncJob is SyncJobStatus.InProgress) {
          val inProgressSyncJob = syncJobStatus.inProgressSyncJob as SyncJobStatus.InProgress
          val isSyncUpload = inProgressSyncJob.syncOperation == SyncOperation.UPLOAD
          val progressPercentage =
            getSyncProgress(inProgressSyncJob.completed, inProgressSyncJob.total)
          updateNotificationProgress(progress = progressPercentage, isSyncUpload = isSyncUpload)
        }
      }
      is CurrentSyncJobStatus.Succeeded -> saveSyncEndTimestamp()
      else -> {}
    }
  }

  private fun buildNotification(
    progress: Int,
    isSyncUpload: Boolean,
    isInitial: Boolean,
  ): Notification {
    return NotificationCompat.Builder(applicationContext, NotificationConstants.ChannelId.DATA_SYNC)
      .setContentTitle(
        applicationContext.getString(
          if (isInitial) {
            R.string.syncing_initiated
          } else if (isSyncUpload) R.string.syncing_up else R.string.syncing_down,
        ),
      )
      .setSmallIcon(R.drawable.ic_opensrp_small_logo)
      .setLargeIcon(Icon.createWithResource(applicationContext, configService.getLauncherIcon()))
      .setContentText(applicationContext.getString(R.string.percentage_progress, progress))
      .setProgress(100, progress, progress == 0)
      .setOngoing(true)
      .build()
  }

  private fun updateNotificationProgress(progress: Int, isSyncUpload: Boolean) {
    val notificationManager =
      applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification =
      buildNotification(progress = progress, isSyncUpload = isSyncUpload, isInitial = false)
    notificationManager.notify(NotificationConstants.NotificationId.DATA_SYNC, notification)
  }

  companion object {
    const val MAX_RETRIES = "max_retires"
  }
}
