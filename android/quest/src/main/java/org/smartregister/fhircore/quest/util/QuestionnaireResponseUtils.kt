/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.util

import android.content.Context
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.datacapture.validation.ValidationResult
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.util.extension.packRepeatedGroups

object QuestionnaireResponseUtils {
  /**
   * This function validates all [org.hl7.fhir.r4.model.QuestionnaireResponse] and returns true if
   * all the validation result of [QuestionnaireResponseUtils] are
   * [com.google.android.fhir.datacapture.validation.Valid] or
   * [com.google.android.fhir.datacapture.validation.NotValidated] (validation is optional on
   * [org.hl7.fhir.r4.model.Questionnaire] fields)
   */
  suspend fun validateQuestionnaireResponse(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context,
  ): Boolean {
    return getQuestionnaireResponseErrors(
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
        context = context,
      )
      .isEmpty()
  }

  suspend fun getQuestionnaireResponseErrors(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context,
  ): List<ValidationResult> {
    val validQuestionnaireResponseItems =
      mutableListOf<QuestionnaireResponse.QuestionnaireResponseItemComponent>()
    val validQuestionnaireItems = mutableListOf<Questionnaire.QuestionnaireItemComponent>()
    val questionnaireItemsMap = questionnaire.item.associateBy { it.linkId }

    // Only validate items that are present on both Questionnaire and the QuestionnaireResponse
    questionnaireResponse.copy().item.forEach {
      if (questionnaireItemsMap.containsKey(it.linkId)) {
        val questionnaireItem = questionnaireItemsMap.getValue(it.linkId)
        validQuestionnaireResponseItems.add(it)
        validQuestionnaireItems.add(questionnaireItem)
      }
    }

    return QuestionnaireResponseValidator.validateQuestionnaireResponse(
        questionnaire = Questionnaire().apply { item = validQuestionnaireItems },
        questionnaireResponse =
          QuestionnaireResponse().apply {
            item = validQuestionnaireResponseItems
            packRepeatedGroups()
          },
        context = context,
      )
      .values
      .flatten()
      .filter { it is Invalid }
  }

  suspend fun getQuestionnaireResponseErrorsAsStrings(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context,
  ): List<String> {
    return getQuestionnaireResponseErrors(
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
        context = context,
      )
      .map { it.toString() }
  }
}
