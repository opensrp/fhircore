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

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.fhir.sync.State
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * A singleton class that maintains a list of [OnSyncListener] that have been registered to listen
 * to sync [State] events.
 */
@Singleton
class SyncListenerManager @Inject constructor() {

  private val _onSyncListeners = mutableListOf<WeakReference<OnSyncListener>>()
  val onSyncListeners: List<OnSyncListener>
    get() = _onSyncListeners.mapNotNull { it.get() }

  /**
   * Register [OnSyncListener] for sync [State] events. Typically the [OnSyncListener] will be
   * implemented in a [Lifecycle](an Activity/Fragment). This function ensures the [OnSyncListener]
   * is removed for the [_onSyncListeners] list when the [Lifecycle] changes to
   * [Lifecycle.State.DESTROYED]
   */
  fun registerSyncListener(onSyncListener: OnSyncListener, lifecycle: Lifecycle) {
    Timber.w("${onSyncListener::class.simpleName} registered to receive sync state events")
    _onSyncListeners.add(WeakReference(onSyncListener))
    lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
          super.onDestroy(owner)
          deregisterSyncListener(onSyncListener)
        }
      }
    )
  }

  /**
   * This function removes [onSyncListener] from the list of registered [OnSyncListener]'s to stop
   * receiving sync state events.
   */
  fun deregisterSyncListener(onSyncListener: OnSyncListener) {
    Timber.w("Deregister ${onSyncListener::class.simpleName} from receiving sync state...")
    _onSyncListeners.removeIf { it.get() == onSyncListener }
  }
}
