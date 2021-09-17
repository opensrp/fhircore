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
import com.google.android.fhir.sync.SyncJob
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference

/**
 * An interface that provides the application configurations. Every FHIR based application is
 * required to implement this interface and call the [configureApplication] method on the OnCreate
 * method of the Application class. Other classes will then be able to access the global e.g. the
 * serverUrl [ApplicationConfiguration]
 *
 * @property applicationConfiguration Set application configurations
 * @property authenticationService Set singleton instance of [AuthenticationService] used for
 * authenticating users
 * @property fhirEngine Set [FhirEngine]
 * @property secureSharedPreference Set singleton of [SecureSharedPreference] used to access
 * encrypted shared preference data
 * @property resourceSyncParams Set [FhirEngine] resource sync params needed for syncing data from
 * the server
 * @property syncBroadcaster Set a singleton of [SyncBroadcaster] that reacts to FHIR sync
 * application level states
 */
interface ConfigurableApplication {

  val syncJob: SyncJob

  val applicationConfiguration: ApplicationConfiguration

  val authenticationService: AuthenticationService

  val fhirEngine: FhirEngine

  val secureSharedPreference: SecureSharedPreference

  val resourceSyncParams: Map<ResourceType, Map<String, String>>

  val syncBroadcaster: SyncBroadcaster
    get() = SyncBroadcaster

  val workerContextProvider: SimpleWorkerContext

  /** Provide [applicationConfiguration] for the Application */
  fun configureApplication(applicationConfiguration: ApplicationConfiguration)

  /**
   * Schedule periodic sync job. Should use the [syncJob] to schedule a task that will run
   * periodically
   */
  fun schedulePeriodicSync()
}
