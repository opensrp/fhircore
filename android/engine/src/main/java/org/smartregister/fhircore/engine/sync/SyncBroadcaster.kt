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

import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.SyncJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import timber.log.Timber

/**
 * A broadcaster that maintains a list of [OnSyncListener]. Whenever a new sync [State] is received
 * the [SyncBroadcaster] will transmit the [State] to every [OnSyncListener] that registered
 */
class SyncBroadcaster(
  val fhirResourceDataSource: FhirResourceDataSource,
  val configService: ConfigService,
  val syncJob: SyncJob,
  val fhirEngine: FhirEngine,
  val sharedSyncStatus: MutableSharedFlow<State> = MutableSharedFlow(),
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) {
  fun runSync() {
    CoroutineScope(dispatcherProvider.io()).launch {
      try {
        syncJob.run(
          fhirEngine = fhirEngine,
          dataSource = fhirResourceDataSource,
          resourceSyncParams = configService.resourceSyncParams,
          subscribeTo = sharedSyncStatus
        )
      } catch (exception: Exception) {
        Timber.e("Error syncing data")
        Timber.e(exception)
      }
    }
  }

  fun registerSyncListener(onSyncListener: OnSyncListener, scope: LifecycleCoroutineScope) {
    scope.launch { sharedSyncStatus.collect { onSyncListener.onSync(state = it) } }
  }
}
