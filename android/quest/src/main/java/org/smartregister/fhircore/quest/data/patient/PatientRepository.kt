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
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.data.patient.model.PatientItem

class PatientRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Patient, PatientItem>,
  private val registerViewConfiguration: MutableLiveData<RegisterViewConfiguration>? = null,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Patient, PatientItem> {

  fun applyPrimaryFilter(search: Search) {
    val filter = registerViewConfiguration?.value?.primaryFilter
    if (filter != null) {
      search.filter(
        TokenClientParam(filter.key),
        Coding().apply {
          code = filter.code
          system = filter.system
        }
      )
    } else search.filter(Patient.ACTIVE, true)
  }

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<PatientItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          applyPrimaryFilter(this)

          if (query.isNotBlank()) {
            filter(Patient.NAME) {
              modifier = StringFilterModifier.CONTAINS
              value = query.trim()
            }
          }
          sort(Patient.NAME, Order.ASCENDING)
          count = if (loadAll) countAll()?.toInt() else PaginationUtil.DEFAULT_PAGE_SIZE
          from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
        }

      patients.map { domainMapper.mapToDomainModel(it) }
    }
  }

  override suspend fun countAll(): Long =
    withContext(dispatcherProvider.io()) { fhirEngine.count<Patient> { applyPrimaryFilter(this) } }

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

  fun fetchTestForms(filter: SearchFilter): LiveData<List<QuestionnaireConfig>> {
    val data = MutableLiveData<List<QuestionnaireConfig>>()
    CoroutineScope(dispatcherProvider.io()).launch {
      val result =
        fhirEngine.search<Questionnaire> {
          filter(
            Questionnaire.CONTEXT,
            CodeableConcept().apply {
              addCoding().apply {
                this.code = filter.code
                this.system = filter.system
              }
            }
          )
        }

      // TODO Remove hardcoded appId
      data.postValue(
        result.map {
          QuestionnaireConfig(
            appId = "quest",
            form = it.name,
            title = it.title,
            identifier = it.logicalId
          )
        }
      )
    }
    return data
  }
}
