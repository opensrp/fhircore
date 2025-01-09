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

package org.smartregister.fhircore.quest.audiointerface.speechtoform

import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionResult
import java.io.File
import java.util.logging.Logger

class SpeechToText {

  private val logger = Logger.getLogger(SpeechToText::class.java.name)

  /**
   * Transcribes an audio file to text using Google Cloud Speech-to-Text API and writes it to a
   * temporary file.
   *
   * @param audioFile The audio file to be transcribed.
   * @return The temporary file containing the transcribed text.
   */
  fun transcribeAudioToText(audioFile: File): File? {
    var tempFile: File? = null

    SpeechClient.create().use { speechClient ->
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

      logger.info("Transcription: $transcription")

      // Write transcription to a temporary file
      tempFile = File.createTempFile("transcription", ".txt")
      tempFile?.writeText(transcription)

      logger.info("Transcription written to temporary file. ")
    }
    return tempFile
  }
}
