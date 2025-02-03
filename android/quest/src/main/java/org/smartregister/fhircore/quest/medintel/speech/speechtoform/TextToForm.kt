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

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.json.JSONObject
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel
import org.smartregister.fhircore.quest.util.QuestionnaireResponseUtils
import timber.log.Timber
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString

class TextToForm @Inject constructor(val dispatcherProvider: DispatcherProvider) {

  private val DEFAULT_MAX_RETRIES = 3
  private val JSON_CODE_BLOCK_START = "```json"
  private val JSON_CODE_BLOCK_END = "```"
  private val JSON_CODE_BLOCK_START_LENGTH = JSON_CODE_BLOCK_START.length

  /**
   * Generates an HL7 FHIR QuestionnaireResponse from a transcript using the provided Questionnaire.
   * Includes retry logic if the response is invalid.
   *
   * @param transcriptFile The temporary file containing the transcript text.
   * @param questionnaire The FHIR Questionnaire to base the response on.
   * @return The generated and validated QuestionnaireResponse or null if generation fails after
   *   retry.
   */
  suspend fun <T> generateQuestionnaireResponse(
    transcriptFile: File,
    questionnaire: Questionnaire,
    context: Context,
    llmModel: LlmModel<T>,
  ): QuestionnaireResponse {
    val transcript = transcriptFile.readText()
    return generateQuestionnaireResponse(transcript, questionnaire, context, llmModel)
  }

  /**
   * Generates an HL7 FHIR QuestionnaireResponse from a transcript using the provided Questionnaire.
   * Includes retry logic if the response is invalid.
   *
   * @param transcript The transcript text.
   * @param questionnaire The FHIR Questionnaire to base the response on.
   * @return The generated and validated QuestionnaireResponse or null if generation fails after
   *   retry.
   */
  suspend fun <T> generateQuestionnaireResponse(
    transcript: String,
    questionnaire: Questionnaire,
    context: Context,
    llmModel: LlmModel<T>,
  ): QuestionnaireResponse {
    var retryCount = 0
    var questionnaireResponse = QuestionnaireResponse()
    var prompt = promptTemplate(transcript, questionnaire)
    val fhirContext = FhirContext.forR4()

    do {
      Timber.i("Sending request to Gemini...")
      val generatedText = withContext(dispatcherProvider.io()) { llmModel.generateContent(prompt) }
      val errors: List<String>
      val questionnaireResponseJson = extractJsonBlock(generatedText)

      if (questionnaireResponseJson == null) {
        Timber.e("Failed to extract JSON block from Gemini response.")
        errors = listOf("Failed to extract JSON block from Gemini response.")
      } else {
        questionnaireResponse = parseQuestionnaireResponse(questionnaireResponseJson, fhirContext)
        errors =
          withContext(dispatcherProvider.default()) {
            QuestionnaireResponseUtils.getQuestionnaireResponseErrorsAsStrings(
              questionnaire = questionnaire,
              questionnaireResponse = questionnaireResponse,
              context = context,
            )
          }
      }
      if (errors.isEmpty()) {
        Timber.i("QuestionnaireResponse validated successfully.")
        retryCount = DEFAULT_MAX_RETRIES
      } else {
        Timber.w("QuestionnaireResponse validation failed. Retrying")
        // Determine the invalid response to pass to buildRetryPrompt
        val invalidResponse =
          questionnaireResponse
            .takeIf { it.hasId() }
            ?.let { fhirContext.newJsonParser().encodeResourceToString(it) }
            ?: questionnaireResponseJson?.takeIf { it.isNotBlank() }
            ?: generatedText

        // Build retry prompt with errors and invalid response for next retry
        prompt =
          buildRetryPrompt(
            transcript = transcript,
            errors = errors,
            invalidResponse = invalidResponse,
            questionnaire = questionnaire,
            fhirContext = fhirContext,
          )

        retryCount++
      }
    } while (retryCount < DEFAULT_MAX_RETRIES)

    Timber.d("QuestionnaireResponse:%s", questionnaireResponse.encodeResourceToString())

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
  fun extractJsonBlock(responseText: String?): String? {
    if (responseText != null) {
      try {
        JSONObject(responseText)
        return responseText
      } catch (e: Exception) {
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
    }
    return null
  }

  /**
   * Parses the JSON string into a QuestionnaireResponse object.
   *
   * @param json The JSON string representing the QuestionnaireResponse.
   * @return The parsed QuestionnaireResponse object.
   */
  fun parseQuestionnaireResponse(json: String, fhirContext: FhirContext): QuestionnaireResponse {
    val parser = fhirContext.newJsonParser()
    return parser.parseResource(QuestionnaireResponse::class.java, json)
  }

  /**
   * Parses the JSON string into a Questionnaire object.
   *
   * @param json The JSON string representing the Questionnaire.
   * @return The parsed Questionnaire object.
   */
  fun parseQuestionnaire(json: String, fhirContext: FhirContext): Questionnaire {
    val parser = fhirContext.newJsonParser()
    return parser.parseResource(Questionnaire::class.java, json)
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
  fun buildRetryPrompt(
    transcript: String,
    errors: List<String>,
    invalidResponse: String?,
    questionnaire: Questionnaire,
    fhirContext: FhirContext,
  ): String {
    val jsonParser = fhirContext.newJsonParser().setPrettyPrint(true)
    return """
    You are a scribe created to turn conversational text into structure HL7 FHIR output. The
    previous attempt to generate the QuestionnaireResponse was invalid. Below is the list of errors
    encountered: <errors>${errors.joinToString("\n")}</errors>

    The invalid QuestionnaireResponse was:
    <invalidResponse>$invalidResponse</invalidResponse>

    Avoiding the errors above, retry generating the QuestionnaireResponse based on the conversation
    transcript below and the FHIR Questionnaire.
    
    <transcript>$transcript</transcript>
    <questionnaire>${jsonParser.encodeResourceToString(questionnaire)}</questionnaire>
            """
      .trimIndent()
  }
}
