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

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.util.LOGGED_IN_PRACTITIONER
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory

@HiltViewModel
@ExcludeFromJacocoGeneratedReport
class SettingsViewModel
@Inject
constructor(
  val syncBroadcaster: SyncBroadcaster,
  val accountAuthenticator: AccountAuthenticator,
  val secureSharedPreference: SecureSharedPreference,
  val sharedPreferences: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val keycloakService: KeycloakService,
  val fhirResourceService: FhirResourceService,
) : ViewModel() {

  val onLogout = MutableLiveData<Boolean?>(null)

  val language = MutableLiveData<Language?>(null)

  val data = MutableLiveData<ProfileData>()

  init {
    viewModelScope.launch { fetchData() }
  }

  private suspend fun fetchData() {
    var practitionerName: String? = null
    sharedPreferences.read(key = SharedPreferenceKey.PRACTITIONER_ID.name, defaultValue = null)
      ?.let {
        val practitioner = fhirEngine.get(ResourceType.Practitioner, it) as Practitioner
        practitionerName = practitioner.nameFirstRep.nameAsSingleString
      }

    val organizationIds =
      sharedPreferences.read<List<String>>(
          key = ResourceType.Organization.name,
          decodeWithGson = true
        )
        ?.map {
          val resource = (fhirEngine.get(ResourceType.Organization, it) as Organization)
          FieldData(resource.logicalId, resource.name)
        }

    val locationIds =
      sharedPreferences.read<List<String>>(key = ResourceType.Location.name, decodeWithGson = true)
        ?.map {
          val resource = (fhirEngine.get(ResourceType.Location, it) as Location)
          FieldData(resource.logicalId, resource.name)
        }

    val careTeamIds =
      sharedPreferences.read<List<String>>(key = ResourceType.CareTeam.name, decodeWithGson = true)
        ?.map {
          val resource = (fhirEngine.get(ResourceType.CareTeam, it) as CareTeam)
          FieldData(resource.logicalId, resource.name)
        }

    val isValid = organizationIds != null || locationIds != null || careTeamIds != null

    data.value =
      ProfileData(
        userName = practitionerName ?: "",
        organisations = organizationIds ?: listOf(),
        locations = locationIds ?: listOf(),
        careTeams = careTeamIds ?: listOf(),
        isUserValid = isValid,
        practitionerDetails = null
      )
  }

  fun runSync() {
    syncBroadcaster.runSync()
  }

  fun logoutUser(context: Context) {
    onLogout.postValue(true)
    accountAuthenticator.logout {
      context.getActivity()?.launchActivityWithNoBackStackHistory<LoginActivity>()
    }
  }

  fun retrieveUsername(): String? =
    sharedPreferences.read<Practitioner>(key = LOGGED_IN_PRACTITIONER, decodeWithGson = true)
      ?.nameFirstRep
      ?.nameAsSingleString

  fun fetchPractitionerDetails() {}
}
