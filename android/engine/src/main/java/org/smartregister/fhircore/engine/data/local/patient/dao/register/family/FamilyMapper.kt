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

package org.smartregister.fhircore.engine.data.local.patient.dao.register.family

import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractDeathDate
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.engine.util.extension.overdue

data class Family(
  val family: Patient,
  val members: List<FamilyMember>,
  val servicesDue: List<CarePlan> = listOf()
)

data class FamilyMember(
  val patient: Patient,
  val conditions: List<Condition> = listOf(),
  val servicesDue: List<CarePlan> = listOf()
)

object FamilyMapper : DataMapper<Family, RegisterData.FamilyRegisterData> {

  override fun transformInputToOutputModel(inputModel: Family): RegisterData.FamilyRegisterData {
    val family = inputModel.family
    val members = inputModel.members.map { it.familyMemberRegisterData() }

    return RegisterData.FamilyRegisterData(
      id = family.logicalId,
      name = family.extractName(),
      identifier = family.identifierFirstRep.value,
      address = family.extractAddress(),
      head = members.first { it.id == family.logicalId },
      members = members,
      servicesDue = members.sumOf { it.servicesDue ?: 0 },
      servicesOverdue = members.sumOf { it.servicesOverdue ?: 0 }
    )
  }

  fun FamilyMember.familyMemberRegisterData(): RegisterData.FamilyMemberRegisterData {
    return RegisterData.FamilyMemberRegisterData(
      id = patient.logicalId,
      name = patient.extractName(),
      birthdate = patient.birthDate,
      gender = patient.gender.display.first().toString(),
      pregnant = conditions.hasActivePregnancy(),
      isHead = patient.isFamilyHead(),
      deathDate = patient.extractDeathDate(),
      servicesDue =
        servicesDue.filter { it.due() }.flatMap { it.activity }.filter { it.due() }.size,
      servicesOverdue =
        servicesDue.filter { it.due() }.flatMap { it.activity }.filter { it.overdue() }.size
    )
  }
}
