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

import org.hl7.fhir.r4b.model.Bundle
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel

class Summarize<T>(
  private val llmModel: LlmModel<T>,
) {
  suspend fun summarize(bundle: Bundle): String? {
    val prompt = generatePrompt(bundle)
    return llmModel.generateContent(prompt)
  }

  private fun generatePrompt(bundle: Bundle): String {
    // TODO convert bundle to text and add to prompt
    return "Summarize the patient's medical history"
  }
}
