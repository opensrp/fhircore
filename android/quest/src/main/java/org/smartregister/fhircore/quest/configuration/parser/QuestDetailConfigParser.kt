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
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem

class QuestDetailConfigParser(fhirEngine: FhirEngine) : DetailConfigParser(fhirEngine) {

  override suspend fun getResultItem(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    patientDetailsViewConfiguration: PatientDetailsViewConfiguration
  ): QuestResultItem {

    val data =
      listOf(
        listOf(
          AdditionalData(value = fetchResultItemLabel(questionnaire)),
          AdditionalData(value = " (${questionnaireResponse.authored?.asDdMmmYyyy() ?: ""})")
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

    val questionnaireId = questionnaireResponse.questionnaire.split("/")[1]
    context.startActivity(
      Intent(context, QuestionnaireActivity::class.java)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = patientId,
            formName = questionnaireId,
            questionnaireType = QuestionnaireType.READ_ONLY,
            questionnaireResponse = questionnaireResponse
          )
        )
    )
  }

  fun fetchResultItemLabel(questionnaire: Questionnaire): String {
    return questionnaire.name ?: questionnaire.title ?: questionnaire.logicalId
  }
}
