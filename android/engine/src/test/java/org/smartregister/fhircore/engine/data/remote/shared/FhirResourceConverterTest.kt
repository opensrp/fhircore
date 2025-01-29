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
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverter
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverterFactory
import org.smartregister.fhircore.engine.util.extension.getCustomJsonParser
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
  fun testCustomJsonParserConvertsUserAssignmentsInPractitionerDetailsContainedFieldCorrectly() {
    val parser = FhirContext.forCached(FhirVersionEnum.R4).getCustomJsonParser()

    val practDetails =
      """
        {
          "resourceType": "Bundle",
          "total": 1,
          "entry": [
            {
              "resource": {
                "resourceType": "PractitionerDetail",
                "id": "8e53855c-779d-491d-9b73-5727517602a8",
                "meta": {
                  "profile": [
                    "http://hl7.org/fhir/profiles/custom-resource"
                  ]
                },
                "contained": [
                  {
                    "resourceType": "Location",
                    "id": "e347e698-f9d6-471c-9e73-44b80043e0ba",
                    "meta": {
                      "versionId": "2",
                      "lastUpdated": "2024-12-09T12:55:53.061+00:00",
                      "source": "#7304eb3f27706df5"
                    },
                    "status": "active",
                    "name": "Gurudola Test GND",
                    "type": [
                      {
                        "coding": [
                          {
                            "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                            "code": "jdn",
                            "display": "Jurisdiction"
                          }
                        ]
                      }
                    ],
                    "physicalType": {
                      "coding": [
                        {
                          "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                          "code": "jdn",
                          "display": "Jurisdiction"
                        }
                      ]
                    },
                    "partOf": {
                      "reference": "Location/099c05b2-f509-46c6-8072-f636ebf0c595",
                      "display": "Matugama Test Facility"
                    }
                  },
                  {
                    "resourceType": "Location",
                    "id": "4d7bcfa0-e6ed-44bd-a592-d9b7ba0f2cef",
                    "meta": {
                      "versionId": "1",
                      "lastUpdated": "2024-12-09T12:57:06.865+00:00",
                      "source": "#93280ef9aeb965c3"
                    },
                    "status": "active",
                    "name": "Henpita Test GND",
                    "type": [
                      {
                        "coding": [
                          {
                            "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                            "code": "jdn",
                            "display": "Jurisdiction"
                          }
                        ]
                      }
                    ],
                    "physicalType": {
                      "coding": [
                        {
                          "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                          "code": "jdn",
                          "display": "Jurisdiction"
                        }
                      ]
                    },
                    "partOf": {
                      "reference": "Location/866ab27d-461a-4c5d-b1d0-89ade3058758",
                      "display": "Katugahahena Test Facility"
                    }
                  },
                  {
                    "resourceType": "Location",
                    "id": "6c964cc1-b583-4739-9411-9838d4107d31",
                    "meta": {
                      "versionId": "1",
                      "lastUpdated": "2024-12-09T12:57:36.985+00:00",
                      "source": "#f64849744c57b2a0"
                    },
                    "status": "active",
                    "name": "Diyagala Test GND",
                    "type": [
                      {
                        "coding": [
                          {
                            "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                            "code": "jdn",
                            "display": "Jurisdiction"
                          }
                        ]
                      }
                    ],
                    "physicalType": {
                      "coding": [
                        {
                          "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                          "code": "jdn",
                          "display": "Jurisdiction"
                        }
                      ]
                    },
                    "partOf": {
                      "reference": "Location/866ab27d-461a-4c5d-b1d0-89ade3058758",
                      "display": "Katugahahena Test Facility"
                    }
                  },
                  {
                    "resourceType": "Organization",
                    "id": "51fc72f2-68ab-4feb-80d3-cd67bec87795",
                    "meta": {
                      "versionId": "1",
                      "lastUpdated": "2024-12-10T08:12:22.786+00:00",
                      "source": "#8a10e75e64c7d3e7"
                    },
                    "identifier": [
                      {
                        "use": "official",
                        "value": "43e03c23-04c8-4ea6-acae-691418dca6f2"
                      }
                    ],
                    "active": true,
                    "type": [
                      {
                        "coding": [
                          {
                            "system": "http://terminology.hl7.org/CodeSystem/organization-type",
                            "code": "team"
                          }
                        ]
                      }
                    ],
                    "name": "Gurudola Org"
                  },
                  {
                    "resourceType": "Organization",
                    "id": "592499d8-0837-4632-8a98-cf2c19316c86",
                    "meta": {
                      "versionId": "1",
                      "lastUpdated": "2024-12-10T08:13:06.313+00:00",
                      "source": "#6e9a830646ffd5b5"
                    },
                    "identifier": [
                      {
                        "use": "official",
                        "value": "92823980-8adb-4aa0-b9ee-972913bcd4d9"
                      }
                    ],
                    "active": true,
                    "type": [
                      {
                        "coding": [
                          {
                            "system": "http://terminology.hl7.org/CodeSystem/organization-type",
                            "code": "team"
                          }
                        ]
                      }
                    ],
                    "name": "Diyagala"
                  },
                  {
                    "resourceType": "Organization",
                    "id": "8992f2a8-6fed-4945-9f4b-98612d43d7ee",
                    "meta": {
                      "versionId": "1",
                      "lastUpdated": "2024-12-10T08:13:48.854+00:00",
                      "source": "#27259eb01172d208"
                    },
                    "identifier": [
                      {
                        "use": "official",
                        "value": "819fdfc2-9d24-48cf-ac21-537697274be5"
                      }
                    ],
                    "active": true,
                    "type": [
                      {
                        "coding": [
                          {
                            "system": "http://terminology.hl7.org/CodeSystem/organization-type",
                            "code": "team"
                          }
                        ]
                      }
                    ],
                    "name": "Henpita"
                  }
                ],
                "fhir": {
                  "id": "8e53855c-779d-491d-9b73-5727517602a8",
                  "careteams": [],
                  "teams": [],
                  "locations": [],
                  "locationHierarchyList": [],
                  "practitionerRoles": [],
                  "groups": [],
                  "practitioner": [],
                  "organizationAffiliation": []
                }
              }
            }
          ]
        }
            """
        .trimIndent()
    val input = practDetails.toByteArray().toResponseBody()
    Bundle()

    val result =
      FhirConverterFactory(parser).responseBodyConverter(type, annotations, retrofit).convert(input)
        as Resource
    val parsedPractitionerDetails = ((result as Bundle).entry[0].resource as PractitionerDetails)
    Assert.assertEquals(6, parsedPractitionerDetails.contained.size)
    Assert.assertEquals(
      "Gurudola Test GND",
      (parsedPractitionerDetails.contained[0] as Location).name
    )
    Assert.assertEquals(
      "Henpita Test GND",
      (parsedPractitionerDetails.contained[1] as Location).name
    )
    Assert.assertEquals(
      "Diyagala Test GND",
      (parsedPractitionerDetails.contained[2] as Location).name
    )
    Assert.assertEquals(
      "Gurudola Org",
      (parsedPractitionerDetails.contained[3] as Organization).name
    )
    Assert.assertEquals("Diyagala", (parsedPractitionerDetails.contained[4] as Organization).name)
    Assert.assertEquals("Henpita", (parsedPractitionerDetails.contained[5] as Organization).name)
  }

  private fun buildPatient(): Patient {
    return Patient().apply {
      id = "12345"
      nameFirstRep.family = "Doe"
      nameFirstRep.given.add(StringType("John"))
    }
  }
}
