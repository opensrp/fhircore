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

package org.smartregister.fhircore.engine.util.extension

import com.google.android.fhir.datacapture.targetStructureMap
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

fun Questionnaire.isExtractionCandidate() =
  this.targetStructureMap != null ||
    this.extension.any { it.url.contains("sdc-questionnaire-itemExtractionContext") }

fun Questionnaire.find(linkId: String): Questionnaire.QuestionnaireItemComponent? {
  return item.find(linkId, null)
}

private fun List<Questionnaire.QuestionnaireItemComponent>.find(
  linkId: String,
  default: Questionnaire.QuestionnaireItemComponent?
): Questionnaire.QuestionnaireItemComponent? {
  var result = default
  run loop@{
    forEach {
      if (it.linkId == linkId) {
        result = it
        return@loop
      } else if (it.item.isNotEmpty()) {
        result = it.item.find(linkId, result)
      }
    }
  }

  return result
}

fun QuestionnaireResponse.find(
  linkId: String
): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
  return item.find(linkId, null)
}

private fun List<QuestionnaireResponse.QuestionnaireResponseItemComponent>.find(
  linkId: String,
  default: QuestionnaireResponse.QuestionnaireResponseItemComponent?
): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
  var result = default
  run loop@{
    forEach {
      if (it.linkId == linkId) {
        result = it
        return@loop
      } else if (it.item.isNotEmpty()) {
        result = it.item.find(linkId, result)
      } else if (it.hasAnswer()) {
        it.answer.forEach { result = it.item.find(linkId, result) }
      }
    }
  }

  return result
}
