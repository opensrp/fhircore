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

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.ai.client.generativeai.GenerativeModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Test
import org.smartregister.fhircore.quest.medintel.speech.models.GeminiModel
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class TextToFormTest : RobolectricTest() {

  private lateinit var mockLlmModel: LlmModel<GenerativeModel>
  private val fhirContext = FhirContext.forR4()
  private val useGeminiApi = System.getProperty("USE_GEMINI_API")?.toBoolean() ?: false

  @After
  override fun tearDown() {
    unmockkAll()
  }

  @Test
  fun testGenerateQuestionnaireResponseShouldReturnQuestionnaireResponse() = runTest {
    if (useGeminiApi) {
      testGenerateQuestionnaireResponseRealApi()
    } else {
      mockLlmModel = mockk(relaxed = true)
      testGenerateQuestionnaireResponseMock()
    }
  }

  @Test
  fun testExtractJsonBlockWithValidJson() {
    val responseText =
      """```json
    {
      "resourceType": "QuestionnaireResponse",
      "id": "test-id"
    }
    ```"""
    val jsonBlock = TextToForm.extractJsonBlock(responseText)
    assertNotNull(jsonBlock, "JSON block should not be null")
    assertEquals(
      """{
      "resourceType": "QuestionnaireResponse",
      "id": "test-id"
    }
            """
        .trimIndent(),
      jsonBlock,
      "JSON block content should match",
    )
  }

  @Test
  fun testExtractJsonBlockWithInvalidJson() {
    val responseText = "Invalid text without JSON block"
    val jsonBlock = TextToForm.extractJsonBlock(responseText)
    assertEquals(null, jsonBlock, "JSON block should be null for invalid input")
  }

  @Test
  fun testParseQuestionnaireResponseWithValidJson() {
    val json =
      """
    {
      "resourceType": "QuestionnaireResponse",
      "id": "valid-id"
    }
    """
    val questionnaireResponse = TextToForm.parseQuestionnaireResponse(json, fhirContext)
    assertNotNull(questionnaireResponse, "QuestionnaireResponse should not be null")
    assertEquals(
      "QuestionnaireResponse/valid-id",
      questionnaireResponse.id,
      "QuestionnaireResponse ID should match",
    )
  }

  @Test
  fun testParseQuestionnaireResponseWithInvalidJson() {
    val json = "{ invalid-json }"
    try {
      TextToForm.parseQuestionnaireResponse(json, fhirContext)
      fail("Exception should have been thrown for invalid JSON")
    } catch (e: Exception) {
      assertTrue(
        e is ca.uhn.fhir.parser.DataFormatException,
        "Exception should be of type DataFormatException",
      )
    }
  }

  @Test
  fun testBuildRetryPrompt() {
    val errors = listOf("Error 1", "Error 2")
    val invalidQuestionnaire = QuestionnaireResponse().apply { id = "invalid-id" }
    val questionnaire = Questionnaire().apply { id = "questionnaire-id" }
    val transcript = "Sample transcript"

    val jsonParser = fhirContext.newJsonParser().setPrettyPrint(true)

    val invalidResponse = jsonParser.encodeResourceToString(invalidQuestionnaire)

    val prompt =
      TextToForm.buildRetryPrompt(transcript, errors, invalidResponse, questionnaire, fhirContext)
    assertTrue(
      prompt.contains("<errors>Error 1\nError 2</errors>"),
      "Prompt should include errors",
    )
    assertTrue(
      prompt.contains(
        "<invalidResponse>{\n  \"resourceType\": \"QuestionnaireResponse\",\n  \"id\": \"invalid-id\"\n}</invalidResponse>",
      ),
      "Prompt should include invalid response",
    )
    assertTrue(
      prompt.contains("<transcript>Sample transcript</transcript>"),
      "Prompt should include transcript",
    )
    assertTrue(
      prompt.contains(
        "<questionnaire>{\n  \"resourceType\": \"Questionnaire\",\n  \"id\": \"questionnaire-id\"\n}</questionnaire>",
      ),
      "Prompt should include questionnaire",
    )
  }

  private suspend fun testGenerateQuestionnaireResponseRealApi() {
    val workingDir = System.getProperty("user.dir")
    val testFile =
      File(
        workingDir,
        "src/test/java/org/smartregister/fhircore/quest/resources/sample_transcript.txt",
      )
    require(testFile.exists()) { "Test transcript file not found at ${testFile.absolutePath}" }
    val mockQuestionnaire = Questionnaire()
    val geminiModel = GeminiModel()

    val result =
      TextToForm.generateQuestionnaireResponse(
        transcriptFile = testFile,
        questionnaire = mockQuestionnaire,
        context = ApplicationProvider.getApplicationContext(),
        llmModel = geminiModel,
      )
    assertNotNull(result, "QuestionnaireResponse should not be null")
    println("Generated QuestionnaireResponse: ${result.id}")
  }

  private suspend fun testGenerateQuestionnaireResponseMock() {
    val workingDir = System.getProperty("user.dir")
    val transcript =
      File(
        workingDir,
        "src/test/java/org/smartregister/fhircore/quest/resources/sample_transcript.txt",
      )
    val questionnaireContent =
      File(
          workingDir,
          "src/test/java/org/smartregister/fhircore/quest/resources/sample_questionnaire.json",
        )
        .readText()
    val questionnaireResponseContent =
      File(
          workingDir,
          "src/test/java/org/smartregister/fhircore/quest/resources/sample_questionnaire_response.json",
        )
        .readText()

    val questionnaire = TextToForm.parseQuestionnaire(questionnaireContent, fhirContext)

    coEvery { mockLlmModel.generateContent(any(String::class)) } returns
      "```json\n$questionnaireResponseContent\n```"

    val result =
      TextToForm.generateQuestionnaireResponse(
        transcriptFile = transcript,
        questionnaire = questionnaire,
        context = ApplicationProvider.getApplicationContext(),
        llmModel = mockLlmModel,
      )
    assertNotNull(result, "QuestionnaireResponse should not be null")
    assertEquals(
      "QuestionnaireResponse/f8a7d652-a69b-416f-9a6c-7128a2e76667",
      result.id,
      "QuestionnaireResponse ID should match",
    )
  }
}
