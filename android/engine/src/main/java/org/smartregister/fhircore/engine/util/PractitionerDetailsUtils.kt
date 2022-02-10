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

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.util.extension.loadResourceTemplate
import org.smartregister.fhircore.engine.util.extension.valueToString
import timber.log.Timber

@Singleton
class PractitionerDetailsUtils
@Inject
constructor(@ApplicationContext val context: Context, val fhirEngine: FhirEngine) {

  suspend fun getResourceFromPatientDetails(
    practitionerId: String,
    resourceType: String
  ): List<Resource> {
    val practitionerDetails: Parameters
    val practitionerCareTeams = arrayListOf<CareTeam>()
    val practitionerOrganizations = arrayListOf<Organization>()
    val practitionerLocations = arrayListOf<Location>()

    try {
      practitionerDetails = fhirEngine.load(Parameters::class.java, practitionerId)
      when (resourceType) {
        "careTeams" ->
          practitionerDetails.parameter.forEach {
            if (it.name.equals("CareTeam")) {
              val result = it.value.valueToString().split(",").map { id -> id.trim() }
              if (result.isNotEmpty()) {
                result.forEach { id ->
                  val careTeam = fhirEngine.load(CareTeam::class.java, id)
                  practitionerCareTeams.add(careTeam)
                }
              }
            }
          }
        "organization" ->
          practitionerDetails.parameter.forEach {
            if (it.name.equals("Organization")) {
              val result = it.value.valueToString().split(",").map { id -> id.trim() }
              if (result.isNotEmpty()) {
                result.forEach { id ->
                  val organization = fhirEngine.load(Organization::class.java, it.id)
                  practitionerOrganizations.add(organization)
                }
              }
            }
          }
        "locations" ->
          practitionerDetails.parameter.forEach {
            if (it.name.equals("Location")) {
              val result = it.value.valueToString().split(",").map { id -> id.trim() }
              if (result.isNotEmpty()) {
                result.forEach { id ->
                  val location = fhirEngine.load(Location::class.java, it.id)
                  practitionerLocations.add(location)
                }
              }
            }
          }
      }
    } catch (e: ResourceNotFoundException) {
      Timber.e(e)
    }
    return when (resourceType) {
      "careTeams" -> return practitionerCareTeams
      "organization" -> return practitionerOrganizations
      "locations" -> return practitionerLocations
      else -> practitionerCareTeams
    }
  }

  fun saveParameter(
    practitionerId: String,
    careTeamList: List<CareTeam>,
    organizationList: List<Organization>,
    locationList: List<Location>,
  ): Parameters {
    val carePlanIds = arrayListOf<String>()
    val organizationIds = arrayListOf<String>()
    val locationsIds = arrayListOf<String>()
    if (careTeamList.isNotEmpty()) careTeamList.forEach { careTeam -> carePlanIds.add(careTeam.id) }
    if (organizationList.isNotEmpty())
      organizationList.forEach { organization -> organizationIds.add(organization.id) }
    if (locationList.isNotEmpty())
      organizationList.forEach { location -> locationsIds.add(location.id) }
    val carePlanIdsString = if (carePlanIds.isNotEmpty()) carePlanIds.joinToString { it } else ""
    val organizationIdsString =
      if (organizationIds.isNotEmpty()) organizationIds.joinToString { it } else ""
    val locationIdsString = if (locationsIds.isNotEmpty()) locationsIds.joinToString { it } else ""
    return recordParameters(
      practitionerId = practitionerId,
      carePlanIds = carePlanIdsString,
      organizationIds = organizationIdsString,
      locationIds = locationIdsString
    )
  }

  private fun recordParameters(
    practitionerId: String,
    carePlanIds: String,
    organizationIds: String,
    locationIds: String
  ): Parameters {
    val config =
      buildConfigData(
        practitionerId = practitionerId,
        carePlanIds = carePlanIds,
        organizationIds = organizationIds,
        locationIds = locationIds
      )
    val parameters = loadConfig(Template.PARAMETERS, clazz = Parameters::class.java, data = config)
    return parameters
  }

  private fun buildConfigData(
    practitionerId: String,
    carePlanIds: String,
    organizationIds: String,
    locationIds: String
  ): Map<String, String?> {

    return mapOf(
      "#id" to UUID.randomUUID().toString(),
      "#idPractitioner" to practitionerId,
      "#idPartsCP" to carePlanIds,
      "idPartsO" to organizationIds,
      "idPartsL" to locationIds
    )
  }

  private fun <T : Resource> loadConfig(
    id: String,
    clazz: Class<T>,
    data: Map<String, String?> = emptyMap()
  ): T {
    return context.loadResourceTemplate(id, clazz, data)
  }

  companion object {
    object Template {
      const val PARAMETERS = "prameters_template.json"
    }
  }
}
