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

import com.google.ai.client.generativeai.GenerativeModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.After
import org.junit.Before
import org.junit.Test

class TextToFormTest {

  private lateinit var textToForm: TextToForm
  private lateinit var mockGenerativeModel: GenerativeModel
  private val useRealApi = System.getProperty("USE_REAL_API")?.toBoolean() ?: false

  @Before
  fun setUp() {
    if (!useRealApi) {
      mockGenerativeModel = mockk(relaxed = true)
      textToForm = TextToForm(mockGenerativeModel)
    } else {
      val geminiModel = GeminiModel()
      textToForm = TextToForm(geminiModel.getGeminiModel())
    }
  }

  @After
  fun tearDown() {
    if (!useRealApi) unmockkAll()
  }

  @Test
  fun testGenerateQuestionnaireResponseShouldReturnQuestionnaireResponse() = runTest {
    if (useRealApi) {
      testGenerateQuestionnaireResponseRealApi()
    } else {
      testGenerateQuestionnaireResponseMock()
    }
  }

  private suspend fun testGenerateQuestionnaireResponseRealApi() {
    val testFile = File("src/test/resources/sample_transcript.txt")
    require(testFile.exists()) { "Test transcript file not found at ${testFile.absolutePath}" }
    val mockQuestionnaire = Questionnaire()

    val result = textToForm.generateQuestionnaireResponse(testFile, mockQuestionnaire)
    assertNotNull(result, "QuestionnaireResponse should not be null")
    println("Generated QuestionnaireResponse: ${result.id}")
  }

  private suspend fun testGenerateQuestionnaireResponseMock() {
    val mockTranscriptFile = mockk<File>(relaxed = true)
    val mockQuestionnaire = mockk<Questionnaire>(relaxed = true)
    val mockResponseJson = "{'id': '123'}" // Mock JSON response

    every { mockTranscriptFile.readText() } returns "This is a test transcript."
    coEvery { mockGenerativeModel.generateContent(any(String::class)) } returns
      mockk { every { text } returns "```json\n$mockResponseJson\n```" }

    val result = textToForm.generateQuestionnaireResponse(mockTranscriptFile, mockQuestionnaire)
    assertNotNull(result, "QuestionnaireResponse should not be null")
    assertEquals("123", result.id, "QuestionnaireResponse ID should match")
  }
}
