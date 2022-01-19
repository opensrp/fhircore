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
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.Properties
import org.smartregister.fhircore.quest.configuration.view.Property
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.ui.patient.details.SimpleDetailsActivity

class G6PDDetailConfigParser(fhirEngine: FhirEngine) : DetailConfigParser(fhirEngine) {

  override suspend fun getResultItem(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration
  ): QuestResultItem {

    val encounterId = getEncounterId(questionnaireResponse)
    val condition = getCondition(encounterId)
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

  override fun onResultItemClicked(
    resultItem: QuestResultItem,
    context: Context,
    patientId: String
  ) {

    val questionnaireResponse = resultItem.source.first

    val encounterId = getEncounterId(questionnaireResponse)
    context.startActivity(
      Intent(context, SimpleDetailsActivity::class.java).apply {
        putExtra(SimpleDetailsActivity.RECORD_ID_ARG, encounterId)
      }
    )
  }

  fun getEncounterId(questionnaireResponse: QuestionnaireResponse): String {
    return questionnaireResponse
      .contained
      ?.find { it.resourceType == ResourceType.Encounter }
      ?.logicalId
      ?.replace("#", "")
      ?: ""
  }

  suspend fun getCondition(encounterId: String): Pair<String, String> {
    val condition =
      fhirEngine
        .search<Condition> {
          filter(Condition.ENCOUNTER, { value = "Encounter/$encounterId" })
          filter(
            TokenClientParam("category"),
            {
              value =
                of(CodeableConcept().addCoding(Coding("http://snomed.info/sct", "9024005", null)))
            }
          )
        }
        .firstOrNull()

    return condition?.let {
      Pair(it.code?.codingFirstRep?.display ?: "", " (${it.recordedDate?.asDdMmmYyyy() ?: ""}) ")
    }
      ?: run { Pair("", "") }
  }

  suspend fun getG6pd(encounterId: String): String {
    return getObservation(encounterId, "http://snomed.info/sct", "86859003")?.value.valueToString()
  }

  suspend fun getHb(encounterId: String): String {
    return getObservation(encounterId, "http://snomed.info/sct", "259695003")?.value.valueToString()
  }

  suspend fun getObservation(encounterId: String, system: String, code: String): Observation? {

    return fhirEngine
      .search<Observation> {
        filter(Observation.ENCOUNTER, { value = "Encounter/$encounterId" })
        filter(
          TokenClientParam("code"),
          { value = of(CodeableConcept().addCoding(Coding(system, code, null))) }
        )
      }
      .firstOrNull()
  }
}
