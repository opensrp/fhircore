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
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.util.extension.loadResourceTemplate
import org.smartregister.model.practitioner.FhirCareTeamExtension
import org.smartregister.model.practitioner.FhirOrganizationExtension
import org.smartregister.model.practitioner.PractitionerDetails
import timber.log.Timber

@Singleton
class PractitionerDetailsUtils
@Inject
constructor(@ApplicationContext val context: Context, val fhirEngine: FhirEngine) {

  suspend fun getResourceFromPatientDetails(
    practitionerId: String,
    resourceType: String
  ): List<Resource> {
    var practitionerDetails = PractitionerDetails()

    try {
      practitionerDetails = fhirEngine.load(PractitionerDetails::class.java, practitionerId)
    } catch (e: ResourceNotFoundException) {
      Timber.e(e)
    }
    return when (resourceType) {
      "careTeams" -> return practitionerDetails.fhirPractitionerDetails.fhirCareTeamExtensionList
      "organization" ->
        return practitionerDetails.fhirPractitionerDetails.fhirOrganizationExtensions
      "locations" -> return practitionerDetails.fhirPractitionerDetails.locationHierarchyList
      else -> practitionerDetails.fhirPractitionerDetails.fhirOrganizationExtensions
    }
  }

  fun saveParameter(
    practitionerId: String,
    careTeamList: List<FhirCareTeamExtension>,
    organizationList: List<FhirOrganizationExtension>,
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
    return mapOf("#idPractitioner" to practitionerId, "#parts" to parts.toString())
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
