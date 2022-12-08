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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.DispatcherProvider

/**
 * This class is used to trigger one time and periodic syncs. A new instance of this class is
 * created each time because a new instance of [ResourceParamsBasedDownloadWorkManager] is needed
 * everytime sync is triggered. This class should not be provided as a singleton. The sync [State]
 * events are sent to the registered [OnSyncListener] maintained by the [SyncListenerManager]
 */
class SyncBroadcaster
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val fhirEngine: FhirEngine,
  val syncListenerManager: SyncListenerManager,
  val dispatcherProvider: DispatcherProvider
) {

  /*fun runSync() {
    val coroutineScope = CoroutineScope(dispatcherProvider.main())
    Timber.i("Running one time sync...")
    val syncStateFlow = MutableSharedFlow<State>()
    coroutineScope.launch {
      syncStateFlow
        .onEach {
          syncListenerManager.onSyncListeners.forEach { onSyncListener ->
            onSyncListener.onSync(it)
          }
        }
        .handleErrors()
        .launchIn(this)
    }

    coroutineScope.launch(dispatcherProvider.io()) {
      syncJob.run(
        fhirEngine = fhirEngine,
        downloadManager =
          ResourceParamsBasedDownloadWorkManager(syncParams = syncListenerManager.loadSyncParams()),
        subscribeTo = syncStateFlow,
        resolver = AcceptLocalConflictResolver
      )
    }
  }

  private fun <T> Flow<T>.handleErrors(): Flow<T> = catch { throwable -> Timber.e(throwable) }

  */
  /**
   * Schedule periodic sync periodically as defined in the application config interval. The sync
   * [State] will be broadcast to the listeners
   */
  /*
  @OptIn(ExperimentalCoroutinesApi::class)
  fun schedulePeriodicSync() {
    Timber.i("Scheduling periodic sync...")
    val appConfig =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
    val periodicSyncFlow: Flow<State> =
      syncJob.poll(
        periodicSyncConfiguration =
          PeriodicSyncConfiguration(
            repeat = RepeatInterval(appConfig.syncInterval, TimeUnit.MINUTES)
          ),
        clazz = AppSyncWorker::class.java
      )
    val coroutineScope = CoroutineScope(dispatcherProvider.main())
    coroutineScope.launch {
      periodicSyncFlow.collect { state ->
        syncListenerManager.onSyncListeners.forEach { onSyncListener ->
          onSyncListener.onSync(state)
        }
      }
    }
  }*/
}
