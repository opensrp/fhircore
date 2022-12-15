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
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

/**
 * A singleton class that maintains a list of [OnSyncListener] that have been registered to listen
 * to sync [State] events.
 */
@Singleton
class SyncListenerManager
@Inject
constructor(
  val configService: ConfigService,
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
) {

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
    _onSyncListeners.add(WeakReference(onSyncListener))
    Timber.w("${onSyncListener::class.simpleName} registered to receive sync state events")
    lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
          super.onStop(owner)
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
    val removed = _onSyncListeners.removeIf { it.get() == onSyncListener }
    if (removed)
      Timber.w("De-registered ${onSyncListener::class.simpleName} from receiving sync state...")
  }

  /** Retrieve registry sync params */
  fun loadSyncParams(): Map<ResourceType, Map<String, String>> {
    val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()

    val syncConfig =
      configurationRegistry.retrieveResourceConfiguration<Parameters>(ConfigType.Sync)

    val appConfig =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)

    val organizationResourceTag =
      configService.defineResourceTags().find { it.type == ResourceType.Organization.name }

    val mandatoryTags = configService.provideResourceTags(sharedPreferencesHelper)

    val relatedResourceTypes: List<String>? =
      sharedPreferencesHelper.read(SharedPreferenceKey.REMOTE_SYNC_RESOURCES.name)

    // TODO Does not support nested parameters i.e. parameters.parameters...
    // TODO: expressionValue supports for Organization and Publisher literals for now
    syncConfig.parameter.map { it.resource as SearchParameter }.forEach { sp ->
      val paramName = sp.name // e.g. organization
      val paramLiteral = "#$paramName" // e.g. #organization in expression for replacement
      val paramExpression = sp.expression
      val expressionValue =
        when (paramName) {
          // TODO: Does not support multi organization yet,
          // https://github.com/opensrp/fhircore/issues/1550
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
          // replace the evaluated value into expression for complex expressions
          // e.g. #organization -> 123
          // e.g. patient.organization eq #organization -> patient.organization eq 123
          paramExpression.replace(paramLiteral, it)
        }

      // for each entity in base create and add param map
      // [Patient=[ name=Abc, organization=111 ], Encounter=[ type=MyType, location=MyHospital
      // ],..]
      if (relatedResourceTypes.isNullOrEmpty()) {
          sp.base.mapNotNull { it.code }
        } else {
          relatedResourceTypes
        }
        .forEach { clinicalResource ->
          val resourceType = ResourceType.fromCode(clinicalResource)
          val pair = pairs.find { it.first == resourceType }
          if (pair == null) {
            pairs.add(
              Pair(
                resourceType,
                expressionValue?.let { mapOf(sp.code to expressionValue) } ?: mapOf()
              )
            )
          } else {
            expressionValue?.let {
              // add another parameter if there is a matching resource type
              // e.g. [(Patient, {organization=105})] to [(Patient, {organization=105,
              // _count=100})]
              val updatedPair = pair.second.toMutableMap().apply { put(sp.code, expressionValue) }
              val index = pairs.indexOfFirst { it.first == resourceType }
              pairs.set(index, Pair(resourceType, updatedPair))
            }
          }
        }
    }

    Timber.i("SYNC CONFIG $pairs")

    return mapOf(*pairs.toTypedArray())
  }
}
