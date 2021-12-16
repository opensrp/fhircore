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
object SyncBroadcaster {

  var syncInitiator: SyncInitiator? = null

  val syncListeners = mutableListOf<WeakReference<OnSyncListener>>()

  fun registerSyncInitiator(syncInitiator: SyncInitiator) =
    if (this.syncInitiator == null) {
      this.syncInitiator = syncInitiator
      this.syncInitiator?.runSync() ?: Timber.e("Register at least one sync initiator")
    } else {
      Timber.w(
        "One time sync can only be triggered from one place within the entire application e.g." +
          " when loading the landing register page. Other views can register as listeners to respond to Sync State"
      )
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
