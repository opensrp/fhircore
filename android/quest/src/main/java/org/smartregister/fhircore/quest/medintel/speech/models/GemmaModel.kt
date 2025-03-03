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

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import org.smartregister.fhircore.quest.medintel.speech.models.GemmaModel.Companion.DEFAULT_MAX_TOKENS
import org.smartregister.fhircore.quest.medintel.speech.models.GemmaModel.Companion.DEFAULT_RANDOM_SEED
import org.smartregister.fhircore.quest.medintel.speech.models.GemmaModel.Companion.DEFAULT_TEMPERATURE
import org.smartregister.fhircore.quest.medintel.speech.models.GemmaModel.Companion.DEFAULT_TOP_K

class GemmaModel(llmInferenceType: LlmInferenceType) : LlmModel<LlmInferenceType> {

  override var model: LlmInferenceType = llmInferenceType

  override suspend fun generateContent(prompt: String): String? {
    return model.generateResponse(prompt)
  }

  companion object {
    const val DEFAULT_MAX_TOKENS = 1000
    const val DEFAULT_TOP_K = 40
    const val DEFAULT_TEMPERATURE = 0.8F
    const val DEFAULT_RANDOM_SEED = 101
  }
}

fun GemmaModel(context: Context, modelPath: String): GemmaModel {
  val options =
    LlmInference.LlmInferenceOptions.builder()
      .setModelPath(modelPath)
      .setMaxTokens(DEFAULT_MAX_TOKENS)
      .setTopK(DEFAULT_TOP_K)
      .setTemperature(DEFAULT_TEMPERATURE)
      .setRandomSeed(DEFAULT_RANDOM_SEED)
      .build()
  val llmInference = LlmInferenceWrapper(LlmInference.createFromOptions(context, options))

  return GemmaModel(llmInference)
}

sealed interface LlmInferenceType {
  fun generateResponse(inputText: String): String?
}

interface ILlmInference : LlmInferenceType

class LlmInferenceWrapper(val llmInference: LlmInference) : LlmInferenceType {
  override fun generateResponse(inputText: String): String? =
    llmInference.generateResponse(inputText)
}
