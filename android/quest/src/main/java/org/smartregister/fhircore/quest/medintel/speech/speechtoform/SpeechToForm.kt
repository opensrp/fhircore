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

import java.io.File
import java.util.logging.Logger
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel

class SpeechToForm(
  private val speechToText: SpeechToText,
  llmModel: LlmModel,
) {
  private val textToForm: TextToForm = TextToForm(llmModel)
  private val logger = Logger.getLogger(SpeechToForm::class.java.name)

  /**
   * Reads an audio file, transcribes it, and generates a FHIR QuestionnaireResponse.
   *
   * @param audioFile The input audio file to process.
   * @param questionnaire The FHIR Questionnaire used to generate the response.
   * @return The generated QuestionnaireResponse, or null if the process fails.
   */
  suspend fun processAudioToQuestionnaireResponse(
    audioFile: File,
    questionnaire: Questionnaire,
  ): QuestionnaireResponse? {
    logger.info("Starting audio transcription process...")

    // Step 1: Transcribe audio to text
    val tempTextFile = speechToText.transcribeAudioToText(audioFile)
    if (tempTextFile == null) {
      logger.severe("Failed to transcribe audio.")
      return null
    }
    logger.info("Transcription successful. File path: ${tempTextFile.absolutePath}")

    // Step 2: Generate QuestionnaireResponse from the transcript
    val questionnaireResponse =
      textToForm.generateQuestionnaireResponse(tempTextFile, questionnaire)
    if (questionnaireResponse == null) {
      logger.severe("Failed to generate QuestionnaireResponse.")
      return null
    }

    logger.info("QuestionnaireResponse generated successfully.")
    return questionnaireResponse
  }
}
