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

class GemmaModel(context: Context) : LlmModel<LlmInference> {
  private val modelPath = "/data/local/.../"
  private val options =
    LlmInference.LlmInferenceOptions.builder()
      .setModelPath(modelPath)
      .setMaxTokens(1000)
      .setTopK(40)
      .setTemperature(0.8F)
      .setRandomSeed(101)
      .build()
  override val model = LlmInference.createFromOptions(context, options)

  override suspend fun generateContent(prompt: String): String? {
    return model.generateResponse(prompt)
  }
}
