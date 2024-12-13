/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.download.ResourceSearchParams
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

/**
 * A singleton class that maintains a list of [OnSyncListener] that have been registered to listen
 * to [SyncJobStatus] emitted to indicate sync progress.
 */
@Singleton
class SyncListenerManager
@Inject
constructor(
  val configService: ConfigService,
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  @ApplicationContext val context: Context,
  val dispatcherProvider: DefaultDispatcherProvider,
) {

  private val _onSyncListeners = mutableListOf<WeakReference<OnSyncListener>>()
  val onSyncListeners: List<OnSyncListener>
    get() = _onSyncListeners.mapNotNull { it.get() }

  /**
   * Register [OnSyncListener] for [SyncJobStatus]. Typically the [OnSyncListener] will be
   * implemented in a [Lifecycle](an Activity/Fragment). This function ensures the [OnSyncListener]
   * is removed for the [_onSyncListeners] list when the [Lifecycle] changes to
   * [Lifecycle.State.DESTROYED]
   */
  fun registerSyncListener(onSyncListener: OnSyncListener, lifecycle: Lifecycle) {
    if (_onSyncListeners.find { it.get() == onSyncListener } == null) {
      _onSyncListeners.add(WeakReference(onSyncListener))
      Timber.w("${onSyncListener::class.simpleName} registered to receive sync state events")
      lifecycle.addObserver(
        object : DefaultLifecycleObserver {
          override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            deregisterSyncListener(onSyncListener)
          }
        },
      )
    }
  }

  fun registerSyncListener(onSyncListener: OnSyncListener) {
    if (_onSyncListeners.find { it.get() == onSyncListener } == null) {
      _onSyncListeners.add(WeakReference(onSyncListener))
      Timber.w("${onSyncListener::class.simpleName} registered to receive sync state events")
    }

    _onSyncListeners.removeIf { it.get() == null }
  }

  /**
   * This function removes [onSyncListener] from the list of registered [OnSyncListener]'s to stop
   * receiving sync state events.
   */
  fun deregisterSyncListener(onSyncListener: OnSyncListener) {
    val removed = _onSyncListeners.removeIf { it.get() == onSyncListener }
    if (removed) {
      Timber.w("De-registered ${onSyncListener::class.simpleName} from receiving sync state...")
    }
  }

  /**
   * This function is used to retrieve search parameters for the various [ResourceType]'s synced by
   * the application. The function returns a pair of maps, one contains the the custom Resource
   * types and the other returns the supported FHIR [ResourceType]s. The [OpenSrpDownloadManager]
   * does not support downloading of custom resource, a separate worker is implemented instead to
   * download the custom resources.
   */
  suspend fun loadResourceSearchParams(): ResourceSearchParams {
    val (_, resourceSearchParams) = configurationRegistry.loadResourceSearchParams()
    Timber.i("FHIR resource sync parameters $resourceSearchParams")
    return resourceSearchParams
  }
}
