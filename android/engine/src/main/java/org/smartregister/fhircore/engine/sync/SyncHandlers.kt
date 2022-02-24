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

import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.SyncJob
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import timber.log.Timber

/**
 * An interface the exposes a callback method [onSync] which accepts an application level FHIR Sync
 * [State].
 */
interface OnSyncListener {
  /** Callback method invoked to handle sync [state] */
  fun onSync(state: State)
}

/**
 * This interface qualifies an Activity/View to initiate sync. NOTE. There can only be one sync
 * initiator for the entire application
 */
interface SyncInitiator {
  /** Run one time sync */
  fun runSync()
}

/**
 * A broadcaster that maintains a list of [OnSyncListener]. Whenever a new sync [State] is received
 * the [SyncBroadcaster] will transmit the [State] to every [OnSyncListener] that registered by
 * invoking [broadcastSync] method
 */
class SyncBroadcaster(
  val fhirResourceDataSource: FhirResourceDataSource,
  val configurationRegistry: ConfigurationRegistry,
  val syncJob: SyncJob,
  val fhirEngine: FhirEngine,
  val sharedSyncStatus: MutableSharedFlow<State> = MutableSharedFlow(),
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) {

  var syncInitiator: SyncInitiator? = null

  val syncListeners = mutableListOf<WeakReference<OnSyncListener>>()

  fun runSync() {
    CoroutineScope(dispatcherProvider.main()).launch {
      try {
        syncJob.run(
          fhirEngine = fhirEngine,
          dataSource = fhirResourceDataSource,
          resourceSyncParams = configurationRegistry.configService.resourceSyncParams,
          subscribeTo = sharedSyncStatus
        )

        sharedSyncStatus.collect { broadcastSync(it) }
      } catch (exception: Exception) {
        Timber.e("Error syncing data", exception.stackTraceToString())
      }
    }
  }

  fun registerSyncListener(onSyncListener: OnSyncListener) {
    syncListeners.add(WeakReference(onSyncListener))
  }

  fun unRegisterSyncInitiator() {
    this.syncInitiator = null
  }

  fun unRegisterSyncListener(onSyncListener: OnSyncListener) {
    syncListeners.removeIf { it.get() == onSyncListener }
  }

  fun broadcastSync(state: State) {
    syncListeners.forEach { it.get()?.onSync(state = state) }
  }
}
