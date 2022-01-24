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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.quest.configuration.parser.DetailConfigParser
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.quest.util.loadAdditionalData
import timber.log.Timber

class PatientRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val domainMapper: PatientItemMapper,
  private val dispatcherProvider: DispatcherProvider,
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
          filter(Patient.ACTIVE, { value = of(true) })
          if (query.isNotBlank()) {
            filter(
              Patient.NAME,
              {
                modifier = StringFilterModifier.CONTAINS
                value = query.trim()
              }
            )
          }
          sort(Patient.NAME, Order.ASCENDING)
          count = if (loadAll) countAll().toInt() else PaginationUtil.DEFAULT_PAGE_SIZE
          from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
        }

      patients.map {
        val patientItem = domainMapper.mapToDomainModel(it)
        patientItem.additionalData =
          loadAdditionalData(patientItem.id, configurationRegistry, fhirEngine)
        patientItem
      }
    }
  }

  override suspend fun countAll(): Long =
    withContext(dispatcherProvider.io()) { fhirEngine.countActivePatients() }

  suspend fun fetchDemographics(patientId: String): Patient =
    withContext(dispatcherProvider.io()) { fhirEngine.load(Patient::class.java, patientId) }

  suspend fun fetchTestResults(
    patientId: String,
    forms: List<QuestionnaireConfig>,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration,
    parser: DetailConfigParser?
  ): List<QuestResultItem> {
    return withContext(dispatcherProvider.io()) {
      val testResults = mutableListOf<QuestResultItem>()

      parser?.let { p ->
        val questionnaireResponses =
          searchQuestionnaireResponses(patientId, forms).sortedByDescending { it.authored }
        questionnaireResponses.forEach {
          val questionnaire = getQuestionnaire(it)
          testResults.add(p.getResultItem(questionnaire, it, patientDetailsViewConfiguration))
        }
      }
        ?: run { Timber.w("Getting null parser") }

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

  suspend fun loadEncounter(id: String): Encounter =
    withContext(dispatcherProvider.io()) { fhirEngine.load(Encounter::class.java, id) }

  private suspend fun searchQuestionnaireResponses(
    patientId: String,
    forms: List<QuestionnaireConfig>
  ): List<QuestionnaireResponse> =
    mutableListOf<QuestionnaireResponse>().also { result ->
      forms.forEach {
        result.addAll(
          fhirEngine.search {
            filter(QuestionnaireResponse.SUBJECT, { value = "Patient/$patientId" })
            filter(
              QuestionnaireResponse.QUESTIONNAIRE,
              { value = "Questionnaire/${it.identifier}" }
            )
          }
        )
      }
    }

  suspend fun fetchTestForms(filter: SearchFilter): List<QuestionnaireConfig> =
    withContext(dispatcherProvider.io()) {
      val result =
        fhirEngine.search<Questionnaire> {
          filter(
            Questionnaire.CONTEXT,
            {
              value =
                of(
                  CodeableConcept().apply {
                    addCoding().apply {
                      this.code = filter.code
                      this.system = filter.system
                    }
                  }
                )
            }
          )
        }

      result.map {
        QuestionnaireConfig(
          appId =
            if (configurationRegistry.isAppIdInitialized()) configurationRegistry.appId else "",
          form = it.name ?: it.logicalId,
          title = it.title ?: it.name ?: it.logicalId,
          identifier = it.logicalId
        )
      }
    }

  suspend fun fetchDemographicsWithAdditionalData(patientId: String): PatientItem {
    return withContext(dispatcherProvider.io()) {
      val patientItem = domainMapper.mapToDomainModel(fetchDemographics(patientId))
      patientItem.additionalData =
        loadAdditionalData(patientItem.id, configurationRegistry, fhirEngine)
      patientItem
    }
  }
}
