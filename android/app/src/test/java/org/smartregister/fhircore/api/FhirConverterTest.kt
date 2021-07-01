/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.api

import ca.uhn.fhir.parser.IParser
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FhirConverterTest {

  private lateinit var fhirConverterFactory: FhirConverterFactory

  @MockK private lateinit var parser: IParser

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    fhirConverterFactory = FhirConverterFactory(parser)
  }

  @Test
  fun testVerifyParseResource() {
    val converter = fhirConverterFactory.responseBodyConverter(mockk(), arrayOf(), mockk())
    Assert.assertNotNull(converter)

    val responseBody = mockk<ResponseBody>()
    every { parser.parseResource(any<Class<out Resource>>(), any<String>()) } returns mockk()
    every { responseBody.string() } returns ""

    converter?.convert(responseBody)
    verify(exactly = 1) { parser.parseResource(any<Class<out Resource>>(), any<String>()) }
  }
}
