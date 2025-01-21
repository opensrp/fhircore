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
import com.google.ai.client.generativeai.GenerativeModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.After
import org.junit.Test
import org.smartregister.fhircore.quest.medintel.speech.models.GeminiModel
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class TextToFormTest : RobolectricTest() {

  private lateinit var mockLlmModel: LlmModel<GenerativeModel>
  private val useGeminiApi = System.getProperty("USE_GEMINI_API")?.toBoolean() ?: false

  @After
  override fun tearDown() {
    super.tearDown()
    if (!useGeminiApi) unmockkAll()
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

    val questionnaire = TextToForm.parseQuestionnaire(questionnaireContent)

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
