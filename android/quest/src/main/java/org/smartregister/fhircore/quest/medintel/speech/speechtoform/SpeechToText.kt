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

import com.google.cloud.speech.v1p1beta1.RecognitionAudio
import com.google.cloud.speech.v1p1beta1.RecognitionConfig
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding
import com.google.cloud.speech.v1p1beta1.SpeechClient
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult
import java.io.File
import timber.log.Timber

object SpeechToText {
  /**
   * Transcribes an audio file to text using Google Cloud Speech-to-Text API and writes it to a
   * temporary file.
   *
   * @param audioFile The audio file to be transcribed.
   * @return The temporary file containing the transcribed text.
   */
  suspend fun transcribeAudioToText(audioFile: File): File? {
    if (!isValidWavFile(audioFile)) {
      Timber.e("Invalid file format. Please provide a WAV file.")
      return null
    }
    val outputFile = File.createTempFile("transcription", ".txt")

    SpeechClient.create().use { speechClient ->
      transcribeAudioToTextFile(speechClient, audioFile, textFile = outputFile)
    }
    return outputFile
  }

  @Suppress("RedundantSuspendModifier")
  private suspend fun transcribeAudioToTextFile(
    speechClient: SpeechClient,
    audioFile: File,
    textFile: File,
  ) {
    val audioBytes = audioFile.readBytes()

    // Build the recognition audio
    val recognitionAudio =
      RecognitionAudio.newBuilder()
        .setContent(com.google.protobuf.ByteString.copyFrom(audioBytes))
        .build()

    // Configure recognition settings
    val config =
      RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        .setSampleRateHertz(16000)
        .setLanguageCode("en-US")
        .build()

    // Perform transcription
    val response = speechClient.recognize(config, recognitionAudio)
    val transcription =
      response.resultsList.joinToString(" ") { result: SpeechRecognitionResult ->
        result.alternativesList[0].transcript
      }

    Timber.i("Transcription: $transcription")

    // Write transcription to a temporary file
    textFile.writeText(transcription)

    Timber.i("Transcription written to temporary file. ")
  }

  /**
   * Checks if the provided file is a valid WAV file based on its extension and content type.
   *
   * @param file The file to validate.
   * @return True if the file is a valid WAV file; false otherwise.
   */
  private fun isValidWavFile(file: File): Boolean {
    if (!file.extension.equals("wav", ignoreCase = true)) {
      Timber.e("File extension is not .wav")
      return false
    }
    return true
  }
}
