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

package org.smartregister.fhircore.quest.configuration.parser

import android.content.Context
import android.content.Intent
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.Properties
import org.smartregister.fhircore.quest.configuration.view.Property
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItemCell
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItemRow
import org.smartregister.fhircore.quest.data.patient.model.QuestResultSubItemRow
import org.smartregister.fhircore.quest.util.getSearchResults

class QuestDetailConfigParser @Inject constructor(fhirEngine: FhirEngine) : DetailConfigParser(fhirEngine) {

  override suspend fun getResultItem(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration
  ): QuestResultItem {

    when {
      patientDetailsViewConfiguration.rows.isEmpty() -> {
        val data : List<QuestResultSubItemRow> = mutableListOf()

          listOf(
            listOf(
              AdditionalData(value = fetchResultItemLabel(questionnaire)),
              AdditionalData(value = " (${questionnaireResponse.authored?.asDdMmmYyyy() ?: ""})")
            )
          )

        return QuestResultItem(Pair(questionnaireResponse, questionnaire), data)
      } else -> {


      val rowData = QuestResultItemRow()
      val encounterId = getEncounterId(questionnaireResponse)
      val encounter = loadEncounter(encounterId)

      patientDetailsViewConfiguration.rows.forEach {

        val subItemRow = QuestResultSubItemRow()

        it.filters.forEach { filter ->
          val value =
            when (filter.resourceType) {
              Enumerations.ResourceType.CONDITION ->
                getCondition(encounter, filter)?.code?.codingFirstRep
              Enumerations.ResourceType.OBSERVATION -> getObservation(encounter, filter)?.value
              else -> null
            }
          subItemRow.cells.add(QuestResultItemCell(value, filter))

        }

      }
/////////////////////
      val condition = getCondition(encounter)
      val g6pd = getG6pd(encounterId)
      val hb = getHb(encounterId)

      val property = Property(color = "#74787A", textSize = 16)
      val properties = Properties(label = property, value = property)

      val data =
        listOf(
          listOf(AdditionalData(value = condition.first), AdditionalData(value = condition.second)),
          listOf(
            AdditionalData(label = "G6PD: ", value = g6pd, properties = properties),
            AdditionalData(label = " - Hb: ", value = hb, properties = properties)
          )
        )

      return QuestResultItem(Pair(questionnaireResponse, questionnaire), data)
      }
    }
  }

  override fun onResultItemClicked(
    resultItem: QuestResultItem,
    context: Context,
    patientId: String
  ) {

    val questionnaireResponse = resultItem.source.first

    val questionnaireId = questionnaireResponse.questionnaire.split("/")[1]
    val populationResources = ArrayList<Resource>().apply { add(questionnaireResponse) }
    context.startActivity(
      Intent(context, QuestionnaireActivity::class.java)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = patientId,
            formName = questionnaireId,
            readOnly = true,
            populationResources = populationResources
          )
        )
    )
  }

  fun fetchResultItemLabel(questionnaire: Questionnaire): String {
    return questionnaire.name ?: questionnaire.title ?: questionnaire.logicalId
  }

  fun getEncounterId(questionnaireResponse: QuestionnaireResponse): String {
    return questionnaireResponse
      .contained
      ?.find { it.resourceType == ResourceType.Encounter }
      ?.logicalId
      ?.replace("#", "")
      ?: ""
  }

  suspend fun getCondition(encounterId: String, key: String, system: String, code: String): Pair<String, String> {
    val condition =
      fhirEngine
        .search<Condition> {
          filter(Condition.ENCOUNTER) { value = "Encounter/$encounterId" }
          filter(
            TokenClientParam(key),
            CodeableConcept().addCoding(Coding(system, code, null))
          )
        }
        .firstOrNull()

    return condition?.let {
      Pair(it.code?.codingFirstRep?.display ?: "", " (${it.recordedDate?.asDdMmmYyyy() ?: ""}) ")
    }
      ?: run { Pair("", "") }
  }

  suspend fun getG6pd(encounterId: String, key: String, system: String, code: String): String {
    return getObservation(encounterId, key, system, code)?.value.valueToString()
  }

  suspend fun getHb(encounterId: String, key: String, system: String, code: String): String {
    return getObservation(encounterId, key, system, code)?.value.valueToString()
  }

  suspend fun getObservation(encounterId: String, key: String, system: String, code: String): Observation? {

    return fhirEngine
      .search<Observation> {
        filter(Observation.ENCOUNTER) { value = "Encounter/$encounterId" }
        filter(TokenClientParam(key), CodeableConcept().addCoding(Coding(system, code, null)))
      }
      .firstOrNull()
  }

  suspend fun getCondition(encounter: Encounter, filter: Filter): Condition? {
    return getSearchResults<Condition>(
      encounter.referenceValue(),
      Condition.ENCOUNTER,
      filter,
      fhirEngine
    )
      .firstOrNull()
  }

  suspend fun getObservation(encounter: Encounter, filter: Filter): Observation? {
    return getSearchResults<Observation>(
      encounter.referenceValue(),
      Observation.ENCOUNTER,
      filter,
      fhirEngine
    )
      .firstOrNull()
  }

  suspend fun loadEncounter(id: String): Encounter =
    withContext(dispatcherProvider.io()) { fhirEngine.load(Encounter::class.java, id) }
}
