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

package org.smartregister.fhircore.engine.configuration.app

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.SyncJob
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.sync.PeriodicSyncWorker
import org.smartregister.fhircore.engine.sync.SyncBroadcaster

/**
 * An interface that provides the application configurations.
 * @property resourceSyncParams Set [FhirEngine] resource sync params needed for syncing data from
 * the server
 */
interface ConfigService {

  val resourceSyncParams: Map<ResourceType, Map<String, String>>

  /** Provide [AuthConfiguration] for the Application */
  fun provideAuthConfiguration(): AuthConfiguration = AuthConfiguration(
    fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
    oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
    clientId = BuildConfig.OAUTH_CIENT_ID,
    clientSecret = BuildConfig.OAUTH_CLIENT_SECRET,
  )

  /**
   * Schedule periodic sync periodically as defined in the [configurationRegistry] application
   * config interval. The [syncBroadcaster] will broadcast the sync status to its listeners
   */
  fun schedulePeriodicSync(
    syncJob: SyncJob,
    configurationRegistry: ConfigurationRegistry,
    syncBroadcaster: SyncBroadcaster,
    syncInterval: Long = 30
  ) {
    CoroutineScope(Dispatchers.Main).launch {
      syncBroadcaster.sharedSyncStatus.emitAll(syncJob.stateFlow())
    }

    syncJob.poll(
      periodicSyncConfiguration =
        PeriodicSyncConfiguration(repeat = RepeatInterval(syncInterval, TimeUnit.MINUTES)),
      clazz = PeriodicSyncWorker::class.java
    )
  }
}
