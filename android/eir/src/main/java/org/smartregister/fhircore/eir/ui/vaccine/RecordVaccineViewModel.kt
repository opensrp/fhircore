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
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class RecordVaccineViewModel(
  application: Application,
  questionnaireConfig: QuestionnaireConfig,
  val patientRepository: PatientRepository,
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : QuestionnaireViewModel(application, questionnaireConfig) {

  fun getVaccineSummary(logicalId: String): LiveData<PatientVaccineSummary> {
    val mutableLiveData: MutableLiveData<PatientVaccineSummary> = MutableLiveData()
    viewModelScope.launch(dispatcherProvider.io()) {
      val immunizations = patientRepository.getPatientImmunizations(logicalId = logicalId)
      if (!immunizations.isNullOrEmpty()) {
        val immunization = immunizations.first()
        mutableLiveData.postValue(
          PatientVaccineSummary(
            doseNumber = (immunization.protocolApplied[0].doseNumber as PositiveIntType).value,
            initialDose = immunization.vaccineCode.coding.first().code
          )
        )
      } else mutableLiveData.postValue(PatientVaccineSummary(doseNumber = 0, initialDose = ""))
    }
    return mutableLiveData
  }

  override suspend fun getPopulationResources(intent: Intent): Array<Resource> {
    val resourcesList = mutableListOf<Resource>()

    intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)?.let { patientId ->
      loadPatient(patientId)?.run { resourcesList.add(this) }
      loadImmunization(patientId)?.run { resourcesList.add(this) }
    }

    return resourcesList.toTypedArray()
  }

  suspend fun loadImmunization(patientId: String): Immunization? {
    return defaultRepository.loadImmunizations(patientId)?.firstOrNull()
  }
}
