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

package org.smartregister.fhircore.engine.data.local.patient.dao.register

import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractDeathDate
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.isFamilyHead

data class FamilyDetail(
  val family: Patient,
  val members: List<FamilyMemberDetail>,
  val servicesDue: List<CarePlan> = listOf(),
  val tasks: List<Task> = listOf()
)

data class FamilyMemberDetail(
  val patient: Patient,
  val conditions: List<Condition> = listOf(),
  val flags: List<Flag> = listOf(),
  val servicesDue: List<CarePlan> = listOf(),
  val tasks: List<Task> = listOf()
)

object FamilyProfileMapper : DataMapper<FamilyDetail, ProfileData.FamilyProfileData> {

  override fun transformInputToOutputModel(
    inputModel: FamilyDetail
  ): ProfileData.FamilyProfileData {
    val family = inputModel.family
    val members = inputModel.members.map { it.familyMemberProfileData() }

    return ProfileData.FamilyProfileData(
      id = family.logicalId,
      name = family.extractName(),
      identifier = family.identifierFirstRep.value,
      address = family.extractAddress(),
      head = members.first { it.id == family.logicalId },
      members = members,
      services = inputModel.servicesDue,
      tasks = inputModel.tasks
    )
  }

  fun FamilyMemberDetail.familyMemberProfileData(): ProfileData.FamilyMemberProfileData {
    return ProfileData.FamilyMemberProfileData(
      id = patient.logicalId,
      name = patient.extractName(),
      birthdate = patient.birthDate,
      gender = patient.gender.display.first().toString(),
      pregnant = conditions.hasActivePregnancy(),
      isHead = patient.isFamilyHead(),
      deathDate = patient.extractDeathDate(),
      conditions = conditions,
      flags = flags,
      services = servicesDue
    )
  }
}
