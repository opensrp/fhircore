package org.smartregister.fhircore.quest.ui.speechtoform

import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import java.io.File
import java.util.logging.Logger

class SpeechToForm(
    private val speechToText: SpeechToText,
    private val textToForm: TextToForm
) {

    private val logger = Logger.getLogger(SpeechToForm::class.java.name)

    /**
     * Reads an audio file, transcribes it, and generates a FHIR QuestionnaireResponse.
     *
     * @param audioFile The input audio file to process.
     * @param questionnaire The FHIR Questionnaire used to generate the response.
     * @return The generated QuestionnaireResponse, or null if the process fails.
     */
    fun processAudioToQuestionnaireResponse(audioFile: File, questionnaire: Questionnaire): QuestionnaireResponse? {
        logger.info("Starting audio transcription process...")

        // Step 1: Transcribe audio to text
        val tempTextFile = speechToText.transcribeAudioToText(audioFile)
        if (tempTextFile == null) {
            logger.severe("Failed to transcribe audio.")
            return null
        }
        logger.info("Transcription successful. File path: ${tempTextFile.absolutePath}")

        // Step 2: Generate QuestionnaireResponse from the transcript
        val questionnaireResponse = textToForm.generateQuestionnaireResponse(tempTextFile, questionnaire)
        if (questionnaireResponse == null) {
            logger.severe("Failed to generate QuestionnaireResponse.")
            return null
        }

        logger.info("QuestionnaireResponse generated successfully.")
        return questionnaireResponse
    }
}
