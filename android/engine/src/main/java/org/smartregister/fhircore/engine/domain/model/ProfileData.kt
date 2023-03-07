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

package org.smartregister.fhircore.engine.domain.model

import org.hl7.fhir.r4.model.*
import java.util.Date
import org.smartregister.fhircore.engine.data.domain.Guardian
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

sealed class ProfileData(open val logicalId: String, open val name: String) {
  data class DefaultProfileData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val birthdate: Date,
    val age: String = birthdate.toAgeDisplay(),
    val address: String,
    val gender: Enumerations.AdministrativeGender,
    val deathDate: Date? = null,
    val deceased: Boolean? = deathDate?.let { true },
    val conditions: List<Condition> = listOf(),
    val flags: List<Flag> = listOf(),
    val services: List<CarePlan> = listOf(),
    val tasks: List<Task> = listOf(),
    val visits: List<Encounter> = listOf(),
    val forms: List<QuestionnaireConfig> = listOf(),
    val responses: List<QuestionnaireResponse> = listOf()
  ) : ProfileData(logicalId = logicalId, name = name)

  data class FamilyProfileData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val address: String,
    val age: String,
    val head: FamilyMemberProfileData? = null,
    val members: List<FamilyMemberProfileData>,
    val services: List<CarePlan> = listOf(),
    val tasks: List<Task> = listOf()
  ) : ProfileData(logicalId = logicalId, name = name)

  data class AncProfileData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val birthdate: Date,
    val age: String,
    val gender: Enumerations.AdministrativeGender,
    val address: String,
    val visitStatus: VisitStatus,
    val conditions: List<Condition> = listOf(),
    val flags: List<Flag> = listOf(),
    val services: List<CarePlan> = listOf(),
    val tasks: List<Task> = listOf(),
    val visits: List<Encounter> = listOf()
  ) : ProfileData(logicalId = logicalId, name = name)

  data class HivProfileData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val givenName: String = "",
    val familyName: String = "",
    val birthdate: Date,
    val age: String = birthdate.toAgeDisplay(),
    val gender: Enumerations.AdministrativeGender,
    val address: String,
    val addressDistrict: String = "",
    val addressTracingCatchment: String = "",
    val addressPhysicalLocator: String = "",
    val services: List<CarePlan> = listOf(),
    val tasks: List<Task> = listOf(),
    val chwAssigned: Reference,
    val healthStatus: HealthStatus,
    val phoneContacts: List<String> = listOf(),
    val showIdentifierInProfile: Boolean = false,
    val conditions: List<Condition> = emptyList(),
    val otherPatients: List<Resource> = listOf(),
    val guardians: List<Guardian> = emptyList(),
    val practitioners: List<Practitioner> = emptyList()
  ) : ProfileData(logicalId = logicalId, name = name)

  data class TracingProfileData(
    override val logicalId: String,
    override val name: String,
    val birthdate: Date,
    val address: String,
    val chwAssigned: Reference,
    val healthStatus: HealthStatus,
    val age: String = birthdate.toAgeDisplay(),
    val gender: Enumerations.AdministrativeGender,
    val addressDistrict: String = "",
    val addressTracingCatchment: String = "",
    val addressPhysicalLocator: String = "",
    val services: List<CarePlan> = listOf(),
    val tasks: List<Task> = listOf(),
    val phoneContacts: List<String> = listOf(),
    val showIdentifierInProfile: Boolean = false,
    val conditions: List<Condition> = emptyList(),
    val guardians: List<Guardian> = emptyList(),
    val practitioners: List<Practitioner> = emptyList()
  ) : ProfileData(logicalId = logicalId, name = name)
}
