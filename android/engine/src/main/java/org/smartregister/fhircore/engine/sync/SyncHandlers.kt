package org.smartregister.fhircore.engine.sync

import com.google.android.fhir.sync.State
import java.lang.ref.WeakReference
import org.smartregister.fhircore.engine.sync.SyncBroadcaster.broadcastSync

/**
 * An interface the exposes a callback method [onSync] which accepts an application level FHIR Sync
 * [State].
 */
interface OnSyncListener {
  /**
   * Callback method invoked to handle sync [state]
   */
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
