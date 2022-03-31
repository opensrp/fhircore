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

package org.smartregister.fhircore.anc.ui.family.register

import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractDeathDate
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.engine.util.extension.overdue

data class Family(
  val family: Patient,
  val members: List<FamilyMemberItem>,
  val familyServicesDue: List<CarePlan>
)

class FamilyItemMapper
@Inject
constructor(
  @ApplicationContext val context: Context,
) : DataMapper<Family, FamilyItem> {

  override fun transformInputToOutputModel(inputModel: Family): FamilyItem {
    val family = inputModel.family
    val members = inputModel.members

    return FamilyItem(
      id = family.logicalId,
      identifier = family.identifierFirstRep.value,
      name = family.extractName(),
      address = family.extractAddress(),
      head = members.first { it.houseHoldHead },
      members = members,
      // TODO for now head in members list contains family services as well
      servicesDue = members.sumOf { it.servicesDue ?: 0 },
      servicesOverdue = members.sumOf { it.servicesOverdue ?: 0 }
    )
  }

  fun toFamilyMemberItem(
    member: Patient,
    conditions: List<Condition>? = null,
    servicesDue: List<CarePlan>? = null
  ): FamilyMemberItem {
    return FamilyMemberItem(
      name = member.extractName(),
      id = member.logicalId,
      birthdate = member.birthDate,
      gender = (member.extractGender(context)?.firstOrNull() ?: "").toString(),
      pregnant = conditions?.hasActivePregnancy(),
      houseHoldHead = member.isFamilyHead(),
      deathDate = member.extractDeathDate(),
      servicesDue =
        servicesDue?.filter { it.due() }?.flatMap { it.activity }?.filter { it.due() }?.size,
      servicesOverdue =
        servicesDue?.filter { it.due() }?.flatMap { it.activity }?.filter { it.overdue() }?.size
    )
  }
}
