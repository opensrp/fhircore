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

package org.smartregister.fhircore.quest.data.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.engine.util.extension.searchActivePatients
import org.smartregister.fhircore.quest.data.patient.model.PatientItem

class PatientRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Patient, PatientItem>,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Patient, PatientItem> {

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<PatientItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.searchActivePatients(query = query, pageNumber = pageNumber, loadAll = loadAll)

      patients.map { domainMapper.mapToDomainModel(it) }
    }
  }

  override suspend fun countAll(): Long =
    withContext(dispatcherProvider.io()) { fhirEngine.countActivePatients() }

  fun fetchDemographics(patientId: String): LiveData<Patient> {
    val data = MutableLiveData<Patient>()
    CoroutineScope(dispatcherProvider.io()).launch {
      data.postValue(fhirEngine.load(Patient::class.java, patientId))
    }
    return data
  }

  fun fetchTestResults(patientId: String): LiveData<List<QuestionnaireResponse>> {
    val data = MutableLiveData<List<QuestionnaireResponse>>()
    CoroutineScope(dispatcherProvider.io()).launch {
      val result =
        fhirEngine.search<QuestionnaireResponse> {
          filter(QuestionnaireResponse.SUBJECT) { value = "Patient/$patientId" }
        }

      data.postValue(result)
    }
    return data
  }

  fun fetchTestForms(code: String, system: String): LiveData<List<QuestionnaireConfig>> {
    val data = MutableLiveData<List<QuestionnaireConfig>>()
    CoroutineScope(dispatcherProvider.io()).launch {
      val result =
        fhirEngine.search<Questionnaire> {
          filter(
            Questionnaire.CONTEXT,
            CodeableConcept().apply {
              addCoding().apply {
                this.code = code
                this.system = system
              }
            }
          )
        }

      data.postValue(result.map { QuestionnaireConfig(it.name, it.title, it.logicalId) })
    }
    return data
  }
}
