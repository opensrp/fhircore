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

import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.eir.data.model.PatientItem
import org.smartregister.fhircore.eir.data.model.PatientVaccineStatus
import org.smartregister.fhircore.eir.data.model.VaccineStatus
import org.smartregister.fhircore.eir.ui.patient.details.isOverdue
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.atRisk
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.getLastSeen
import org.smartregister.fhircore.engine.util.extension.toDisplay

class PatientItemMapper @Inject constructor(@ApplicationContext val context: Context) :
  DataMapper<Pair<Patient, List<Immunization>>, PatientItem> {

  override fun transformInputToOutputModel(
    inputModel: Pair<Patient, List<Immunization>>
  ): PatientItem {
    val (patient, immunizations) = inputModel
    val name = patient.extractName()
    val gender = patient.extractGender(context)?.first() ?: ""
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
    val computedStatus =
      if (this.size >= 2) VaccineStatus.VACCINATED
      else if (this.size == 1 && this[0].isOverdue()) VaccineStatus.OVERDUE
      else if (this.size == 1) VaccineStatus.PARTIAL else VaccineStatus.DUE

    val date = if (this.isNullOrEmpty()) "" else this[0].occurrenceDateTimeType.toDisplay()
    return PatientVaccineStatus(status = computedStatus, date = date)
  }
}
