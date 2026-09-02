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
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.BackoffCriteria
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.LastSyncJobStatus
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.PeriodicSyncJobStatus
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.RetryConfiguration
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.DispatcherProvider
import timber.log.Timber

/**
 * This class is used to trigger one time and periodic syncs. A new instance of this class is
 * created each time because a new instance of [ResourceParamsBasedDownloadWorkManager] is needed
 * everytime sync is triggered; this class SHOULD NOT be provided as a singleton. The
 * [SyncJobStatus] events are sent to the registered [OnSyncListener] maintained by the
 * [SyncListenerManager]
 */
class SyncBroadcaster
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val fhirEngine: FhirEngine,
  val syncListenerManager: SyncListenerManager,
  val dispatcherProvider: DispatcherProvider,
  val workManager: WorkManager,
  @ApplicationContext val context: Context,
) {

  /**
   * Timestamp of the last periodic-sync terminal result already forwarded to listeners. Used to
   * suppress the periodic flow's repeated stale terminal emissions while it sits idle between runs.
   */
  private var lastPeriodicTerminalTimestamp: OffsetDateTime? = null

  /**
   * Whether the periodic-sync baseline has been seeded from its first emission yet. On
   * (re)scheduling the periodic flow reports the PREVIOUS run's result via lastSyncJobStatus (or an
   * android-fhir default — commonly Failed — when nothing has run yet). That pre-existing terminal
   * must be adopted as the baseline WITHOUT being surfaced, otherwise a stale "Sync failed"/"Sync
   * complete" flashes on the sync bar at startup, right after the initial one-time sync's dialog is
   * dismissed.
   */
  private var periodicBaselineSeeded = false

  /**
   * Wall-clock timestamp of the last "sync complete" ([CurrentSyncJobStatus.Succeeded]) actually
   * surfaced to listeners, from EITHER the one-time or the periodic sync. Used to debounce the
   * redundant periodic run that WorkManager fires immediately after login — right after the initial
   * one-time sync already showed "Sync complete", the periodic sync re-runs the same pull and
   * flashed "Syncing"→"Sync complete" a second time. Periodic activity within
   * [REDUNDANT_PERIODIC_WINDOW] of this timestamp is suppressed so that second cycle stays silent.
   */
  private var lastBroadcastSucceededTimestamp: OffsetDateTime? = null

  /**
   * Whether a periodic emission timestamped [eventTimestamp] falls inside the redundant window that
   * immediately follows a surfaced "sync complete" at [lastSucceededTimestamp]. A genuine periodic
   * run happens a whole [interval] (default 15 min) later, well outside the window.
   */
  internal fun isWithinRedundantPeriodicWindow(
    eventTimestamp: OffsetDateTime?,
    lastSucceededTimestamp: OffsetDateTime?,
  ): Boolean {
    if (eventTimestamp == null || lastSucceededTimestamp == null) return false
    val elapsed = java.time.Duration.between(lastSucceededTimestamp, eventTimestamp)
    return !elapsed.isNegative && elapsed < REDUNDANT_PERIODIC_WINDOW
  }

  /**
   * Run one time sync. The [SyncJobStatus] will be broadcast to all the registered [OnSyncListener]
   * 's
   */
  suspend fun runOneTimeSync(): Unit = coroutineScope {
    Timber.i("Running one time sync...")
    Sync.oneTimeSync<AppSyncWorker>(context).handleOneTimeSyncJobStatus(this)
  }

  /**
   * Schedule periodic sync periodically as defined in the application config interval. The
   * [SyncJobStatus] will be broadcast to all the registered [OnSyncListener]'s
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun schedulePeriodicSync(interval: Long = 15) = coroutineScope {
    Timber.i("Scheduling periodic sync...")
    Sync.periodicSync<AppSyncWorker>(
        context = context,
        periodicSyncConfiguration =
          PeriodicSyncConfiguration(
            syncConstraints =
              Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            repeat = RepeatInterval(interval = interval, timeUnit = TimeUnit.MINUTES),
            retryConfiguration =
              RetryConfiguration(
                backoffCriteria =
                  BackoffCriteria(
                    backoffDelay = 10,
                    timeUnit = TimeUnit.SECONDS,
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                  ),
                maxRetries = 3,
              ),
          ),
      )
      .handlePeriodicSyncJobStatus(this)
  }

  private fun Flow<PeriodicSyncJobStatus>.handlePeriodicSyncJobStatus(
    coroutineScope: CoroutineScope,
  ) {
    this.onEach { periodicSyncJobStatus ->
        if (!periodicBaselineSeeded) {
          periodicBaselineSeeded = true
          terminalTimestampOf(periodicSyncJobStatus.lastSyncJobStatus)?.let {
            lastPeriodicTerminalTimestamp = it
          }
        }
        val statusToBroadcast =
          periodicStatusToBroadcast(periodicSyncJobStatus, lastPeriodicTerminalTimestamp)
        if (statusToBroadcast != null) {
          if (statusToBroadcast is CurrentSyncJobStatus.Succeeded) {
            lastPeriodicTerminalTimestamp = statusToBroadcast.timestamp
          }

          val eventTimestamp =
            when (statusToBroadcast) {
              is CurrentSyncJobStatus.Running ->
                (statusToBroadcast.inProgressSyncJob as? SyncJobStatus.InProgress)?.timestamp
              is CurrentSyncJobStatus.Succeeded -> statusToBroadcast.timestamp
              else -> null
            }
          if (!isWithinRedundantPeriodicWindow(eventTimestamp, lastBroadcastSucceededTimestamp)) {
            if (statusToBroadcast is CurrentSyncJobStatus.Succeeded) {
              lastBroadcastSucceededTimestamp = statusToBroadcast.timestamp
            }
            val syncState =
              SyncState(
                counter = configurationRegistry.retrieveTotalSyncCount(),
                currentSyncJobStatus = statusToBroadcast,
              )
            syncListenerManager.onSyncListeners.forEach { it.onSync(syncState) }
          }
        }
      }
      .catch { throwable -> Timber.e("Encountered an error during periodic sync:", throwable) }
      .shareIn(coroutineScope, SharingStarted.Eagerly, 1)
      .launchIn(coroutineScope)
  }

  /**
   * Maps a [PeriodicSyncJobStatus] to the [CurrentSyncJobStatus] that should be forwarded to the
   * foreground sync UI, or `null` when the emission is an idle/stale heartbeat that must be
   * suppressed. Only two things are surfaced:
   * - live [CurrentSyncJobStatus.Running] progress while a periodic sync is actually executing, and
   * - a terminal [CurrentSyncJobStatus.Succeeded]/[CurrentSyncJobStatus.Failed] once per completed
   *   run, detected via a new [LastSyncJobStatus] timestamp that differs from
   *   [lastKnownTerminalTimestamp].
   *
   * Everything else (idle Enqueued/Blocked while waiting for the next interval, and repeated stale
   * terminals whose timestamp has not changed) returns `null` so it never reaches the shared UI
   * state and cannot disrupt a concurrent one-time sync.
   */
  internal fun periodicStatusToBroadcast(
    periodicSyncJobStatus: PeriodicSyncJobStatus,
    lastKnownTerminalTimestamp: OffsetDateTime?,
  ): CurrentSyncJobStatus? =
    when (val current = periodicSyncJobStatus.currentSyncJobStatus) {
      is CurrentSyncJobStatus.Running -> current
      else ->
        when (val last = periodicSyncJobStatus.lastSyncJobStatus) {
          is LastSyncJobStatus.Succeeded ->
            if (last.timestamp != lastKnownTerminalTimestamp) {
              CurrentSyncJobStatus.Succeeded(last.timestamp)
            } else {
              null
            }
          is LastSyncJobStatus.Failed -> null
          else -> null
        }
    }

  /** The timestamp of a terminal [LastSyncJobStatus] (Succeeded/Failed), or `null` otherwise. */
  internal fun terminalTimestampOf(last: LastSyncJobStatus?): OffsetDateTime? =
    when (last) {
      is LastSyncJobStatus.Succeeded -> last.timestamp
      is LastSyncJobStatus.Failed -> last.timestamp
      else -> null
    }

  private fun Flow<CurrentSyncJobStatus>.handleOneTimeSyncJobStatus(
    coroutineScope: CoroutineScope,
  ) {
    this.onEach { currentSyncJobStatus ->
        if (currentSyncJobStatus is CurrentSyncJobStatus.Succeeded) {
          lastBroadcastSucceededTimestamp = currentSyncJobStatus.timestamp
        }
        syncListenerManager.onSyncListeners.forEach { onSyncListener ->
          onSyncListener.onSync(
            SyncState(
              counter = configurationRegistry.retrieveTotalSyncCount(),
              currentSyncJobStatus = currentSyncJobStatus,
            ),
          )
        }
      }
      .catch { throwable -> Timber.e("Encountered an error during one time sync:", throwable) }
      .shareIn(coroutineScope, SharingStarted.Eagerly, 1)
      .launchIn(coroutineScope)
  }

  companion object {
    /**
     * How long after a surfaced "sync complete" a following periodic run is treated as the
     * redundant post-login re-sync and kept silent. Comfortably shorter than the 15-min periodic
     * interval, so genuine scheduled runs are never suppressed.
     */
    private val REDUNDANT_PERIODIC_WINDOW: java.time.Duration = java.time.Duration.ofMinutes(2)
  }
}
