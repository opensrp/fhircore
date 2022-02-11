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
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString

@Singleton
class PractitionerDetailsUtils
@Inject
constructor(val sharedPreferences: SharedPreferencesHelper, val fhirEngine: FhirEngine) {

  suspend fun getResourceFromPatientDetails(resourceType: String): List<Resource> {
    val practitionerCareTeams = arrayListOf<CareTeam>()
    val practitionerOrganizations = arrayListOf<Organization>()
    val practitionerLocations = arrayListOf<Location>()

    val practitionerDetails: Parameters =
      sharedPreferences.read(PARAMETERS_SHARED_PREFERENCE_KEY, "")!!.decodeResourceFromString()
    when (resourceType) {
      "CareTeam" ->
        practitionerDetails.parameter.forEach {
          if (it.name.equals("CareTeam")) {
            val result = it.resource as ListResource
            if (result.hasEntry()) {
              result.entry.forEach { entry ->
                practitionerCareTeams.add(
                  loadResource(
                    id = entry.item.reference,
                    stringToReplace = "CareTeam/",
                    clazz = CareTeam::class.java
                  )
                )
              }
            }
          }
        }
      "Organization" ->
        practitionerDetails.parameter.forEach {
          if (it.name.equals("Organization")) {
            val result = it.resource as ListResource
            if (result.hasEntry()) {
              result.entry.forEach { entry ->
                practitionerOrganizations.add(
                  loadResource(
                    id = entry.item.reference,
                    stringToReplace = "Organization/",
                    clazz = Organization::class.java
                  )
                )
              }
            }
          }
        }
      "Location" ->
        practitionerDetails.parameter.forEach {
          if (it.name.equals("Location")) {
            val result = it.resource as ListResource
            if (result.hasEntry()) {
              result.entry.forEach { entry ->
                practitionerLocations.add(
                  loadResource(
                    id = entry.item.reference,
                    stringToReplace = "Location/",
                    clazz = Location::class.java
                  )
                )
              }
            }
          }
        }
    }
    return when (resourceType) {
      "CareTeam" -> return practitionerCareTeams
      "Organization" -> return practitionerOrganizations
      "Location" -> return practitionerLocations
      else -> practitionerCareTeams
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
      name = "Practitioner"
      value = StringType(practitionerId)
    }
    if (careTeamList.isNotEmpty())
      parameters.addParameter().apply {
        name = "CaraTeam"
        resource =
          ListResource().apply {
            careTeamList.forEach { addEntry().apply { item = it.asReference() } }
          }
      }
    if (organizationList.isNotEmpty())
      parameters.addParameter().apply {
        name = "Organization"
        resource =
          ListResource().apply {
            organizationList.forEach { addEntry().apply { item = it.asReference() } }
          }
      }
    if (locationList.isNotEmpty())
      parameters.addParameter().apply {
        name = "Location"
        resource =
          ListResource().apply {
            locationList.forEach { addEntry().apply { item = it.asReference() } }
          }
      }
    sharedPreferences.write(PARAMETERS_SHARED_PREFERENCE_KEY, parameters.encodeResourceToString())
  }

  private suspend fun <T : Resource> loadResource(
    id: String,
    stringToReplace: String,
    clazz: Class<T>,
  ): T {
    return fhirEngine.load(clazz = clazz, id = id.replace(stringToReplace, ""))
  }
}
