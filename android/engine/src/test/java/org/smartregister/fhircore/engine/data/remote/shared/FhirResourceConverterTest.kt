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

package org.smartregister.fhircore.engine.data.remote.shared

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.extensions.logicalId
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import java.lang.reflect.Type
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverter
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverterFactory
import org.smartregister.fhircore.engine.data.remote.fhir.resource.parser.CustomFhirContext
import org.smartregister.fhircore.engine.util.extension.getCustomJsonParser
import org.smartregister.model.practitioner.FhirPractitionerDetails
import org.smartregister.model.practitioner.PractitionerDetails
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
      FhirConverterFactory(parser).responseBodyConverter(type, annotations, retrofit).convert(input)
        as Patient
    Assert.assertEquals("John", result.nameFirstRep.given[0].value)
    Assert.assertEquals("Doe", result.nameFirstRep.family)
    Assert.assertEquals("12345", result.logicalId)
  }

  @Test
  fun testCustomFhirConverterFactoryShouldConvertPractitionerDetailsCorrectly() {
    val parser = CustomFhirContext().getCustomJsonParser()

    val practDetails =
      "{\n" +
        "  \"resourceType\": \"Bundle\",\n" +
        "  \"total\": 1,\n" +
        "  \"entry\": [\n" +
        "    {\n" +
        "      \"resource\": {\n" +
        "        \"resourceType\": \"PractitionerDetail\",\n" +
        "        \"id\": \"8e53855c-779d-491d-9b73-5727517602a8\",\n" +
        "        \"meta\": {\n" +
        "          \"profile\": [\n" +
        "            \"http://hl7.org/fhir/profiles/custom-resource\"\n" +
        "          ]\n" +
        "        },\n" +
        "        \"fhir\": {\n" +
        "          \"id\": \"8e53855c-779d-491d-9b73-5727517602a8\",\n" +
        "                    \"teams\": [\n" +
        "            {\n" +
        "              \"resourceType\": \"Organization\",\n" +
        "              \"id\": \"51fc72f2-68ab-4feb-80d3-cd67bec87795\",\n" +
        "              \"meta\": {\n" +
        "                \"versionId\": \"1\",\n" +
        "                \"lastUpdated\": \"2024-12-10T08:12:22.786+00:00\",\n" +
        "                \"source\": \"#8a10e75e64c7d3e7\"\n" +
        "              },\n" +
        "              \"identifier\": [\n" +
        "                {\n" +
        "                  \"use\": \"official\",\n" +
        "                  \"value\": \"43e03c23-04c8-4ea6-acae-691418dca6f2\"\n" +
        "                }\n" +
        "              ],\n" +
        "              \"active\": true,\n" +
        "              \"type\": [\n" +
        "                {\n" +
        "                  \"coding\": [\n" +
        "                    {\n" +
        "                      \"system\": \"http://terminology.hl7.org/CodeSystem/organization-type\",\n" +
        "                      \"code\": \"team\"\n" +
        "                    }\n" +
        "                  ]\n" +
        "                }\n" +
        "              ],\n" +
        "              \"name\": \"Gurudola Org\"\n" +
        "            },\n" +
        "            {\n" +
        "              \"resourceType\": \"Organization\",\n" +
        "              \"id\": \"592499d8-0837-4632-8a98-cf2c19316c86\",\n" +
        "              \"meta\": {\n" +
        "                \"versionId\": \"1\",\n" +
        "                \"lastUpdated\": \"2024-12-10T08:13:06.313+00:00\",\n" +
        "                \"source\": \"#6e9a830646ffd5b5\"\n" +
        "              },\n" +
        "              \"identifier\": [\n" +
        "                {\n" +
        "                  \"use\": \"official\",\n" +
        "                  \"value\": \"92823980-8adb-4aa0-b9ee-972913bcd4d9\"\n" +
        "                }\n" +
        "              ],\n" +
        "              \"active\": true,\n" +
        "              \"type\": [\n" +
        "                {\n" +
        "                  \"coding\": [\n" +
        "                    {\n" +
        "                      \"system\": \"http://terminology.hl7.org/CodeSystem/organization-type\",\n" +
        "                      \"code\": \"team\"\n" +
        "                    }\n" +
        "                  ]\n" +
        "                }\n" +
        "              ],\n" +
        "              \"name\": \"Diyagala\"\n" +
        "            },\n" +
        "            {\n" +
        "              \"resourceType\": \"Organization\",\n" +
        "              \"id\": \"8992f2a8-6fed-4945-9f4b-98612d43d7ee\",\n" +
        "              \"meta\": {\n" +
        "                \"versionId\": \"1\",\n" +
        "                \"lastUpdated\": \"2024-12-10T08:13:48.854+00:00\",\n" +
        "                \"source\": \"#27259eb01172d208\"\n" +
        "              },\n" +
        "              \"identifier\": [\n" +
        "                {\n" +
        "                  \"use\": \"official\",\n" +
        "                  \"value\": \"819fdfc2-9d24-48cf-ac21-537697274be5\"\n" +
        "                }\n" +
        "              ],\n" +
        "              \"active\": true,\n" +
        "              \"type\": [\n" +
        "                {\n" +
        "                  \"coding\": [\n" +
        "                    {\n" +
        "                      \"system\": \"http://terminology.hl7.org/CodeSystem/organization-type\",\n" +
        "                      \"code\": \"team\"\n" +
        "                    }\n" +
        "                  ]\n" +
        "                }\n" +
        "              ],\n" +
        "              \"name\": \"Henpita\"\n" +
        "            }\n" +
        "          ],\n" +
        "          \"locations\": [\n" +
        "            {\n" +
        "              \"resourceType\": \"Location\",\n" +
        "              \"id\": \"e347e698-f9d6-471c-9e73-44b80043e0ba\",\n" +
        "              \"meta\": {\n" +
        "                \"versionId\": \"2\",\n" +
        "                \"lastUpdated\": \"2024-12-09T12:55:53.061+00:00\",\n" +
        "                \"source\": \"#7304eb3f27706df5\"\n" +
        "              },\n" +
        "              \"status\": \"active\",\n" +
        "              \"name\": \"Gurudola Test GND\",\n" +
        "              \"type\": [\n" +
        "                {\n" +
        "                  \"coding\": [\n" +
        "                    {\n" +
        "                      \"system\": \"http://terminology.hl7.org/CodeSystem/location-physical-type\",\n" +
        "                      \"code\": \"jdn\",\n" +
        "                      \"display\": \"Jurisdiction\"\n" +
        "                    }\n" +
        "                  ]\n" +
        "                }\n" +
        "              ],\n" +
        "              \"physicalType\": {\n" +
        "                \"coding\": [\n" +
        "                  {\n" +
        "                    \"system\": \"http://terminology.hl7.org/CodeSystem/location-physical-type\",\n" +
        "                    \"code\": \"jdn\",\n" +
        "                    \"display\": \"Jurisdiction\"\n" +
        "                  }\n" +
        "                ]\n" +
        "              },\n" +
        "              \"partOf\": {\n" +
        "                \"reference\": \"Location/099c05b2-f509-46c6-8072-f636ebf0c595\",\n" +
        "                \"display\": \"Matugama Test Facility\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"resourceType\": \"Location\",\n" +
        "              \"id\": \"4d7bcfa0-e6ed-44bd-a592-d9b7ba0f2cef\",\n" +
        "              \"meta\": {\n" +
        "                \"versionId\": \"1\",\n" +
        "                \"lastUpdated\": \"2024-12-09T12:57:06.865+00:00\",\n" +
        "                \"source\": \"#93280ef9aeb965c3\"\n" +
        "              },\n" +
        "              \"status\": \"active\",\n" +
        "              \"name\": \"Henpita Test GND\",\n" +
        "              \"type\": [\n" +
        "                {\n" +
        "                  \"coding\": [\n" +
        "                    {\n" +
        "                      \"system\": \"http://terminology.hl7.org/CodeSystem/location-physical-type\",\n" +
        "                      \"code\": \"jdn\",\n" +
        "                      \"display\": \"Jurisdiction\"\n" +
        "                    }\n" +
        "                  ]\n" +
        "                }\n" +
        "              ],\n" +
        "              \"physicalType\": {\n" +
        "                \"coding\": [\n" +
        "                  {\n" +
        "                    \"system\": \"http://terminology.hl7.org/CodeSystem/location-physical-type\",\n" +
        "                    \"code\": \"jdn\",\n" +
        "                    \"display\": \"Jurisdiction\"\n" +
        "                  }\n" +
        "                ]\n" +
        "              },\n" +
        "              \"partOf\": {\n" +
        "                \"reference\": \"Location/866ab27d-461a-4c5d-b1d0-89ade3058758\",\n" +
        "                \"display\": \"Katugahahena Test Facility\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"resourceType\": \"Location\",\n" +
        "              \"id\": \"6c964cc1-b583-4739-9411-9838d4107d31\",\n" +
        "              \"meta\": {\n" +
        "                \"versionId\": \"1\",\n" +
        "                \"lastUpdated\": \"2024-12-09T12:57:36.985+00:00\",\n" +
        "                \"source\": \"#f64849744c57b2a0\"\n" +
        "              },\n" +
        "              \"status\": \"active\",\n" +
        "              \"name\": \"Diyagala Test GND\",\n" +
        "              \"type\": [\n" +
        "                {\n" +
        "                  \"coding\": [\n" +
        "                    {\n" +
        "                      \"system\": \"http://terminology.hl7.org/CodeSystem/location-physical-type\",\n" +
        "                      \"code\": \"jdn\",\n" +
        "                      \"display\": \"Jurisdiction\"\n" +
        "                    }\n" +
        "                  ]\n" +
        "                }\n" +
        "              ],\n" +
        "              \"physicalType\": {\n" +
        "                \"coding\": [\n" +
        "                  {\n" +
        "                    \"system\": \"http://terminology.hl7.org/CodeSystem/location-physical-type\",\n" +
        "                    \"code\": \"jdn\",\n" +
        "                    \"display\": \"Jurisdiction\"\n" +
        "                  }\n" +
        "                ]\n" +
        "              },\n" +
        "              \"partOf\": {\n" +
        "                \"reference\": \"Location/866ab27d-461a-4c5d-b1d0-89ade3058758\",\n" +
        "                \"display\": \"Katugahahena Test Facility\"\n" +
        "              }\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}"
    val input = practDetails.toByteArray().toResponseBody()
    Bundle()
    Location()

    val result =
      FhirConverterFactory(parser).responseBodyConverter(type, annotations, retrofit).convert(input)
        as Resource
    val parsedPractitionerDetails = ((result as Bundle).entry[0].resource as PractitionerDetails)
    val parsedFhirPractitionerDetails =
      parsedPractitionerDetails.fhirPractitionerDetails as FhirPractitionerDetails
    Assert.assertEquals(3, parsedFhirPractitionerDetails.locations.size)
    Assert.assertEquals("Gurudola Test GND", parsedFhirPractitionerDetails.locations[0].name)
    Assert.assertEquals("Henpita Test GND", parsedFhirPractitionerDetails.locations[1].name)
    Assert.assertEquals("Diyagala Test GND", parsedFhirPractitionerDetails.locations[2].name)
    Assert.assertEquals(3, parsedFhirPractitionerDetails.organizations.size)
    Assert.assertEquals("Gurudola Org", parsedFhirPractitionerDetails.organizations[0].name)
    Assert.assertEquals("Diyagala", parsedFhirPractitionerDetails.organizations[1].name)
    Assert.assertEquals("Henpita", parsedFhirPractitionerDetails.organizations[2].name)
  }

  private fun buildPatient(): Patient {
    return Patient().apply {
      id = "12345"
      nameFirstRep.family = "Doe"
      nameFirstRep.given.add(StringType("John"))
    }
  }
}
