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

package org.smartregister.fhircore.engine.util

import com.google.android.fhir.FhirEngine
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.model.practitioner.KeycloakUserDetails
import org.smartregister.model.practitioner.PractitionerDetails

@Singleton
class PractitionerDetailsUtils
@Inject
constructor(
  val sharedPreferences: SharedPreferencesHelper,
  val fhirEngine: FhirEngine,
  val gson: Gson,
  val userInfoItemMapper: UserInfoItemMapper
) {

  suspend fun getResourceFromPractitionerDetails(resourceType: String): List<Resource> {
    val practitionerDetails: Parameters =
      sharedPreferences.read(PRACTITIONER_PARAMETERS_SHARED_PREFERENCE_KEY, "")!!
        .decodeResourceFromString()
    return when (resourceType) {
      ResourceType.CareTeam.name ->
        return getResourcesList(
          practitionerDetails,
          ResourceType.CareTeam.name,
          CareTeam::class.java
        )
      ResourceType.Organization.name ->
        return getResourcesList(
          practitionerDetails,
          ResourceType.Organization.name,
          Organization::class.java
        )
      ResourceType.Location.name ->
        return getResourcesList(
          practitionerDetails,
          ResourceType.Location.name,
          Location::class.java
        )
      else -> mutableListOf()
    }
  }

  suspend fun <T : Resource> getResourcesList(
    practitionerDetails: Parameters,
    resourceName: String,
    clazz: Class<T>
  ): List<T> {
    return practitionerDetails
      .parameter
      .filter { parameterComponent ->
        parameterComponent.hasName() &&
          parameterComponent.name.equals(resourceName, ignoreCase = true) &&
          parameterComponent.resource is ListResource
      }
      .flatMap { (it.resource as ListResource).entry }
      .map { entryComponent ->
        fhirEngine.load(
          clazz = clazz,
          id = entryComponent.item.reference.replace("$resourceName/", "")
        )
      }
  }

  fun saveParameter(
    practitionerId: String,
    careTeamList: List<CareTeam>,
    organizationList: List<Organization>,
    locationList: List<Location>,
  ) {
    val parameters = Parameters()
    parameters.addParameter().apply {
      name = ResourceType.Practitioner.name
      value = StringType(practitionerId)
    }
    addParameters(careTeamList, parameters, ResourceType.CareTeam.name)
    addParameters(organizationList, parameters, ResourceType.Organization.name)
    addParameters(locationList, parameters, ResourceType.Location.name)

    sharedPreferences.write(
      PRACTITIONER_PARAMETERS_SHARED_PREFERENCE_KEY,
      parameters.encodeResourceToString()
    )
  }

  fun <T : Resource> addParameters(
    resources: List<T>,
    parameters: Parameters,
    resourceName: String
  ) {
    if (resources.isNotEmpty())
      parameters.addParameter().apply {
        name = resourceName
        resource =
          ListResource().apply {
            resources.forEach { addEntry().apply { item = it.asReference() } }
          }
      }
  }

  fun storeUserPreferences(userInfo: UserInfo) {
    sharedPreferences.write(USER_INFO_SHARED_PREFERENCE_KEY, userInfo.encodeJson())
  }

  fun updateUserDetailsFromPractitionerDetails(
    practitionerDetails: PractitionerDetails,
    userResponse: UserInfo
  ) {
    storeUserPreferences(userInfo = userInfoItemMapper.mapToDomainModel(practitionerDetails))
  }

  fun storeKeyClockInfo(practitionerDetails: PractitionerDetails) {
    val userData = practitionerDetails.userDetail as KeycloakUserDetails
    val json = gson.toJson(userData)
    sharedPreferences.write(KEY_CLOCK_INFO_SHARED_PREFERENCE_KEY, json)
  }

  fun retrieveKeyClockInfo(): KeycloakUserDetails {
    val keycloakUserDetailsString =
      sharedPreferences.read(KEY_CLOCK_INFO_SHARED_PREFERENCE_KEY, "")!!
    return gson.fromJson(keycloakUserDetailsString, KeycloakUserDetails::class.java)
  }
}
