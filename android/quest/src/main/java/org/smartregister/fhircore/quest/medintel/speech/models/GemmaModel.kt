package org.smartregister.fhircore.quest.medintel.speech.models

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class GemmaModel(context: Context) : LlmModel<LlmInference> {
  private val modelPath = "/data/local/.../"
  private val options = LlmInference.LlmInferenceOptions.builder()
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