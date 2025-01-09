package org.smartregister.fhircore.quest.audiointerface.models

import com.google.ai.client.generativeai.GenerativeModel

abstract class LlmModel {
  abstract val model: GenerativeModel

  /**
   * Returns the configured GenerativeModel instance.
   *
   * @return The GenerativeModel instance.
   */
  fun getModel(): GenerativeModel = model

  abstract suspend fun generateContent(prompt: String): String?
}