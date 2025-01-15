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

package org.smartregister.fhircore.quest.medintel.speech.speechtoform

import ca.uhn.fhir.interceptor.model.RequestPartitionId.fromJson
import java.io.File
import java.util.logging.Logger
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.json.JSONObject
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel
import org.smartregister.fhircore.quest.medintel.speech.validation.QuestionnaireResponseValidator
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity

private const val s = "```json"

class TextToForm(
  private val llmModel: LlmModel,
  private val maxRetries: Int = DEFAULT_MAX_RETRIES,
) {

  private val logger = Logger.getLogger(TextToForm::class.java.name)

  /**
   * Generates an HL7 FHIR QuestionnaireResponse from a transcript using the provided Questionnaire.
   * Includes retry logic if the response is invalid.
   *
   * @param transcriptFile The temporary file containing the transcript text.
   * @param questionnaire The FHIR Questionnaire to base the response on.
   * @return The generated and validated QuestionnaireResponse or null if generation fails after
   *   retry.
   */
  suspend fun generateQuestionnaireResponse(
    transcriptFile: File,
    questionnaire: Questionnaire,
  ): QuestionnaireResponse {
    val transcript = transcriptFile.readText()
    var retryCount = 0
    var questionnaireResponse = QuestionnaireResponse()
    var prompt = promptTemplate(transcript, questionnaire)

    do {
      logger.info("Sending request to Gemini...")
      val generatedText = llmModel.generateContent(prompt)
      val errors: List<String>
      val questionnaireResponseJson = extractJsonBlock(generatedText)

      if (questionnaireResponseJson == null) {
        logger.severe("Failed to extract JSON block from Gemini response.")
        errors = listOf("Failed to extract JSON block from Gemini response.")
      } else {
        questionnaireResponse = parseQuestionnaireResponse(questionnaireResponseJson)
        errors =
          QuestionnaireResponseValidator.getQuestionnaireResponseErrorsAsStrings(
            questionnaire,
            questionnaireResponse,
            QuestionnaireActivity(),
            DefaultDispatcherProvider(),
          )
      }
      if (errors.isEmpty()) {
        logger.info("QuestionnaireResponse validated successfully.")
        retryCount = maxRetries
      } else {
        logger.warning("QuestionnaireResponse validation failed.")

        // Build retry prompt with errors and invalid response for next retry
        prompt =
          buildRetryPrompt(
            transcript = transcript,
            errors = errors,
            invalidResponse = questionnaireResponse,
            questionnaire = questionnaire,
          )

        retryCount++
      }
    } while (retryCount < maxRetries)

    return questionnaireResponse
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
    if (responseText != null) {
      val start = responseText.indexOf(JSON_CODE_BLOCK_START)

      if (start != -1) {
        val end = responseText.indexOf(JSON_CODE_BLOCK_END, start + JSON_CODE_BLOCK_START_LENGTH)

        if (end != -1) {
          return responseText
            .substring(
              start + JSON_CODE_BLOCK_START_LENGTH,
              end,
            )
            .trim()
        }
      }
    }
    return null
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
   * Builds the retry prompt based on the errors, invalid response, and original questionnaire.
   *
   * @param transcript The transcript of the conversation.
   * @param errors List of errors encountered in the original response.
   * @param invalidResponse The original invalid QuestionnaireResponse.
   * @param questionnaire The FHIR Questionnaire to base the response on.
   * @return The retry prompt string to be sent to the Gemini model.
   */
  private fun buildRetryPrompt(
    transcript: String,
    errors: List<String>,
    invalidResponse: QuestionnaireResponse?,
    questionnaire: Questionnaire,
  ): String {
    return """
    You are a scribe created to turn conversational text into structure HL7 FHIR output. The
    previous attempt to generate the QuestionnaireResponse was invalid. Below is the list of errors
    encountered: <errors>${errors.joinToString("\n")}</errors>

    The invalid QuestionnaireResponse was:
    <invalidResponse>$invalidResponse</invalidResponse>

    Avoiding the errors above, retry generating the QuestionnaireResponse based on the conversation
    transcript below and the FHIR Questionnaire.
    
    <transcript>$transcript</transcript>
    <questionnaire>$questionnaire</questionnaire>
            """
      .trimIndent()
  }

  companion object {
    private const val DEFAULT_MAX_RETRIES = 3
    private const val JSON_CODE_BLOCK_START = "```json"
    private const val JSON_CODE_BLOCK_END = "```"
    private const val JSON_CODE_BLOCK_START_LENGTH = JSON_CODE_BLOCK_START.length
  }
}
