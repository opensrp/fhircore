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
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.datastore.syncLocationIdsProtoStore
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
  private val appConfig by lazy {
    configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
      ConfigType.Application,
    )
  }
  private val syncConfig by lazy {
    configurationRegistry.retrieveResourceConfiguration<Parameters>(ConfigType.Sync)
  }

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
    _onSyncListeners.add(WeakReference(onSyncListener))
    Timber.w("${onSyncListener::class.simpleName} registered to receive sync state events")
    lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
          super.onStop(owner)
          deregisterSyncListener(onSyncListener)
        }
      },
    )
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

  /** Retrieve registry sync params */
  fun loadSyncParams(): Map<ResourceType, Map<String, String>> {
    val syncParamsMap = mutableMapOf<ResourceType, MutableMap<String, String>>()
    val organizationResourceTag =
      configService.defineResourceTags().find { it.type == ResourceType.Organization.name }
    val mandatoryTags = configService.provideResourceTags(sharedPreferencesHelper)

    // Retrieve REL locationIds otherwise return null
    val locationIds = runBlocking {
      context.syncLocationIdsProtoStore.data
        .firstOrNull()
        ?.filter { it.toggleableState == ToggleableState.On }
        ?.map { it.locationId }
        .takeIf { !it.isNullOrEmpty() }
    }

    // TODO Does not support nested parameters i.e. parameters.parameters...
    syncConfig.parameter
      .asSequence()
      .map { it.resource as SearchParameter }
      .filterNot { it.type == Enumerations.SearchParamType.SPECIAL }
      .forEach { searchParameter ->
        val paramName = searchParameter.name
        val paramLiteral = "#$paramName" // e.g. #organization in expression for replacement
        val paramExpression = searchParameter.expression
        val expressionValue =
          when (paramName) {
            ConfigurationRegistry.ORGANIZATION ->
              mandatoryTags
                .firstOrNull {
                  it.display.contentEquals(organizationResourceTag?.tag?.display, ignoreCase = true)
                }
                ?.code
            ConfigurationRegistry.ID -> paramExpression
            ConfigurationRegistry.COUNT -> appConfig.remoteSyncPageSize.toString()
            else -> null
          }?.let {
            // Replace the evaluated expression with actual value .g. #organization -> 123
            paramExpression?.replace(paramLiteral, it)
          }

        // Create query param for each ResourceType p e.g.[Patient=[name=Abc, organization=111]
        searchParameter.base
          .mapNotNull { it.code }
          .forEach { resource ->
            val resourceType = ResourceType.fromCode(resource)
            val resourceQueryParamMap =
              syncParamsMap
                .getOrPut(resourceType) { mutableMapOf() }
                .apply {
                  expressionValue?.let { value -> put(searchParameter.code, value) }
                  locationIds?.let { ids -> put(SYNC_LOCATION_IDS, ids.joinToString(",")) }
                }
            syncParamsMap[resourceType] = resourceQueryParamMap
          }
      }
    Timber.i("Resource sync parameters $syncParamsMap")
    return syncParamsMap
  }

  companion object {
    private const val SYNC_LOCATION_IDS = "_syncLocations"
  }
}
