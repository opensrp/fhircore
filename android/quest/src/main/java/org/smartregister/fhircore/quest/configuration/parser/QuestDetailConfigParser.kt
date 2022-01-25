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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import javax.inject.Inject
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.util.getSearchResults

class QuestDetailConfigParser @Inject constructor(fhirEngine: FhirEngine) :
  DetailConfigParser(fhirEngine) {

  override suspend fun getResultItem(
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
        val encounterId = getEncounterId(questionnaireResponse)
        // val encounter = loadEncounter(encounterId)

        val data: MutableList<List<AdditionalData>> = mutableListOf()

        patientDetailsViewConfiguration.dynamicRows.forEach { filters ->
          val additionalDataList = mutableListOf<AdditionalData>()

          filters.forEach { filter ->

            /*          val value =
            when (filter.resourceType) {
              Enumerations.ResourceType.CONDITION ->
                getCondition(encounter, filter)?.code?.codingFirstRep
              Enumerations.ResourceType.OBSERVATION -> getObservation(encounter, filter)?.value
              else -> null
            }*/

            additionalDataList.add(AdditionalData(value = ""))
          }
          data.add(additionalDataList)
        }

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

  /*  suspend fun loadEncounter(id: String): Encounter =
  withContext(dispatcherProvider.io()) { fhirEngine.load(Encounter::class.java, id) }*/
}
