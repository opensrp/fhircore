package org.smartregister.fhircore.quest.ui.speechtoform

import ca.uhn.fhir.interceptor.model.RequestPartitionId.fromJson
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class TextToFormTest {

    private lateinit var generativeModelMock: GeminiClient
    private lateinit var textToForm: TextToForm
    private lateinit var transcriptFileMock: File
    private lateinit var questionnaireMock: Questionnaire

    @Before
    fun setUp() {
        // Mock dependencies
        generativeModelMock = mockk()
        transcriptFileMock = mockk()
        questionnaireMock = mockk()


        textToForm = TextToForm(generativeModelMock)
    }

    @Test
    fun `generateQuestionnaireResponse should return valid QuestionnaireResponse for valid inputs`() = runBlocking {
        // todo mock client
        val transcript = "Patient has a mild fever and sore throat."
        val prompt = """
            You are a scribe created to turn conversational text into structure HL7 FHIR output. Below
            you will see the text Transcript of a conversation between a nurse and a patient within
            <transcript> XML tags and an HL7 FHIR Questionnaire within <questionnaire> XML tags. Your job
            is to convert the text in Transcript into a new HL7 FHIR QuestionnaireResponse as if the
            information in Transcript had been entered directly into the FHIR Questionniare. Only output
            the FHIR QuestionnaireResponse as JSON and nothing else.
            <transcript>$transcript</transcript>
            <questionnaire>$questionnaireMock</questionnaire>
        """.trimIndent()
        val generativeModelResponse = """
            ```json
            {
                "resourceType": "QuestionnaireResponse",
                "status": "completed",
                "item": [
                    {
                        "linkId": "symptoms",
                        "answer": [
                            {"valueString": "fever"},
                            {"valueString": "sore throat"}
                        ]
                    }
                ]
            }
            ```
        """.trimIndent()
        val questionnaireResponseJson = JSONObject(
            """
            {
                "resourceType": "QuestionnaireResponse",
                "status": "completed",
                "item": [
                    {
                        "linkId": "symptoms",
                        "answer": [
                            {"valueString": "fever"},
                            {"valueString": "sore throat"}
                        ]
                    }
                ]
            }
            """
        ).toString()

        val expectedQuestionnaireResponse = QuestionnaireResponse().apply { fromJson(questionnaireResponseJson) }

        every { transcriptFileMock.readText() } returns transcript
        coEvery { generativeModelMock.generateContent(prompt) } returns generativeModelResponse

        val result = textToForm.generateQuestionnaireResponse(transcriptFileMock, questionnaireMock)

        assertNotNull(result)
        assertEquals(expectedQuestionnaireResponse, result)
        coVerify { generativeModelMock.generateContent(prompt) }
    }
}
