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

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Candidate
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.TextPart
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class GeminiModelTest {

  private lateinit var geminiModel: GeminiModel
  private lateinit var mockGenerativeModel: GenerativeModel
  private lateinit var mockCandidate: Candidate
  private lateinit var mockContent: Content
  private val expectedResponse = "Hello, user!"

  @Before
  fun setUp() {
    mockGenerativeModel = mock(GenerativeModel::class.java)
    geminiModel = GeminiModel().apply { model = mockGenerativeModel }
    mockCandidate = mock(Candidate::class.java)
    mockContent = mock(Content::class.java)
    `when`(mockCandidate.content).thenReturn(mockContent)
    `when`(mockContent.parts).thenReturn(listOf(TextPart(expectedResponse)))
  }

  @Test
  fun generateContent_handlesNullGeneratedContent() = runBlocking {
    val prompt = "Hello, world!"

    `when`(mockGenerativeModel.generateContent(prompt))
      .thenReturn(
        GenerateContentResponse(
          listOf(mockCandidate),
          null,
          null,
        ),
      )
    val result = geminiModel.generateContent(prompt)
    assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesEmptyPrompt() = runBlocking {
    val prompt = ""
    `when`(mockGenerativeModel.generateContent(prompt))
      .thenReturn(
        GenerateContentResponse(
          listOf(mockCandidate),
          null,
          null,
        ),
      )
    val result = geminiModel.generateContent(prompt)
    assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesLongPrompt() = runBlocking {
    val prompt = "a".repeat(5000)
    `when`(mockGenerativeModel.generateContent(prompt))
      .thenReturn(
        GenerateContentResponse(
          listOf(mockCandidate),
          null,
          null,
        ),
      )
    val result = geminiModel.generateContent(prompt)
    assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesSpecialCharacters() = runBlocking {
    val prompt = "!@#$%^&*()"
    `when`(mockGenerativeModel.generateContent(prompt))
      .thenReturn(
        GenerateContentResponse(
          listOf(mockCandidate),
          null,
          null,
        ),
      )
    val result = geminiModel.generateContent(prompt)
    assertEquals(expectedResponse, result)
  }
}
