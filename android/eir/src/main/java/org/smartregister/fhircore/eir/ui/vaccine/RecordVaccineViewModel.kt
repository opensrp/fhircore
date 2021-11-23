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

package org.smartregister.fhircore.eir.ui.vaccine

import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.data.model.PatientVaccineSummary
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class RecordVaccineViewModel(
  application: Application,
  val patientRepository: PatientRepository,
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : QuestionnaireViewModel(application) {

  suspend fun loadLatestVaccine(patientId: String): PatientVaccineSummary? {
    val lastImmunization =
      patientRepository
        .getPatientImmunizations(patientId)
        // take only if received datetime is assigned to vaccine otherwise consider invalid
        .filter { it.hasOccurrenceDateTimeType() }
        .maxByOrNull { it.occurrenceDateTimeType.value }
        ?: return null

    return PatientVaccineSummary(
      doseNumber = lastImmunization.protocolAppliedFirstRep.doseNumberPositiveIntType?.value ?: 0,
      initialDose = lastImmunization.vaccineCode.coding.first().code
    )
  }

  override suspend fun getPopulationResources(intent: Intent): Array<Resource> {
    val resourcesList = mutableListOf<Resource>()

    intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)?.let { patientId ->
      loadPatient(patientId)?.run { resourcesList.add(this) }
      loadPatientImmunization(patientId)?.run { resourcesList.add(this) }
    }

    return resourcesList.toTypedArray()
  }

  suspend fun loadPatientImmunization(patientId: String): Immunization? {
    return defaultRepository.loadPatientImmunizations(patientId)?.firstOrNull()
  }
}
