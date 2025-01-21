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

package org.smartregister.fhircore.quest.medintel.summarization

import ca.uhn.fhir.context.FhirContext
import com.google.ai.client.generativeai.GenerativeModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.smartregister.fhircore.quest.medintel.speech.models.GeminiModel
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel
import org.smartregister.fhircore.quest.medintel.speech.speechtoform.TextToForm
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class SummarizeTest : RobolectricTest() {

  private lateinit var llmModel: LlmModel<GenerativeModel>
  private val fhirContext = FhirContext.forR4()
  private val useGeminiApi = System.getProperty("USE_GEMINI_API")?.toBoolean() ?: false

  @After
  override fun tearDown() {
    unmockkAll()
  }

  @Test
  fun testSummarizeShouldReturnSummary() = runTest {
    if (useGeminiApi) {
      testSummarizeRealApi()
    } else {
      llmModel = mockk(relaxed = true)
      testSummarizeMock()
    }
  }

  private suspend fun testSummarizeRealApi() {
    val workingDir = System.getProperty("user.dir")
    val geminiModel = GeminiModel()

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
    val questionnaireResponse =
      TextToForm.parseQuestionnaireResponse(questionnaireResponseContent, fhirContext)
    val summary =
      Summarize.summarize(
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
        llmModel = geminiModel,
      )
    assertNotNull(summary)
    assertTrue(summary.isNotEmpty())
  }

  private suspend fun testSummarizeMock() {
    val workingDir = System.getProperty("user.dir")

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
    val questionnaireResponse =
      TextToForm.parseQuestionnaireResponse(questionnaireResponseContent, fhirContext)
    val mockSummary =
      "The patient, John Doe Smith, is a 39-year-old male caregiver who registered with the system. He provided his national ID number, mobile phone number, and GPS location. He also selected his location as Province 1, District 2, and Village 3."

    coEvery { llmModel.generateContent(any(String::class)) } returns mockSummary
    val summary =
      Summarize.summarize(
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
        llmModel = llmModel,
      )
    assertNotNull(summary)
    assertTrue(summary.isNotEmpty())
  }
}
