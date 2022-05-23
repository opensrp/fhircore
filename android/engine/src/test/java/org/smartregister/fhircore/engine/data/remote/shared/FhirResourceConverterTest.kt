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

package org.smartregister.fhircore.engine.data.remote.shared

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.logicalId
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import java.lang.reflect.Type
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverter
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverterFactory
import retrofit2.Retrofit

class FhirResourceConverterTest {
  @MockK lateinit var type: Type

  @MockK lateinit var retrofit: Retrofit

  lateinit var annotations: Array<Annotation>
  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true, relaxUnitFun = true)

    annotations = arrayOf()
  }

  @Test
  fun testFhirConverterShouldConvertResourceCorrectly() {
    val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    val input = parser.encodeResourceToString(buildPatient()).toByteArray().toResponseBody()

    val result = FhirConverter(parser).convert(input) as Patient
    Assert.assertEquals("John", result.nameFirstRep.given[0].value)
    Assert.assertEquals("Doe", result.nameFirstRep.family)
    Assert.assertEquals("12345", result.logicalId)
  }

  @Test
  fun testFhirConverterFactoryShouldConvertResourceCorrectly() {
    val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    val input = parser.encodeResourceToString(buildPatient()).toByteArray().toResponseBody()

    val result =
      FhirConverterFactory(parser)
        .responseBodyConverter(type, annotations, retrofit)
        .convert(input) as
        Patient
    Assert.assertEquals("John", result.nameFirstRep.given[0].value)
    Assert.assertEquals("Doe", result.nameFirstRep.family)
    Assert.assertEquals("12345", result.logicalId)
  }

  private fun buildPatient(): Patient {
    return Patient().apply {
      id = "12345"
      nameFirstRep.family = "Doe"
      nameFirstRep.given.add(StringType("John"))
    }
  }
}
