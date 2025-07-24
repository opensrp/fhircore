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

import com.google.cloud.speech.v1p1beta1.RecognizeResponse
import com.google.cloud.speech.v1p1beta1.SpeechClient
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SpeechToTextTest {

  private lateinit var mockSpeechClient: SpeechClient
  private val useRealApi = System.getProperty("USE_REAL_API")?.toBoolean() ?: false

  @Before
  fun setUp() {
    if (!useRealApi) {
      mockSpeechClient = mockk(relaxed = true)
      mockkStatic(SpeechClient::class)
      every { SpeechClient.create() } returns mockSpeechClient
    }
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun testTranscribeAudioToTextShouldReturnTemporaryFileWithTranscription() {
    if (useRealApi) {
      testTranscribeAudioToTextRealApi()
    } else {
      testTranscribeAudioToTextMock()
    }
  }

  private fun testTranscribeAudioToTextRealApi() = runTest {
    val workingDir = System.getProperty("user.dir")
    val testFile =
      File(
        workingDir,
        "src/test/java/org/smartregister/fhircore/quest/resources/sample_conversation.wav",
      )
    require(testFile.exists()) { "Test audio file not found at ${testFile.absolutePath}" }

    val resultFile = SpeechToText.transcribeAudioToText(testFile)
    assertNotNull(resultFile, "Result file should not be null")
    assertTrue(resultFile.exists(), "Result file should exist")
    assertContains(resultFile.readText(), "what is your")
  }

  private fun testTranscribeAudioToTextMock() = runTest {
    val workingDir = System.getProperty("user.dir")
    val audioFile =
      File(
        workingDir,
        "src/test/java/org/smartregister/fhircore/quest/resources/sample_conversation.wav",
      )
    require(audioFile.exists()) { "Test audio file not found at ${audioFile.absolutePath}" }

    val transcriptFile =
      File(
        workingDir,
        "src/test/java/org/smartregister/fhircore/quest/resources/sample_transcript.txt",
      )
    require(transcriptFile.exists()) {
      "Transcript file not found at ${transcriptFile.absolutePath}"
    }

    val mockResult =
      SpeechRecognitionResult.newBuilder()
        .addAlternatives(
          SpeechRecognitionAlternative.newBuilder().setTranscript(transcriptFile.readText().trim()),
        )
        .build()
    val mockResponse = RecognizeResponse.newBuilder().addResults(mockResult).build()
    every { mockSpeechClient.recognize(any(), any()) } returns mockResponse

    val resultFile = SpeechToText.transcribeAudioToText(audioFile)
    assertNotNull(resultFile, "Result file should not be null")
    assertTrue(resultFile.exists(), "Result file should exist")
    assertEquals(
      transcriptFile.readText().trim(),
      resultFile.readText().trim(),
      "Transcription content should match the expected content",
    )
  }
}
