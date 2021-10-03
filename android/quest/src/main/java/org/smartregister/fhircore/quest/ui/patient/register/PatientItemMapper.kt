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

package org.smartregister.fhircore.quest.ui.patient.register

import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.data.patient.model.PatientItem

object PatientItemMapper : DomainMapper<Patient, PatientItem> {

  override fun mapToDomainModel(dto: Patient): PatientItem {
    val name = dto.extractName()
    val gender = dto.extractGender(QuestApplication.getContext())?.first() ?: ""
    val age = dto.extractAge()
    return PatientItem(
      id = dto.logicalId,
      identifier = dto.identifierFirstRep.value ?: "",
      name = name,
      gender = gender.toString(),
      age = age,
      address = dto.extractAddress()
    )
  }
}
