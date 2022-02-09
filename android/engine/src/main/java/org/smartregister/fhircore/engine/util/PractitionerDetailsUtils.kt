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
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.loadResourceTemplate
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
      practitionerDetails.parameter.forEach {
        if (it.name.equals("CareTeam")) {
          val careTeam = fhirEngine.load(CareTeam::class.java, it.id)
          practitionerCareTeams.add(careTeam)
        } else if (it.name.equals("Organization")) {
          val organization = fhirEngine.load(Organization::class.java, it.id)
          practitionerOrganizations.add(organization)
        } else if (it.name.equals("Location")) {
          val location = fhirEngine.load(Location::class.java, it.id)
          practitionerLocations.add(location)
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
    locationHierarchyList: Location,
  ): Parameters {
    val parts = arrayListOf<Parameters.ParametersParameterComponent>()
    if (careTeamList.isNotEmpty())
      careTeamList.forEach { careTeam ->
        val part = Parameters.ParametersParameterComponent()
        part.apply {
          this.name = "CareTeam"
          this.id = careTeam.id
        }
        parts.add(part)
      }
    if (organizationList.isNotEmpty())
      organizationList.forEach { organization ->
        val part = Parameters.ParametersParameterComponent()
        part.apply {
          this.name = "Organization"
          this.id = organization.id
        }
        parts.add(part)
      }
    if (!locationHierarchyList.isEmpty) {
      val part = Parameters.ParametersParameterComponent()
      part.apply {
        this.name = "Location"
        this.id = locationHierarchyList.id
      }
      parts.add(part)
    }
    return recordParameters(practitionerId = practitionerId, parts = parts)
  }

  private fun recordParameters(
    practitionerId: String,
    parts: List<Parameters.ParametersParameterComponent>
  ): Parameters {
    val config = buildConfigData(practitionerId = practitionerId, parts = parts)
    val parameters = loadConfig(Template.PARAMETERS, clazz = Parameters::class.java, data = config)
    return parameters
  }

  private fun buildConfigData(
    practitionerId: String,
    parts: List<Parameters.ParametersParameterComponent>
  ): Map<String, String?> {
    return mapOf("#idPractitioner" to practitionerId, "#parts" to parts.encodeJson())
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
