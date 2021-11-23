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

import android.app.Application
import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.isPregnant
import org.smartregister.fhircore.engine.util.extension.overdue
import javax.inject.Inject
import javax.inject.Singleton

data class Family(val head: Patient, val members: List<Patient>, val servicesDue: List<CarePlan>)

class FamilyItemMapper @Inject constructor(
  @ApplicationContext val context: Context,
  ) : DomainMapper<Family, FamilyItem> {

  override fun mapToDomainModel(dto: Family): FamilyItem {
    val head = dto.head
    val members = dto.members
    val servicesDue = dto.servicesDue

    return FamilyItem(
      id = head.logicalId,
      identifier = head.identifierFirstRep.value,
      name = head.extractName(),
      gender = (head.extractGender(context)?.firstOrNull() ?: "").toString(),
      age = head.extractAge(),
      address = head.extractAddress(),
      isPregnant = head.isPregnant(),
      members = members.map { toFamilyMemberItem(it) },
      servicesDue = servicesDue.flatMap { it.activity }.filter { it.detail.due() }.size,
      servicesOverdue = servicesDue.flatMap { it.activity }.filter { it.detail.overdue() }.size
    )
  }

  fun toFamilyMemberItem(member: Patient): FamilyMemberItem {
    return FamilyMemberItem(
      name = member.extractName(),
      id = member.logicalId,
      age = member.extractAge(),
      gender = (member.extractGender(context)?.firstOrNull() ?: "").toString(),
      pregnant = member.isPregnant()
    )
  }
}
