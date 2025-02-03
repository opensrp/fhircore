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

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class GemmaModelTest {

  private lateinit var gemmaModel: GemmaModel
  private lateinit var mockContext: Context
  private lateinit var mockLlmInference: LlmInference

  @Before
  fun setUp() {
    mockContext = mock(Context::class.java)
    mockLlmInference = mock(LlmInference::class.java)
    gemmaModel = GemmaModel(mockContext, "model/path").apply { model = mockLlmInference }
  }

  @Test
  fun generateContent_returnsGeneratedText() = runBlocking {
    val prompt = "Hello, world!"
    val expectedResponse = "Hello, user!"
    `when`(mockLlmInference.generateResponse(prompt)).thenReturn(expectedResponse)
    val result = gemmaModel.generateContent(prompt)
    assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesNullResponse() = runBlocking {
    val prompt = "Hello, world!"
    `when`(mockLlmInference.generateResponse(prompt)).thenReturn(null)
    val result = gemmaModel.generateContent(prompt)
    assertNull(result)
  }

  @Test
  fun generateContent_handlesEmptyPrompt() = runBlocking {
    val prompt = ""
    val expectedResponse = "Empty prompt response"
    `when`(mockLlmInference.generateResponse(prompt)).thenReturn(expectedResponse)
    val result = gemmaModel.generateContent(prompt)
    assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesLongPrompt() = runBlocking {
    val prompt = "a".repeat(5000)
    val expectedResponse = "Long prompt response"
    `when`(mockLlmInference.generateResponse(prompt)).thenReturn(expectedResponse)
    val result = gemmaModel.generateContent(prompt)
    assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesSpecialCharacters() = runBlocking {
    val prompt = "!@#$%^&*()"
    val expectedResponse = "Special characters response"
    `when`(mockLlmInference.generateResponse(prompt)).thenReturn(expectedResponse)
    val result = gemmaModel.generateContent(prompt)
    assertEquals(expectedResponse, result)
  }
}
