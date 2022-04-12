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
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Task

data class FamilyMemberProfileData(
  val id: String,
  val name: String,
  val identifier: String? = null,
  val birthdate: Date?,
  val age: String,
  val gender: Enumerations.AdministrativeGender,
  val isHead: Boolean,
  val pregnant: Boolean? = null,
  val deathDate: Date? = null,
  val conditions: List<Condition> = listOf(),
  val flags: List<Flag> = listOf(),
  val services: List<CarePlan> = listOf(),
  val tasks: List<Task> = listOf()
)
