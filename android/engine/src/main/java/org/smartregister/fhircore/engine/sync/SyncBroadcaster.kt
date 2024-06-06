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

package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.hasKeyWithValueOfType
import androidx.work.workDataOf
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.ResourceSyncException
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJobStatus
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

/**
 * A broadcaster that maintains a list of [OnSyncListener]. Whenever a new sync [State] is received
 * the [SyncBroadcaster] will transmit the [State] to every [OnSyncListener] that registered
 */
class SyncBroadcaster
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val configService: ConfigService,
  val fhirEngine: FhirEngine,
  // TODO: Move this to the SyncListenerManager
  val sharedSyncStatus: MutableSharedFlow<SyncJobStatus> = MutableSharedFlow(),
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
  val tracer: PerformanceReporter,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val tokenAuthenticator: TokenAuthenticator,
  @ApplicationContext val appContext: Context,
) {

  private inline fun <reified W : FhirSyncWorker> getWorkerInfo(): Flow<SyncJobStatus> {
    val oneTimeWorkName = "${W::class.java.name}-oneTimeSync"
    val periodicWorkName = "${W::class.java.name}-periodicSync"

    return getWorkerInfo(oneTimeWorkName)
  }

  fun runSync(networkState: (Context) -> Boolean = { NetworkState(it).invoke() }) {
    Timber.i("Running one-time sync...")
    CoroutineScope(dispatcherProvider.io()).launch {
      val isConnected = networkState(appContext)
      if (!isConnected) {
        val message = appContext.getString(R.string.unable_to_sync)
        val resourceSyncException =
          listOf(ResourceSyncException(ResourceType.Flag, java.lang.Exception(message)))
        sharedSyncStatus.emit(SyncJobStatus.Failed(resourceSyncException))
        return@launch
      }
      val isAuthenticated = tokenAuthenticator.sessionActive()
      if (!isAuthenticated) {
        val authFailResourceSyncException =
          ResourceSyncException(
            ResourceType.Flag,
            Exception(appContext.getString(R.string.sync_authentication_error)),
          )
        sharedSyncStatus.emit(SyncJobStatus.Failed(listOf(authFailResourceSyncException)))
        return@launch
      }

      Sync.oneTimeSync<AppSyncWorker>(appContext)
      getWorkerInfo<AppSyncWorker>().collect {
        sharedSyncStatus.emit(it)
        this@SyncBroadcaster.traceSync(it)
      }
    }
  }

  fun isInitialSync() =
    sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null).isNullOrBlank()

  fun registerSyncListener(onSyncListener: OnSyncListener, scope: CoroutineScope) {
    scope.launch { sharedSyncStatus.collect { onSyncListener.onSync(state = it) } }
  }

  private fun traceSync(syncJobStatus: SyncJobStatus) {
    when (syncJobStatus) {
      is SyncJobStatus.Failed,
      is SyncJobStatus.Succeeded, -> {
        if (syncJobStatus is SyncJobStatus.Failed) {
          syncJobStatus.exceptions?.forEachIndexed { _, resourceSyncException ->
            Firebase.crashlytics.recordException(resourceSyncException.exception)
          }
        }
        tracer.putAttribute(SYNC_TRACE, SYNC_ATTR_RESULT, syncJobStatus::class.java.simpleName)
        tracer.stopTrace(SYNC_TRACE)
      }
      is SyncJobStatus.Started -> {
        tracer.startTrace(SYNC_TRACE)
        tracer.putAttribute(
          SYNC_TRACE,
          SYNC_ATTR_TYPE,
          if (isInitialSync()) SYNC_ATTR_TYPE_INITIAL else SYNC_ATTR_TYPE_SUBSEQUENT,
        )
      }
      else -> {}
    }
  }

  /**
   * Workaround to ensure terminal SyncJobStatus, i.e SyncJobStatus.Failed and
   * SyncJobStatus.Succeeded, get emitted
   *
   * Gets the worker info for the [FhirSyncWorker], including outputData
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun getWorkerInfo(workerName: String): Flow<SyncJobStatus> =
    WorkManager.getInstance(appContext)
      .getWorkInfosForUniqueWorkLiveData(workerName)
      .asFlow()
      .flatMapConcat { it.asFlow() }
      .flatMapConcat { workInfo ->
        val failedWorkData =
          when {
            workInfo.state.isFinished && workInfo.state != WorkInfo.State.SUCCEEDED ->
              workDataOf(
                "StateType" to SyncJobStatus.Failed::class.java.name,
                "State" to Sync.gson.toJson(SyncJobStatus.Failed(emptyList())),
              )
            else -> workDataOf()
          }

        flowOf(workInfo.progress, workInfo.outputData, failedWorkData)
          .filter { it.keyValueMap.isNotEmpty() && it.hasKeyWithValueOfType<String>("StateType") }
          .mapNotNull {
            val state = it.getString("StateType")!!
            val stateData = it.getString("State")
            Sync.gson.fromJson(stateData, Class.forName(state)) as SyncJobStatus
          }
      }

  companion object {
    const val SYNC_TRACE = "runSync"
    const val SYNC_GLITCHES_METRIC = "sync_glitches"
    const val SYNC_ATTR_TYPE = "sync_type"
    const val SYNC_ATTR_RESULT = "sync_result"
    const val SYNC_ATTR_RETRY = "sync_retry_count"
    const val SYNC_ATTR_TYPE_INITIAL = "initial sync"
    const val SYNC_ATTR_TYPE_SUBSEQUENT = "subsequent sync"
    const val DEFAULT_SYNC_INTERVAL: Long = 15
  }
}
