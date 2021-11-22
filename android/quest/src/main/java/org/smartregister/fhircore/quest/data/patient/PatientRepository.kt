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

import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper

class PatientRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val domainMapper: PatientItemMapper,
  val dispatcherProvider: DispatcherProvider
) : RegisterRepository<Patient, PatientItem> {

  // TODO obtain correct filter param for the logged in user
  val searchFilter = SearchFilter(key = "_tag", code = "http://fhir.ona.io", system = "000003")

  private fun applyPrimaryFilter(search: Search, filter: SearchFilter) {
    search.filter(
      TokenClientParam(filter.key),
      Coding().apply {
        code = filter.code
        system = filter.system
      }
    )
  }

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<PatientItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          applyPrimaryFilter(search = this, filter = searchFilter)

          if (query.isNotBlank()) {
            filter(Patient.NAME) {
              modifier = StringFilterModifier.CONTAINS
              value = query.trim()
            }
          }
          sort(Patient.NAME, Order.ASCENDING)
          count = if (loadAll) countAll().toInt() else PaginationUtil.DEFAULT_PAGE_SIZE
          from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
        }

      patients.map { domainMapper.mapToDomainModel(it) }
    }
  }

  override suspend fun countAll(): Long =
    withContext(dispatcherProvider.io()) {
      fhirEngine.count<Patient> { applyPrimaryFilter(search = this, filter = searchFilter) }
    }

  suspend fun fetchDemographics(patientId: String): Patient =
    withContext(dispatcherProvider.io()) { fhirEngine.load(Patient::class.java, patientId) }

  suspend fun fetchTestResults(patientId: String): List<QuestionnaireResponse> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(QuestionnaireResponse.SUBJECT) { value = "Patient/$patientId" } }
    }

  suspend fun fetchTestForms(
    filter: SearchFilter,
    appId: String = "quest"
  ): List<QuestionnaireConfig> =
    withContext(dispatcherProvider.io()) {
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

      result.map {
        QuestionnaireConfig(
          appId = appId,
          form = it.name,
          title = it.title,
          identifier = it.logicalId
        )
      }
    }
}
