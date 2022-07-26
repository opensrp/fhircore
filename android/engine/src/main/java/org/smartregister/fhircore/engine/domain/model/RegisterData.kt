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

import java.util.Date
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Reference

sealed class RegisterData(open val logicalId: String, open val name: String) {
  data class DefaultRegisterData(
    override val logicalId: String,
    override val name: String,
    val gender: Enumerations.AdministrativeGender,
    val age: String
  ) : RegisterData(logicalId = logicalId, name = name)

  data class FamilyRegisterData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val address: String,
    val head: FamilyMemberRegisterData? = null,
    val members: List<FamilyMemberRegisterData> = emptyList(),
    val servicesDue: Int? = null,
    val servicesOverdue: Int? = null,
    val lastSeen: String? = null
  ) : RegisterData(logicalId = logicalId, name = name)

  data class FamilyMemberRegisterData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val age: String,
    val birthdate: Date?,
    val gender: String,
    val isHead: Boolean,
    val pregnant: Boolean = false,
    val deathDate: Date? = null,
    val servicesDue: Int? = null,
    val servicesOverdue: Int? = null
  ) : RegisterData(logicalId = logicalId, name = name)

  data class AncRegisterData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val gender: Enumerations.AdministrativeGender,
    val age: String,
    val address: String,
    val visitStatus: VisitStatus,
    val servicesDue: Int? = null,
    val servicesOverdue: Int? = null,
    val familyName: String? = null
  ) : RegisterData(logicalId = logicalId, name = name)

  data class HivRegisterData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val gender: Enumerations.AdministrativeGender,
    val age: String,
    val address: String,
    val familyName: String? = null,
    val phoneContacts: List<String>? = null,
    val practitioners: List<Reference>? = null,
    val chwAssigned: String,
    val healthStatus: HealthStatus,
    val isPregnant: Boolean = false,
  ) : RegisterData(logicalId = logicalId, name = name)

  data class AppointmentRegisterData(
    override val logicalId: String,
    override val name: String,
    val identifier: String? = null,
    val gender: Enumerations.AdministrativeGender,
    val age: String,
    val address: String,
    val familyName: String? = null,
    val phoneContacts: List<String>? = null,
    val practitioners: List<Reference>? = null,
    val chwAssigned: String
  ) : RegisterData(logicalId = logicalId, name = name)
}
