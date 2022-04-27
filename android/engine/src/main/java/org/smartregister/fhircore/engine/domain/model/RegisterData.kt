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

sealed class RegisterData(open val id: String, open val name: String) {
  data class DefaultRegisterData(
    override val id: String,
    override val name: String,
    val gender: Enumerations.AdministrativeGender,
    val age: String
  ) : RegisterData(id = id, name = name)

  data class FamilyRegisterData(
    override val id: String,
    override val name: String,
    val identifier: String? = null,
    val address: String,
    val head: FamilyMemberRegisterData? = null,
    val members: List<FamilyMemberRegisterData> = emptyList(),
    val servicesDue: Int? = null,
    val servicesOverdue: Int? = null,
    val lastSeen: String? = null
  ) : RegisterData(id = id, name = name)

  data class FamilyMemberRegisterData(
    override val id: String,
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
  ) : RegisterData(id = id, name = name)

  data class AncRegisterData(
    override val id: String,
    override val name: String,
    val identifier: String? = null,
    val age: String,
    val address: String,
    val visitStatus: VisitStatus,
    val servicesDue: Int? = null,
    val servicesOverdue: Int? = null
  ) : RegisterData(id = id, name = name)
}
