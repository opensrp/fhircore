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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.PositiveIntType
import org.smartregister.fhircore.engine.data.local.repository.model.PatientVaccineSummary
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientRepository

class RecordVaccineViewModel(application: Application, val patientRepository: PatientRepository) :
  AndroidViewModel(application) {

  fun getVaccineSummary(logicalId: String): LiveData<PatientVaccineSummary> {
    val mutableLiveData: MutableLiveData<PatientVaccineSummary> = MutableLiveData()
    viewModelScope.launch {
      val immunizations = patientRepository.getPatientImmunizations(logicalId = logicalId)
      if (!immunizations.isNullOrEmpty()) {
        val immunization = immunizations.first()
        mutableLiveData.value =
          PatientVaccineSummary(
            doseNumber = (immunization.protocolApplied[0].doseNumber as PositiveIntType).value,
            initialDose = immunization.vaccineCode.coding.first().code
          )
      }
    }
    return mutableLiveData
  }
}
