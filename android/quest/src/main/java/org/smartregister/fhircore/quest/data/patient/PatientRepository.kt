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

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.quest.configuration.view.Code
import org.smartregister.fhircore.quest.configuration.view.DynamicColor
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.configuration.view.PatientRegisterRowViewConfiguration
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.quest.util.QuestConfigClassification
import timber.log.Timber

class PatientRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val domainMapper: PatientItemMapper,
  val dispatcherProvider: DispatcherProvider,
  val configurationRegistry: ConfigurationRegistry
) : RegisterRepository<Patient, PatientItem> {

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<PatientItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          filter(Patient.ACTIVE, true)
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

      patients.map {
        val patientItem = domainMapper.mapToDomainModel(it)
        patientItem.additionalData = loadAdditionalData(patientItem.id)
        patientItem
      }
    }
  }

  override suspend fun countAll(): Long =
    withContext(dispatcherProvider.io()) { fhirEngine.countActivePatients() }

  suspend fun fetchDemographics(patientId: String): Patient =
    withContext(dispatcherProvider.io()) { fhirEngine.load(Patient::class.java, patientId) }

  suspend fun fetchTestResults(
    patientId: String
  ): List<Pair<QuestionnaireResponse, Questionnaire>> {
    return withContext(dispatcherProvider.io()) {
      val questionnaireResponses = searchQuestionnaireResponses(patientId)

      val testResults = mutableListOf<Pair<QuestionnaireResponse, Questionnaire>>()
      questionnaireResponses.forEach {
        val questionnaire = getQuestionnaire(it)
        testResults.add(Pair(it, questionnaire))
      }
      testResults
    }
  }

  suspend fun getQuestionnaire(questionnaireResponse: QuestionnaireResponse): Questionnaire {
    return if (questionnaireResponse.questionnaire != null) {
      val questionnaireId = questionnaireResponse.questionnaire.split("/")[1]
      loadQuestionnaire(questionnaireId = questionnaireId)
    } else {
      Timber.e(
        Exception(
          "Cannot open QuestionnaireResponse because QuestionnaireResponse.questionnaire is null"
        )
      )
      Questionnaire()
    }
  }

  private suspend fun loadQuestionnaire(questionnaireId: String): Questionnaire =
    withContext(dispatcherProvider.io()) {
      fhirEngine.load(Questionnaire::class.java, questionnaireId)
    }

  private suspend fun searchQuestionnaireResponses(patientId: String): List<QuestionnaireResponse> =
    fhirEngine.search { filter(QuestionnaireResponse.SUBJECT) { value = "Patient/$patientId" } }

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

  private suspend fun loadAdditionalData(patientId: String): List<AdditionalData>? {
    val result = mutableListOf<AdditionalData>()

    val patientRegisterRowViewConfiguration =
      configurationRegistry.retrieveConfiguration<PatientRegisterRowViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER_ROW
      )

    patientRegisterRowViewConfiguration.filters?.forEach { filter ->
      when (filter.resourceType) {
        Enumerations.ResourceType.CONDITION -> {
          val conditions = getSearchResults<Condition>(patientId, Condition.SUBJECT, filter)

          val sortedByDescending = conditions.maxByOrNull { it.recordedDate }
          sortedByDescending?.code?.coding
            ?.firstOrNull { it.code == filter.valueCoding!!.code }
            ?.let {
              result.add(
                AdditionalData(
                  it.display,
                  getColor(it.display, filter.dynamicColors) ?: filter.color,
                  filter.valuePrefix
                )
              )
            }
        }
      }
    }

    return result
  }

  private suspend inline fun <reified T : Resource> getSearchResults(
    patientId: String,
    reference: ReferenceClientParam,
    filter: Filter
  ): List<T> {
    return fhirEngine.search {
      filterByPatient(reference, patientId)

      when (filter.valueType) {
        Enumerations.DataType.CODEABLECONCEPT -> {
          filter(TokenClientParam(filter.key), filter.valueCoding!!.asCodeableConcept())
        }
        Enumerations.DataType.CODING -> {
          filter(TokenClientParam(filter.key), filter.valueCoding!!.asCoding())
        }
      }
    }
  }

  private fun getColor(value: String, dynamicColors: List<DynamicColor>?): String? {
    return dynamicColors?.firstOrNull { it.valueEqual == value }?.useColor
  }

  private fun Search.filterByPatient(reference: ReferenceClientParam, patientId: String) {
    filter(reference) { this.value = "${ResourceType.Patient.name}/$patientId" }
  }

  private fun Code.asCoding(): Coding {
    return Coding(system, code, display)
  }

  private fun Code.asCodeableConcept(): CodeableConcept {
    return CodeableConcept().addCoding(this.asCoding())
  }
}
