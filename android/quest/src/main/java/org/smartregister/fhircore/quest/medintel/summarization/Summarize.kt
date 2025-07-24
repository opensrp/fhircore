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

package org.smartregister.fhircore.quest.medintel.summarization

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel

object Summarize {
  private val fhirContext = FhirContext.forR4()

  suspend fun <T> summarize(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    llmModel: LlmModel<T>,
  ): String? {
    val prompt =
      generateSummarizationPrompt(
        questionnaireName = questionnaire.name,
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
      )
    return llmModel.generateContent(prompt)
  }

  private fun generateSummarizationPrompt(
    questionnaireName: String,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
  ): String {
    val jsonParser = fhirContext.newJsonParser().setPrettyPrint(true)
    return """
      Summarize in 3 sentences a patient visit based on the
      QuestionnaireResponse and Questionnaire provided.\n
      Questionnaire Name: $questionnaireName\n
      Questionnaire: ${jsonParser.encodeResourceToString(questionnaire)}\n
      QuestionnaireResponse: ${jsonParser.encodeResourceToString(questionnaireResponse)}
            """
      .trimIndent()
  }
}
