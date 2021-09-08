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

package org.smartregister.fhircore.eir.ui.patient.register

import com.google.android.fhir.logicalId
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.data.model.PatientItem
import org.smartregister.fhircore.eir.data.model.PatientVaccineStatus
import org.smartregister.fhircore.eir.data.model.VaccineStatus
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.extension.atRisk
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.getLastSeen

object PatientItemMapper : DomainMapper<Pair<Patient, List<Immunization>>, PatientItem> {

  override fun mapToDomainModel(dto: Pair<Patient, List<Immunization>>): PatientItem {
    val (patient, immunizations) = dto
    val name = patient.extractName()
    val gender = patient.extractGender(EirApplication.getContext()).first()
    val age = patient.extractAge()
    return PatientItem(
      patientIdentifier = patient.logicalId,
      name = name,
      gender = gender.toString(),
      age = age,
      demographics = "$name, $gender, $age",
      lastSeen = patient.getLastSeen(immunizations),
      vaccineStatus = immunizations.getVaccineStatus(),
      atRisk = patient.atRisk()
    )
  }

  private fun List<Immunization>.getVaccineStatus(): PatientVaccineStatus {
    val calendar: Calendar = Calendar.getInstance()
    calendar.add(Calendar.DATE, -28)
    val overDueStart: Date = calendar.time
    val formatter = SimpleDateFormat("dd-MM-yy", Locale.US)
    val computedStatus =
      if (this.size >= 2) VaccineStatus.VACCINATED
      else if (this.size == 1 && this[0].recorded.before(overDueStart)) VaccineStatus.OVERDUE
      else if (this.size == 1) VaccineStatus.PARTIAL else VaccineStatus.DUE

    return PatientVaccineStatus(
      status = computedStatus,
      date = if (this.isNotEmpty()) formatter.format(this[0].recorded) else ""
    )
  }
}
