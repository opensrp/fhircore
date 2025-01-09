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

package org.smartregister.fhircore.quest.ui.speechtoform

import ca.uhn.fhir.interceptor.model.RequestPartitionId.fromJson
import com.google.ai.client.generativeai.GenerativeModel
import java.io.File
import java.util.logging.Logger
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.json.JSONObject
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.util.QuestionnaireResponseValidator

class TextToForm(
  private val generativeModel: GenerativeModel,
  private val dispatcherProvider: DispatcherProvider?,
  private val questionnaireActivity: QuestionnaireActivity?,
) {

  private val logger = Logger.getLogger(TextToForm::class.java.name)

  /**
   * Generates an HL7 FHIR QuestionnaireResponse from a transcript using the provided Questionnaire.
   *
   * @param transcriptFile The temporary file containing the transcript text.
   * @param questionnaire The FHIR Questionnaire to base the response on.
   * @return The generated and validated QuestionnaireResponse or null if generation fails.
   */
  suspend fun generateQuestionnaireResponse(
    transcriptFile: File,
    questionnaire: Questionnaire,
  ): QuestionnaireResponse? {
    val transcript = transcriptFile.readText()
    val prompt = promptTemplate(transcript, questionnaire)

    logger.info("Sending request to Gemini...")
    val generatedText = generativeModel.generateContent(prompt).text

    val questionnaireResponseJson = extractJsonBlock(generatedText) ?: return null

    return try {
      val questionnaireResponse = parseQuestionnaireResponse(questionnaireResponseJson)
      if (validateQuestionnaireResponse(questionnaire, questionnaireResponse)) {
        logger.info("QuestionnaireResponse validated successfully.")
        questionnaireResponse
      } else {
        logger.warning("QuestionnaireResponse validation failed.")
        null
      }
    } catch (e: Exception) {
      logger.severe("Error generating QuestionnaireResponse: ${e.message}")
      null
    }
  }

  /**
   * Builds the prompt for the Gemini model.
   *
   * @param transcript The text transcript of the conversation.
   * @param questionnaire The FHIR Questionnaire to base the response on.
   * @return The prompt string to be sent to the Gemini model.
   */
  private fun promptTemplate(transcript: String, questionnaire: Questionnaire): String {
    return """
      You are a scribe created to turn conversational text into structure HL7 FHIR output. Below
      you will see the text Transcript of a conversation between a nurse and a patient within
      <transcript> XML tags and an HL7 FHIR Questionnaire within <questionnaire> XML tags. Your job
      is to convert the text in Transcript into a new HL7 FHIR QuestionnaireResponse as if the
      information in Transcript had been entered directly into the FHIR Questionniare. Only output
      the FHIR QuestionnaireResponse as JSON and nothing else.
      <transcript>$transcript</transcript>
      <questionnaire>$questionnaire</questionnaire>
      """
      .trimIndent()
  }

  /**
   * Extracts the JSON block from the generated text.
   *
   * @param responseText The text response from the Gemini model.
   * @return The extracted JSON string or null if extraction fails.
   */
  private fun extractJsonBlock(responseText: String?): String? {
    if (responseText == null) return null
    val start = responseText.indexOf("```json")
    if (start == -1) return null
    val end = responseText.indexOf("```", start + 7)
    return if (end == -1) null else responseText.substring(start + 7, end).trim()
  }

  /**
   * Parses the JSON string into a QuestionnaireResponse object.
   *
   * @param json The JSON string representing the QuestionnaireResponse.
   * @return The parsed QuestionnaireResponse object.
   */
  private fun parseQuestionnaireResponse(json: String): QuestionnaireResponse {
    return QuestionnaireResponse().apply { fromJson(JSONObject(json).toString()) }
  }

  /**
   * Validates the QuestionnaireResponse structure.
   *
   * @param questionnaireResponse The QuestionnaireResponse object to validate.
   * @return True if the QuestionnaireResponse is valid, false otherwise.
   */
  private suspend fun validateQuestionnaireResponse(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
  ): Boolean {
    // validate using existing code, not sure about how to pass/instantiate in the
    // dispatcherProvider and questionnaireActivity
    val errors =
      QuestionnaireResponseValidator.getQuestionnaireResponseErrors(
        questionnaire,
        questionnaireResponse,
        QuestionnaireActivity(),
        DefaultDispatcherProvider(),
      )

    // no errors, no need to retry
    if (errors.isEmpty()) {
      return true
    }

    // TODO build new prompt from errors

    // TODO retry with new prompt, and go to top

    return false
  }
}
