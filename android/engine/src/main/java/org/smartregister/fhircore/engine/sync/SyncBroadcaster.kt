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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.SyncJob
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

/**
 * This class is used to trigger one time and periodic syncs. A new instance of this class is
 * created each time because a new instance of [ResourceParamsBasedDownloadWorkManager] is needed
 * everytime sync is triggered. This class should not be provided as a singleton. The sync [State]
 * events are sent to the registered [OnSyncListener] maintained by the [SyncListenerManager]
 */
class SyncBroadcaster
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configService: ConfigService,
  val syncJob: SyncJob,
  val fhirEngine: FhirEngine,
  val syncListenerManager: SyncListenerManager,
  val dispatcherProvider: DispatcherProvider
) {

  fun runSync() {
    val coroutineScope = CoroutineScope(dispatcherProvider.main())
    Timber.i("Running one time sync...")
    val syncStateFlow = MutableSharedFlow<State>()
    coroutineScope.launch {
      syncStateFlow
        .onEach {
          syncListenerManager.onSyncListeners.forEach { onSyncListener ->
            onSyncListener.onSync(it)
          }
        }
        .handleErrors()
        .launchIn(this)
    }

    coroutineScope.launch(dispatcherProvider.io()) {
      syncJob.run(
        fhirEngine = fhirEngine,
        downloadManager =
          ResourceParamsBasedDownloadWorkManager(syncParams = loadSyncParams().toMap()),
        subscribeTo = syncStateFlow,
        resolver = AcceptLocalConflictResolver
      )
    }
  }

  private fun <T> Flow<T>.handleErrors(): Flow<T> = catch { throwable -> Timber.e(throwable) }

  /** Retrieve registry sync params */
  fun loadSyncParams(): Map<ResourceType, Map<String, String>> {
    val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()

    val syncConfig =
      configurationRegistry.retrieveResourceConfiguration<Parameters>(ConfigType.Sync)

    val appConfig =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)

    val organizationIds: List<String>? =
      if (appConfig.syncStrategies.contains(ResourceType.Organization.name)) {
        sharedPreferencesHelper.read<List<String>>(ResourceType.Organization.name)
      } else null

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
          ConfigurationRegistry.ORGANIZATION -> organizationIds?.firstOrNull()
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

  /**
   * Schedule periodic sync periodically as defined in the application config interval. The sync
   * [State] will be broadcast to the listeners
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun schedulePeriodicSync() {
    Timber.i("Scheduling periodic sync...")
    val appConfig =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
    val periodicSyncFlow: Flow<State> =
      syncJob.poll(
        periodicSyncConfiguration =
          PeriodicSyncConfiguration(
            repeat = RepeatInterval(appConfig.syncInterval, TimeUnit.MINUTES)
          ),
        clazz = FhirSyncWorker::class.java // TODO requires a concrete class of FhirSyncWorker
      )
    periodicSyncFlow.collect { state ->
      syncListenerManager.onSyncListeners.forEach { onSyncListener -> onSyncListener.onSync(state) }
    }
  }
}
