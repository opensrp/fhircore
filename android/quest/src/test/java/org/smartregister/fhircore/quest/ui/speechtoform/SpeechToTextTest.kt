package org.smartregister.fhircore.quest.ui.speechtoform

import com.google.cloud.speech.v1.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpeechToTextTest {

    private lateinit var speechToText: SpeechToText
    private lateinit var mockSpeechClient: SpeechClient

    @Before
    fun setUp() {
        // Initialize the SpeechToText instance and mock dependencies
        mockSpeechClient = mockk(relaxed = true)
        mockkStatic(SpeechClient::class)
        every { SpeechClient.create() } returns mockSpeechClient

        speechToText = SpeechToText()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun transcribeAudioToTextShouldReturnTemporaryFileWithTranscription() {
        val mockAudioFile = mockk<File>(relaxed = true)
        val mockAudioBytes = "test audio bytes".toByteArray()
        every { mockAudioFile.readBytes() } returns mockAudioBytes

        // Mock SpeechClient response
        val mockRecognitionAudio = RecognitionAudio.newBuilder()
            .setContent(com.google.protobuf.ByteString.copyFrom(mockAudioBytes))
            .build()
        val mockConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(16000)
            .setLanguageCode("en-US")
            .build()
        val mockResult = SpeechRecognitionResult.newBuilder()
            .addAlternatives(SpeechRecognitionAlternative.newBuilder().setTranscript("Hello World"))
            .build()
        val mockResponse = RecognizeResponse.newBuilder()
            .addResults(mockResult)
            .build()
        every { mockSpeechClient.recognize(mockConfig, mockRecognitionAudio) } returns mockResponse

        val resultFile = speechToText.transcribeAudioToText(mockAudioFile)

        assertNotNull(resultFile, "Result file should not be null")
        assertTrue(resultFile.exists(), "Result file should exist")
        assertEquals("Hello World", resultFile.readText(), "Transcription content should match")
    }
}
