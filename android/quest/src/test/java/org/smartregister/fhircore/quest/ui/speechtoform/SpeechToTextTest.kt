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

import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeResponse
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1.SpeechRecognitionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class SpeechToTextTest {

  private lateinit var speechToText: SpeechToText
  private lateinit var mockSpeechClient: SpeechClient
  private val useRealApi = System.getProperty("USE_REAL_API")?.toBoolean() ?: false

  @Before
  fun setUp() {
    if (!useRealApi) {
      mockSpeechClient = mockk(relaxed = true)
      mockkStatic(SpeechClient::class)
      every { SpeechClient.create() } returns mockSpeechClient
    }
    speechToText = SpeechToText()
  }

  @After
  fun tearDown() {
    if (!useRealApi) unmockkAll()
  }

  @Test
  fun testTranscribeAudioToTextShouldReturnTemporaryFileWithTranscription() {
    if (useRealApi) {
      testTranscribeAudioToTextRealApi()
    } else {
      testTranscribeAudioToTextMock()
    }
  }

  private fun testTranscribeAudioToTextRealApi() {
    val testFile = File("src/test/resources/sample_audio.wav")
    require(testFile.exists()) { "Test audio file not found at ${testFile.absolutePath}" }

    val resultFile = speechToText.transcribeAudioToText(testFile)
    assertNotNull(resultFile, "Result file should not be null")
    assertTrue(resultFile.exists(), "Result file should exist")
    println("Transcription result: ${resultFile.readText()}")
  }

  private fun testTranscribeAudioToTextMock() {
    val mockAudioFile = mockk<File>(relaxed = true)
    val mockAudioBytes = "test audio bytes".toByteArray()
    every { mockAudioFile.readBytes() } returns mockAudioBytes

    val mockRecognitionAudio =
      RecognitionAudio.newBuilder()
        .setContent(com.google.protobuf.ByteString.copyFrom(mockAudioBytes))
        .build()
    val mockConfig =
      RecognitionConfig.newBuilder()
        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
        .setSampleRateHertz(16000)
        .setLanguageCode("en-US")
        .build()
    val mockResult =
      SpeechRecognitionResult.newBuilder()
        .addAlternatives(SpeechRecognitionAlternative.newBuilder().setTranscript("Hello World"))
        .build()
    val mockResponse = RecognizeResponse.newBuilder().addResults(mockResult).build()
    every { mockSpeechClient.recognize(mockConfig, mockRecognitionAudio) } returns mockResponse

    val resultFile = speechToText.transcribeAudioToText(mockAudioFile)
    assertNotNull(resultFile, "Result file should not be null")
    assertTrue(resultFile.exists(), "Result file should exist")
    assertEquals("Hello World", resultFile.readText(), "Transcription content should match")
  }
}
