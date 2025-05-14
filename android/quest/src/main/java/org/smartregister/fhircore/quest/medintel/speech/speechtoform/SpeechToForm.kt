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
import java.io.File
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel
import timber.log.Timber

class SpeechToForm<T>(
  private val textToForm: TextToForm,
  private val llmModel: LlmModel<T>,
) {

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
    context: Context,
  ): QuestionnaireResponse? {
    Timber.i("Starting audio transcription process...")

    // Step 1: Transcribe audio to text
    val tempTextFile = SpeechToText.transcribeAudioToText(audioFile)
    if (tempTextFile == null) {
      Timber.e("Failed to transcribe audio.")
      return null
    }
    Timber.i("Transcription successful. File path: ${tempTextFile.absolutePath}")

    // Step 2: Generate QuestionnaireResponse from the transcript
    val questionnaireResponse =
      textToForm.generateQuestionnaireResponse(tempTextFile, questionnaire, context, llmModel)

    Timber.i("QuestionnaireResponse generated successfully.")
    return questionnaireResponse
  }
}
