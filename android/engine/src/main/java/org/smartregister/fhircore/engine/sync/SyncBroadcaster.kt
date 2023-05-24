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
import androidx.work.WorkManager
import androidx.work.hasKeyWithValueOfType
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.ResourceSyncException
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
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
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
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
  val sharedSyncStatus: MutableSharedFlow<SyncJobStatus> = MutableSharedFlow(),
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
  val tracer: PerformanceReporter,
  @ApplicationContext val appContext: Context
) {
  /**
   * Workaround to ensure terminal SyncJobStatus, i.e SyncJobStatus.Failed and
   * SyncJobStatus.Finished, get emitted
   *
   * Gets the worker info for the [FhirSyncWorker], including outputData
   */
  @OptIn(FlowPreview::class)
  inline fun <reified W : FhirSyncWorker> getWorkerInfo(): Flow<SyncJobStatus> {
    return WorkManager.getInstance(appContext)
      .getWorkInfosForUniqueWorkLiveData(W::class.java.name)
      .asFlow()
      .flatMapConcat { it.asFlow() }
      .flatMapConcat { workInfo ->
        flowOf(workInfo.progress, workInfo.outputData)
          .filter { it.keyValueMap.isNotEmpty() && it.hasKeyWithValueOfType<String>("StateType") }
          .mapNotNull {
            val state = it.getString("StateType")!!
            val stateData = it.getString("State")
            Sync.gson.fromJson(stateData, Class.forName(state)) as SyncJobStatus
          }
      }
  }

  fun runSync(networkState: (Context) -> Boolean = { NetworkState(it).invoke() }) {
    Timber.i("Running one-time sync...")
    CoroutineScope(dispatcherProvider.io()).launch {
      networkState(appContext).apply {
        if (this) {
          Sync.oneTimeSync<AppSyncWorker>(appContext)
          getWorkerInfo<AppSyncWorker>().collect {
            if (it is SyncJobStatus.Started) {
              tracer.startTrace(SYNC_TRACE)
            } else if (it !is SyncJobStatus.InProgress) {
              tracer.stopTrace(SYNC_TRACE)
            }
            sharedSyncStatus.emit(it)
          }
        } else {
          val message = appContext.getString(R.string.unable_to_sync)
          val resourceSyncException =
            listOf(ResourceSyncException(ResourceType.Flag, java.lang.Exception(message)))
          sharedSyncStatus.emit(SyncJobStatus.Failed(resourceSyncException))
        }
      }
    }
  }

  fun registerSyncListener(onSyncListener: OnSyncListener, scope: CoroutineScope) {
    scope.launch { sharedSyncStatus.collect { onSyncListener.onSync(state = it) } }
  }

  companion object {
    const val SYNC_TRACE = "runSync"
    const val DEFAULT_SYNC_INTERVAL: Long = 15
  }
}
