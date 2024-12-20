package org.smartregister.fhircore.quest.ui.speechtoform

import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionResult
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding
import java.io.File
import java.util.logging.Logger

class SpeechToText {

    private val logger = Logger.getLogger(SpeechToText::class.java.name)

    /**
     * Transcribes an audio file to text using Google Cloud Speech-to-Text API and writes it to a
     * temporary file.
     *
     * @param audioFile The audio file to be transcribed.
     * @return The temporary file containing the transcribed text.
     */
    fun transcribeAudioToText(audioFile: File): File? {
        var tempFile: File? = null

        SpeechClient.create().use { speechClient ->
            val audioBytes = audioFile.readBytes()

            // Build the recognition audio
            val recognitionAudio = RecognitionAudio.newBuilder()
                .setContent(com.google.protobuf.ByteString.copyFrom(audioBytes))
                .build()

            // Configure recognition settings
            val config = RecognitionConfig.newBuilder()
                .setEncoding(AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                .setLanguageCode("en-US")
                .build()

            // Perform transcription
            val response = speechClient.recognize(config, recognitionAudio)
            val transcription = response.resultsList.joinToString(" ") {
                result: SpeechRecognitionResult ->
                result.alternativesList[0].transcript
            }

            logger.info("Transcription: $transcription")

            // Write transcription to a temporary file
            tempFile = File.createTempFile("transcription", ".txt")
            tempFile?.writeText(transcription)

            logger.info("Transcription written to temporary file. ")
        }
        return tempFile
    }
}
