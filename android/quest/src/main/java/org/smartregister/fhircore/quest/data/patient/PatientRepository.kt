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

import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.engine.util.extension.filterByPatient
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.pregnancyCondition
import org.smartregister.fhircore.quest.configuration.parser.DetailConfigParser
import org.smartregister.fhircore.engine.util.extension.getEncounterId
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.isSimilar
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.quest.util.getSearchResults
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

  private val fhirPathEngine = FHIRPathEngine(SimpleWorkerContext())

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
    config: PatientDetailsViewConfiguration
  ): List<QuestResultItem> {
    return withContext(dispatcherProvider.io()) {
      val testResults = mutableListOf<QuestResultItem>()

      val questionnaireResponses =
        searchQuestionnaireResponses(patientId, forms).sortedByDescending { it.authored }
      questionnaireResponses.forEach {
        val questionnaire = getQuestionnaire(it)
        testResults.add(getResultItem(questionnaire, it, config))
      }
      testResults
    }
  }

  suspend fun getResultItem(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration
  ): QuestResultItem {

    when {
      patientDetailsViewConfiguration.dynamicRows.isEmpty() -> {
        val data =
          listOf(
            listOf(
              AdditionalData(value = fetchResultItemLabel(questionnaire)),
              AdditionalData(value = " (${questionnaireResponse.authored?.asDdMmmYyyy() ?: ""})")
            )
          )

        return QuestResultItem(Pair(questionnaireResponse, questionnaire), data)
      }
      else -> {

        val encounter = loadEncounter(questionnaireResponse.getEncounterId())
        val data: MutableList<List<AdditionalData>> = mutableListOf()

        patientDetailsViewConfiguration.dynamicRows.forEach { filtersList ->
          val additionalDataList = mutableListOf<AdditionalData>()

          filtersList.forEach { filter ->
            val value =
              when (filter.resourceType) {
                Enumerations.ResourceType.CONDITION ->
                  getCondition(encounter, filter)
                    .find { doesSatisfyFilter(it, filter) == true }
                    ?.getPathValue(filter.displayableProperty)
                Enumerations.ResourceType.OBSERVATION ->
                  getObservation(encounter, filter)
                    .find { doesSatisfyFilter(it, filter) == true }
                    ?.getPathValue(filter.displayableProperty)
                else -> null
              }

            additionalDataList.add(
              AdditionalData(
                label = filter.label,
                value = value.valueToString(),
                valuePrefix = filter.valuePrefix,
                valuePostfix = filter.valuePostfix,
                properties = filter.properties
              )
            )
          }
          data.add(additionalDataList)
        }

        return QuestResultItem(Pair(questionnaireResponse, questionnaire), data)
      }
    }
  }

  fun Base.getPathValue(path: String) = fhirPathEngine.evaluate(this, path).firstOrNull()

  fun doesSatisfyFilter(resource: Resource, filter: Filter): Boolean? {
    if (filter.valueCoding == null && filter.valueString == null)
      throw IllegalStateException("Filter must have either of one valueCoding or valueString")

    // get property mentioned as filter and match value
    // e.g. category: CodeableConcept in Condition
    return resource
      .getNamedProperty(filter.key)
      .values
      .firstOrNull()
      ?.let {
        when (it) {
          // match relevant type and value
          is CodeableConcept -> filter.valueCoding!!.isSimilar(it)
          is Coding -> filter.valueCoding!!.isSimilar(it)
          is StringType -> it.value == filter.valueString
          else -> false
        }
      }
      .also {
        if (it == null)
          Timber.i("${resource.resourceType}, ${filter.key}: could not resolve key value filter")
      }
  }

  suspend fun getCondition(encounter: Encounter, filter: Filter?) =
    getSearchResults<Condition>(encounter.referenceValue(), Condition.ENCOUNTER, filter, fhirEngine)
      .sortedByDescending {
        if (it.hasOnsetDateTimeType()) it.onsetDateTimeType.value else it.recordedDate
      }
      .sortedByDescending { it.logicalId }

  suspend fun getObservation(encounter: Encounter, filter: Filter?) =
    getSearchResults<Observation>(
      encounter.referenceValue(),
      Observation.ENCOUNTER,
      filter,
      fhirEngine
    )
      .sortedByDescending {
        if (it.hasEffectiveDateTimeType()) it.effectiveDateTimeType.value else it.meta.lastUpdated
      }
      .sortedByDescending { it.logicalId }

  fun fetchResultItemLabel(questionnaire: Questionnaire): String {
    return questionnaire.name ?: questionnaire.title ?: questionnaire.logicalId
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
          appId = configurationRegistry.appId,
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

  suspend fun fetchPregnancyCondition(patientId: String): String {
    val listOfConditions: List<Condition> =
      fhirEngine.search { filterByPatient(Condition.SUBJECT, patientId = patientId) }
    val activePregnancy = listOfConditions.hasActivePregnancy()
    val activePregnancyCondition =
      if (activePregnancy) listOfConditions.pregnancyCondition() else null
    val jsonParser = FhirContext.forR4Cached().newJsonParser()
    return if (activePregnancy) jsonParser.encodeResourceToString(activePregnancyCondition) else ""
  }
}
