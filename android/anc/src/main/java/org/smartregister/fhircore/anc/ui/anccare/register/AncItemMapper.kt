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

package org.smartregister.fhircore.anc.ui.anccare.register

import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.AncVisitStatus
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.overdue

data class Anc(val patient: Patient, val head: Patient?, val carePlans: List<CarePlan>)

object AncItemMapper : DomainMapper<Anc, AncPatientItem> {

  override fun mapToDomainModel(dto: Anc): AncPatientItem {
    val patient = dto.patient
    val name = patient.extractName()
    val gender = patient.extractGender(AncApplication.getContext())?.first() ?: ""
    val age = patient.extractAge()
    var visitStatus = AncVisitStatus.PLANNED

    if (dto.carePlans.any { it.overdue() }) visitStatus = AncVisitStatus.OVERDUE
    else if (dto.carePlans.any { it.due() }) visitStatus = AncVisitStatus.DUE

    return AncPatientItem(
      patientIdentifier = patient.logicalId,
      name = name,
      gender = gender.toString(),
      age = age,
      demographics = "$name, $gender, $age",
      address = if (dto.head == null) patient.extractAddress() else dto.head.extractAddress(),
      visitStatus = visitStatus
    )
  }
}
