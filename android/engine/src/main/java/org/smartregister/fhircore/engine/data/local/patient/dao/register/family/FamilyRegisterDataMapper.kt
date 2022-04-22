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
import org.smartregister.fhircore.engine.data.local.patient.dao.register.DefaultPatientRegisterDao.Companion.OFFICIAL_IDENTIFIER
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.DAYS_IN_YEAR
import org.smartregister.fhircore.engine.util.extension.daysPassed
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractDeathDate
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.engine.util.extension.lastSeenFormat
import org.smartregister.fhircore.engine.util.extension.overdue

object FamilyRegisterDataMapper : DataMapper<FamilyDetail, RegisterData.FamilyRegisterData> {

  override fun transformInputToOutputModel(
    inputModel: FamilyDetail
  ): RegisterData.FamilyRegisterData {
    val family = inputModel.family
    val head = inputModel.head
    val members = inputModel.members.map { it.familyMemberRegisterData() }

    return RegisterData.FamilyRegisterData(
      id = family.logicalId,
      name = family.name,
      identifier =
        family.identifier.firstOrNull { it.use.name.contentEquals(OFFICIAL_IDENTIFIER) }?.value,
      address = head?.patient?.extractAddress() ?: "",
      head = head?.familyMemberRegisterData(),
      members = members,
      servicesDue = members.sumOf { it.servicesDue ?: 0 },
      servicesOverdue = members.sumOf { it.servicesOverdue ?: 0 },
      lastSeen = family.meta?.lastUpdated.lastSeenFormat()
    )
  }

  fun FamilyMemberDetail.familyMemberRegisterData(): RegisterData.FamilyMemberRegisterData {
    return RegisterData.FamilyMemberRegisterData(
      id = patient.logicalId,
      name = patient.extractName(),
      age = (patient.birthDate.daysPassed() / DAYS_IN_YEAR).toString(),
      birthdate = patient.birthDate,
      gender = patient.gender.display.first().toString(),
      pregnant = conditions.hasActivePregnancy(),
      isHead = patient.isFamilyHead(),
      deathDate = patient.extractDeathDate(),
      servicesDue = servicesDue.filter { it.due() }.flatMap { it.activity }.size,
      servicesOverdue = servicesDue.filter { it.overdue() }.flatMap { it.activity }.size
    )
  }
}
