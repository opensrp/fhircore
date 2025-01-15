package org.smartregister.fhircore.quest.medintel.summarization;

import org.hl7.fhir.r4b.model.Bundle;
import org.smartregister.fhircore.quest.medintel.speech.models.LlmModel;

class Summarize(
    private val llmModel: LlmModel
) {
    suspend fun summarize(bundle: Bundle): String? {
        val prompt = generatePrompt(bundle);
        return llmModel.generateContent(prompt)
    }

    private fun generatePrompt(bundle: Bundle): String {
        // TODO convert bundle to text and add to prompt
        return "Summarize the patient's medical history";
    }
}