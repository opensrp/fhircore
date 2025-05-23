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

package org.smartregister.fhircore.quest.medintel.speech.models

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import org.smartregister.fhircore.quest.BuildConfig

class GeminiModel : LlmModel<GenerativeModel> {
  // model usage
  // https://developer.android.com/ai/google-ai-client-sdk
  override var model =
    GenerativeModel(
      modelName = "gemini-2.5-flash-preview-04-17",
      apiKey = BuildConfig.GEMINI_API_KEY,
      generationConfig =
        generationConfig {
          temperature = 0.15f
          topK = 32
          topP = 1f
          maxOutputTokens = 4096
        },
      safetySettings =
        listOf(
          SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
          SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
          SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
          SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
        ),
    )

  /**
   * Generates content based on the provided prompt.
   *
   * @param prompt The prompt string to generate content from.
   * @return The generated content as a string or null.
   */
  override suspend fun generateContent(prompt: String): String? {
    return model.generateContent(prompt).text
  }
}
