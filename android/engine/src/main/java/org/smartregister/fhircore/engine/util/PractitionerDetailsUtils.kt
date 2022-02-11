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
import org.hl7.fhir.r4.model.ResourceType
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
      ResourceType.CareTeam.name ->
        practitionerDetails.parameter.forEach {
          if (it.name.equals(ResourceType.CareTeam.name)) {
            val result = it.resource as ListResource
            if (result.hasEntry()) {
              result.entry.forEach { entry ->
                practitionerCareTeams.add(
                  loadResource(
                    id = entry.item.reference,
                    stringToReplace = "${ResourceType.CareTeam.name}/",
                    clazz = CareTeam::class.java
                  )
                )
              }
            }
          }
        }
      ResourceType.Organization.name ->
        practitionerDetails.parameter.forEach {
          if (it.name.equals(ResourceType.Organization.name)) {
            val result = it.resource as ListResource
            if (result.hasEntry()) {
              result.entry.forEach { entry ->
                practitionerOrganizations.add(
                  loadResource(
                    id = entry.item.reference,
                    stringToReplace = "${ResourceType.Organization.name}/",
                    clazz = Organization::class.java
                  )
                )
              }
            }
          }
        }
      ResourceType.Location.name ->
        practitionerDetails.parameter.forEach {
          if (it.name.equals(ResourceType.Location.name)) {
            val result = it.resource as ListResource
            if (result.hasEntry()) {
              result.entry.forEach { entry ->
                practitionerLocations.add(
                  loadResource(
                    id = entry.item.reference,
                    stringToReplace = "${ResourceType.Location.name}/",
                    clazz = Location::class.java
                  )
                )
              }
            }
          }
        }
    }
    return when (resourceType) {
      ResourceType.CareTeam.name -> return practitionerCareTeams
      ResourceType.Organization.name -> return practitionerOrganizations
      ResourceType.Location.name -> return practitionerLocations
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
      name = ResourceType.Practitioner.name
      value = StringType(practitionerId)
    }
    if (careTeamList.isNotEmpty())
      parameters.addParameter().apply {
        name = ResourceType.CareTeam.name
        resource =
          ListResource().apply {
            careTeamList.forEach { addEntry().apply { item = it.asReference() } }
          }
      }
    if (organizationList.isNotEmpty())
      parameters.addParameter().apply {
        name = ResourceType.Organization.name
        resource =
          ListResource().apply {
            organizationList.forEach { addEntry().apply { item = it.asReference() } }
          }
      }
    if (locationList.isNotEmpty())
      parameters.addParameter().apply {
        name = ResourceType.Location.name
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
