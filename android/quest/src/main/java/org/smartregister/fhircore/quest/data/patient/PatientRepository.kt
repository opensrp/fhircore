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
import com.google.android.fhir.get
import com.google.android.fhir.getLocalizedText
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Observation
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
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.asLabel
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.getEncounterId
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.pregnancyCondition
import org.smartregister.fhircore.engine.util.extension.referenceParamForCondition
import org.smartregister.fhircore.engine.util.extension.referenceParamForObservation
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.configuration.view.DataDetailsListViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.data.patient.model.QuestionnaireItem
import org.smartregister.fhircore.quest.data.patient.model.QuestionnaireResponseItem
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.quest.util.FhirPathUtil.doesSatisfyFilter
import org.smartregister.fhircore.quest.util.FhirPathUtil.getPathValue
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
    withContext(dispatcherProvider.io()) { fhirEngine.get<Patient>(patientId) }

  suspend fun fetchTestResults(
    subjectId: String,
    subjectType: ResourceType,
    forms: List<QuestionnaireConfig>,
    config: DataDetailsListViewConfiguration
  ): List<QuestResultItem> {
    return withContext(dispatcherProvider.io()) {
      val testResults = mutableListOf<QuestResultItem>()

      val questionnaireResponses =
        searchQuestionnaireResponses(subjectId, subjectType, forms).sortedByDescending {
          it.authored
        }
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
    patientDetailsViewConfiguration: DataDetailsListViewConfiguration
  ): QuestResultItem {
    val data: MutableList<List<AdditionalData>> = mutableListOf()

    when {
      patientDetailsViewConfiguration.dynamicRows.isEmpty() -> {
        data.add(
          listOf(
            AdditionalData(value = fetchResultItemLabel(questionnaire)),
            AdditionalData(value = " (${questionnaireResponse.authored?.asDdMmmYyyy() ?: ""})")
          )
        )
      }
      else -> {

        val reference =
          questionnaireResponse.getEncounterId()?.let { loadEncounter(it) } ?: questionnaireResponse

        patientDetailsViewConfiguration.dynamicRows.forEach { filtersList ->
          val additionalDataList = mutableListOf<AdditionalData>()

          filtersList.forEach { filter ->
            val value =
              when (filter.resourceType) {
                Enumerations.ResourceType.CONDITION ->
                  getCondition(reference, filter)
                    .find { doesSatisfyFilter(it, filter) == true }
                    ?.getPathValue(filter.displayableProperty)
                Enumerations.ResourceType.OBSERVATION ->
                  getObservation(reference, filter)
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
      }
    }

    patientDetailsViewConfiguration.questionnaireFieldsFilter.groupBy { it.index }.forEach {
      it
        .value
        .mapNotNull { f ->
          questionnaireResponse.find(f.key)?.let {
            AdditionalData(
              label = f.label ?: it.asLabel(),
              value = it.answerFirstRep.value.valueToString()
            )
          }
        }
        .takeIf { it.isNotEmpty() }
        ?.run { data.add(it.key ?: data.size, this) }
    }

    val questSourceQRItem =
      QuestionnaireResponseItem(
        logicalId = questionnaireResponse.logicalId,
        authored = questionnaireResponse.authored,
        encounterId = questionnaireResponse.getEncounterId(),
        questionnaireResponseString = questionnaireResponse.encodeResourceToString()
      )
    val questSourceQuestionnaireItem =
      QuestionnaireItem(
        logicalId = questionnaire.logicalId,
        name = questionnaire.name,
        title = questionnaire.title
      )

    return QuestResultItem(Pair(questSourceQRItem, questSourceQuestionnaireItem), data)
  }

  suspend fun getCondition(reference: Resource, filter: Filter?) =
    getSearchResults<Condition>(
      reference.referenceValue(),
      reference.referenceParamForCondition(),
      filter,
      fhirEngine
    )
      .sortedByDescending {
        if (it.hasOnsetDateTimeType()) it.onsetDateTimeType.value else it.recordedDate
      }
      .sortedByDescending { it.logicalId }

  suspend fun getObservation(reference: Resource, filter: Filter?) =
    getSearchResults<Observation>(
      reference.referenceValue(),
      reference.referenceParamForObservation(),
      filter,
      fhirEngine
    )
      .sortedByDescending {
        if (it.hasEffectiveDateTimeType()) it.effectiveDateTimeType.value else it.meta.lastUpdated
      }
      .sortedByDescending { it.logicalId }

  suspend fun getMedicationRequest(reference: Resource, filter: Filter?) =
    getSearchResults<MedicationRequest>(
      reference.referenceValue(),
      MedicationRequest.ENCOUNTER,
      filter,
      fhirEngine
    )
      .sortedByDescending { it.authoredOn }
      .sortedByDescending { it.logicalId }

  fun fetchResultItemLabel(questionnaire: Questionnaire): String {
    return questionnaire.titleElement.getLocalizedText()
      ?: questionnaire.nameElement.getLocalizedText() ?: questionnaire.logicalId
  }

  suspend fun getQuestionnaire(questionnaireResponse: QuestionnaireResponse): Questionnaire {
    return when {
      questionnaireResponse.questionnaire.isNullOrBlank() -> {
        Timber.e(
          Exception(
            "Cannot open QuestionnaireResponse because QuestionnaireResponse.questionnaire is null"
          )
        )
        Questionnaire()
      }
      else -> {
        val questionnaireUrlList = questionnaireResponse.questionnaire.split("/")
        when {
          questionnaireUrlList.size > 1 -> {
            loadQuestionnaire(questionnaireId = questionnaireUrlList[1])
          }
          else -> {
            Timber.e(
              Exception(
                "Cannot open QuestionnaireResponse because QuestionnaireResponse.questionnaire is null"
              )
            )
            Questionnaire()
          }
        }
      }
    }
  }

  private suspend fun loadQuestionnaire(questionnaireId: String): Questionnaire =
    withContext(dispatcherProvider.io()) { fhirEngine.get(questionnaireId) }

  suspend fun loadEncounter(id: String): Encounter =
    withContext(dispatcherProvider.io()) { fhirEngine.get(id) }

  private suspend fun searchQuestionnaireResponses(
    subjectId: String,
    subjectType: ResourceType,
    forms: List<QuestionnaireConfig>
  ): List<QuestionnaireResponse> =
    mutableListOf<QuestionnaireResponse>().also { result ->
      forms.forEach {
        result.addAll(
          fhirEngine.search {
            filter(QuestionnaireResponse.SUBJECT, { value = "${subjectType.name}/$subjectId" })
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
          form = it.nameElement.getLocalizedText() ?: it.logicalId,
          title = it.titleElement.getLocalizedText()
              ?: it.nameElement.getLocalizedText() ?: it.logicalId,
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
      fhirEngine.search {
        filterByResourceTypeId(Condition.SUBJECT, ResourceType.Patient, patientId)
      }
    val activePregnancy = listOfConditions.hasActivePregnancy()
    val activePregnancyCondition =
      if (activePregnancy) listOfConditions.pregnancyCondition() else null
    val jsonParser = FhirContext.forR4Cached().newJsonParser()
    return if (activePregnancy) jsonParser.encodeResourceToString(activePregnancyCondition) else ""
  }
}
