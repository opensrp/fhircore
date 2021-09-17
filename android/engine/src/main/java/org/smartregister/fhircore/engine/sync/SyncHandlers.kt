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

import com.google.android.fhir.sync.State
import java.lang.ref.WeakReference
import org.smartregister.fhircore.engine.sync.SyncBroadcaster.broadcastSync

/**
 * An interface the exposes a callback method [onSync] which accepts an application level FHIR Sync
 * [State].
 */
interface OnSyncListener {
  /** Callback method invoked to handle sync [state] */
  fun onSync(state: State)
}

/**
 * A broadcaster that maintains a list of [OnSyncListener]. Whenever a new sync [State] is received
 * the [SyncBroadcaster] will transmit the [State] to every [OnSyncListener] that registered by
 * invoking [broadcastSync] method
 */
object SyncBroadcaster {

  private val syncListeners = mutableListOf<WeakReference<OnSyncListener>>()

  fun registerSyncListener(onSyncListener: OnSyncListener) {
    syncListeners.add(WeakReference(onSyncListener))
  }

  fun broadcastSync(state: State) {
    syncListeners.forEach { it.get()?.onSync(state = state) }
  }
}
