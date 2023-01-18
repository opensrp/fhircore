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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.ResourceSyncException
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
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
  @ApplicationContext val appContext: Context
) {
  fun runSync(networkState: (Context) -> Boolean = { NetworkState(it).invoke() }) {
    Timber.i("Running one-time sync...")
    CoroutineScope(dispatcherProvider.io()).launch {
      networkState(appContext).apply {
        if (this) Sync.oneTimeSync<AppSyncWorker>(appContext).collect { sharedSyncStatus.emit(it) }
        else {
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
    const val DEFAULT_SYNC_INTERVAL: Long = 15
  }
}
