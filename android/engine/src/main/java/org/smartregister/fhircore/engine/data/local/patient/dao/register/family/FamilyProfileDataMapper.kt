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
import org.smartregister.fhircore.engine.domain.model.FamilyMemberProfileData
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractDeathDate
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

object FamilyProfileDataMapper : DataMapper<FamilyDetail, ProfileData.FamilyProfileData> {

  override fun transformInputToOutputModel(
    inputModel: FamilyDetail
  ): ProfileData.FamilyProfileData {
    val family = inputModel.family
    val head = inputModel.head
    val members = inputModel.members.map { it.familyMemberProfileData() }

    return ProfileData.FamilyProfileData(
      id = family.logicalId,
      name = family.name,
      identifier =
        family.identifier.firstOrNull { it.use.name.contentEquals(OFFICIAL_IDENTIFIER) }?.value,
      address = head?.patient?.extractAddress() ?: "",
      head = head?.familyMemberProfileData(),
      members = members,
      services = inputModel.servicesDue,
      tasks = inputModel.tasks
    )
  }

  fun FamilyMemberDetail.familyMemberProfileData(): FamilyMemberProfileData {
    return FamilyMemberProfileData(
      id = patient.logicalId,
      name = patient.extractName(),
      age = patient.birthDate.toAgeDisplay(),
      birthdate = patient.birthDate,
      gender = patient.gender,
      pregnant = conditions.hasActivePregnancy(),
      isHead = patient.isFamilyHead(),
      deathDate = patient.extractDeathDate(),
      conditions = conditions,
      flags = flags,
      services = servicesDue
    )
  }
}
