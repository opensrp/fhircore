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

import android.app.Application
import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.overdue
import javax.inject.Inject

data class Anc(val patient: Patient, val head: Patient?, val carePlans: List<CarePlan>)

class AncItemMapper @Inject constructor(
  @ApplicationContext val context: Context
  ) : DomainMapper<Anc, PatientItem> {

  override fun mapToDomainModel(dto: Anc): PatientItem {
    val patient = dto.patient
    val name = patient.extractName()
    val gender = patient.extractGender(context)?.first() ?: ""
    val age = patient.extractAge()
    var visitStatus = VisitStatus.PLANNED

    if (dto.carePlans.flatMap { it.activity }.any { it.detail.overdue() })
      visitStatus = VisitStatus.OVERDUE
    else if (dto.carePlans.flatMap { it.activity }.any { it.detail.due() })
      visitStatus = VisitStatus.DUE

    return PatientItem(
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
