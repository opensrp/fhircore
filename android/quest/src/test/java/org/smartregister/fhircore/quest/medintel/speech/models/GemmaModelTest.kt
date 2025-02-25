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

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GemmaModelTest {

  private lateinit var gemmaModel: GemmaModel
  private lateinit var mockLlmInference: ILlmInference

  @Before
  fun setUp() {
    mockLlmInference = mockk<ILlmInference>()
    gemmaModel = GemmaModel(mockLlmInference)
  }

  @Test
  fun generateContent_returnsGeneratedText() = runBlocking {
    val prompt = "Hello, world!"
    val expectedResponse = "Hello, user!"
    every { mockLlmInference.generateResponse(prompt) } returns expectedResponse
    val result = gemmaModel.generateContent(prompt)
    Assert.assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesNullResponse() = runBlocking {
    val prompt = "Hello, world!"
    every { mockLlmInference.generateResponse(prompt) } returns null
    val result = gemmaModel.generateContent(prompt)
    Assert.assertNull(result)
  }

  @Test
  fun generateContent_handlesEmptyPrompt() = runBlocking {
    val prompt = ""
    val expectedResponse = "Empty prompt response"
    every { mockLlmInference.generateResponse(prompt) } returns expectedResponse
    val result = gemmaModel.generateContent(prompt)
    Assert.assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesLongPrompt() = runBlocking {
    val prompt = "a".repeat(5000)
    val expectedResponse = "Long prompt response"
    every { mockLlmInference.generateResponse(prompt) } returns expectedResponse
    val result = gemmaModel.generateContent(prompt)
    Assert.assertEquals(expectedResponse, result)
  }

  @Test
  fun generateContent_handlesSpecialCharacters() = runBlocking {
    val prompt = "!@#$%^&*()"
    val expectedResponse = "Special characters response"
    every { mockLlmInference.generateResponse(prompt) } returns expectedResponse
    val result = gemmaModel.generateContent(prompt)
    Assert.assertEquals(expectedResponse, result)
  }
}
