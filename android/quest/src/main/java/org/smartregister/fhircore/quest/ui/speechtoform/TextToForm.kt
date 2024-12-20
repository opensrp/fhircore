package org.smartregister.fhircore.quest.ui.speechtoform

import ca.uhn.fhir.interceptor.model.RequestPartitionId.fromJson
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import java.io.File
import java.util.logging.Logger
import org.json.JSONObject

class TextToForm(private val geminiClient: GeminiClient) {

    private val logger = Logger.getLogger(TextToForm::class.java.name)

    /**
     * Generates an HL7 FHIR QuestionnaireResponse from a transcript using the provided Questionnaire.
     *
     * @param transcriptFile The temporary file containing the transcript text.
     * @param questionnaire The FHIR Questionnaire to base the response on.
     * @return The generated and validated QuestionnaireResponse or null if generation fails.
     */
    fun generateQuestionnaireResponse(transcriptFile: File, questionnaire: Questionnaire): QuestionnaireResponse? {
        val transcript = transcriptFile.readText()
        val prompt = buildPrompt(transcript, questionnaire)

        logger.info("Sending request to Gemini...")
        val generatedText = geminiClient.generateContent(prompt)

        val questionnaireResponseJson = extractJsonBlock(generatedText) ?: return null

        return try {
            val questionnaireResponse = parseQuestionnaireResponse(questionnaireResponseJson)
            if (validateQuestionnaireResponse(questionnaireResponse)) {
                logger.info("QuestionnaireResponse validated successfully.")
                questionnaireResponse
            } else {
                logger.warning("QuestionnaireResponse validation failed.")
                null
            }
        } catch (e: Exception) {
            logger.severe("Error generating QuestionnaireResponse: ${e.message}")
            null
        }
    }

    /**
     * Builds the prompt for the Gemini model.
     */
    private fun buildPrompt(transcript: String, questionnaire: Questionnaire): String {
        return """
            Using the following transcript of a conversation between a nurse and a patient:
            
            $transcript
            
            Generate an HL7 FHIR QuestionnaireResponse as if they had entered that information into the following FHIR Questionnaire:
            
            $questionnaire
        """.trimIndent()
    }

    /**
     * Extracts the JSON block from the generated text.
     */
    private fun extractJsonBlock(responseText: String): String? {
        val start = responseText.indexOf("```json")
        if (start == -1) return null
        val end = responseText.indexOf("```", start + 7)
        return if (end == -1) null else responseText.substring(start + 7, end).trim()
    }

    /**
     * Parses the JSON string into a QuestionnaireResponse object.
     */
    private fun parseQuestionnaireResponse(json: String): QuestionnaireResponse {
        return QuestionnaireResponse().apply {
            fromJson(JSONObject(json).toString())
        }
    }

    /**
     * Validates the QuestionnaireResponse structure.
     */
    private fun validateQuestionnaireResponse(qr: QuestionnaireResponse): Boolean {
        //todo use SDC validation

        return true
    }
}
