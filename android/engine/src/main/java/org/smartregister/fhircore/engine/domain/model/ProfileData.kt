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
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Task

// TODO convert to a sealed class to capture data for different health modules
sealed class ProfileData(open val id: String, open val name: String) {
  data class FamilyProfileData(
    override val id: String,
    override val name: String,
    val identifier: String? = null,
    val address: String,
    val head: FamilyMemberProfileData,
    val members: List<FamilyMemberProfileData>,
    val services: List<CarePlan> = listOf(),
    val tasks: List<Task> = listOf()
  ) : ProfileData(id = id, name = name)

  data class FamilyMemberProfileData(
    override val id: String,
    override val name: String,
    val identifier: String? = null,
    val birthdate: Date?,
    val gender: String,
    val isHead: Boolean,
    val pregnant: Boolean? = null,
    val deathDate: Date? = null,
    val conditions: List<Condition> = listOf(),
    val flags: List<Flag> = listOf(),
    val services: List<CarePlan> = listOf(),
    val tasks: List<Task> = listOf()
  ) : ProfileData(id = id, name = name)
}
