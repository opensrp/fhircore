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

package org.smartregister.fhircore.engine.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.testing.jsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.practitionerEndpointUrl
import org.smartregister.model.practitioner.PractitionerDetails

@HiltViewModel
class DevViewModel
@Inject
constructor(
  val syncBroadcaster: SyncBroadcaster,
  val accountAuthenticator: AccountAuthenticator,
  val secureSharedPreference: SecureSharedPreference,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val keycloakService: KeycloakService,
  val fhirResourceService: FhirResourceService,
) : ViewModel() {

  fun fetchDetails() {
    try {
      viewModelScope.launch {
        val userInfo = keycloakService.fetchUserInfo().body()
        if (userInfo != null && !userInfo.keycloakUuid.isNullOrEmpty()) {
          val bundle =
            fhirResourceService.getResource(url = userInfo.keycloakUuid!!.practitionerEndpointUrl())
          val practitionerDetails = bundle.entry.first().resource as PractitionerDetails

          val data = jsonParser.encodeResourceToString(practitionerDetails)
          println(data)
        }
      }
    } catch (e: Exception) {
      println(e)
    }
  }
}
