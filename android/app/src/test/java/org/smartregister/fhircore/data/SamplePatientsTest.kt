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

package org.smartregister.fhircore.data

import java.text.SimpleDateFormat
import java.util.Date
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.viewmodel.PatientListViewModel

class SamplePatientsTest : RobolectricTest() {

  private lateinit var samplePatients: SamplePatients

  @Before
  fun setUp() {
    samplePatients = SamplePatients()
  }

  @Test
  fun testGetPatientItemsShouldReturnNormalizePatientItemList() {
    val patients = getPatients()
    val items = samplePatients.getPatientItems(patients)

    verifyPatient(patients[0], items[0])
    verifyPatient(patients[1], items[1])
  }

  @Test
  fun testGetObservationItemsShouldReturnNormalizeObservationItemList() {

    val observation = JSONObject(getObservationJson())
    val observations = samplePatients.getObservationItems(observation.toString())

    Assert.assertEquals(1, observations.size)
    Assert.assertEquals("1", observations[0].id)
    Assert.assertEquals("Carbon Dioxide", observations[0].code)
    Assert.assertEquals("2002-07-28T15:08:13-05:00", observations[0].effective)
    Assert.assertEquals("28.85 mmol/L", observations[0].value)

    val resourceObject =
      observation.getJSONArray("entry").getJSONObject(0).getJSONObject("resource")
    resourceObject.remove("effectiveDateTime")
    resourceObject.remove("valueQuantity")

    val updatedObservations = samplePatients.getObservationItems(observation.toString())

    Assert.assertEquals(1, updatedObservations.size)
    Assert.assertEquals("1", updatedObservations[0].id)
    Assert.assertEquals("Carbon Dioxide", updatedObservations[0].code)
    Assert.assertEquals("No effective DateTime", updatedObservations[0].effective)
    Assert.assertEquals("No ValueQuantity ", updatedObservations[0].value)
  }

  @Test
  fun testGetPatientsFromStringReturnsListOfSerializedPatients() {
    val patients = samplePatients.getPatientItems(samplePatientBundle)
    Assert.assertEquals(2, patients.size)
    Assert.assertEquals("female", patients[0].gender)
    Assert.assertEquals("female", patients[0].gender)
    Assert.assertEquals("4545454", patients[0].phone)
  }

  private fun verifyPatient(patient: Patient, item: PatientListViewModel.PatientItem) {
    Assert.assertEquals(patient.id, item.logicalId)
    Assert.assertEquals(patient.name[0].nameAsSingleString, item.name)
    Assert.assertEquals(patient.genderElement.valueAsString, item.gender)
    Assert.assertEquals(patient.birthDateElement.valueAsString, item.dob)
    Assert.assertEquals(patient.telecom[0].value, item.phone)
    Assert.assertEquals(patient.text.div.valueAsString, item.html)
  }

  private fun getPatients(): List<Patient> {
    return listOf(

      // patient 1
      Patient().apply {
        id = "patient_1"
        gender = Enumerations.AdministrativeGender.MALE
        name = getPatientName("mc", "jane")
        telecom = getTelecom("123")
        birthDate = getBirthDate("2000-02-03")
        text.divAsString = "a"
      },

      // patient 2
      Patient().apply {
        id = "patient_2"
        gender = Enumerations.AdministrativeGender.FEMALE
        name = getPatientName("julie", "sam")
        telecom = getTelecom("456")
        birthDate = getBirthDate("2003-04-13")
        text.divAsString = "b"
      }
    )
  }

  private fun getPatientName(given: String, family: String): MutableList<HumanName> {
    return mutableListOf(
      HumanName().apply {
        addGiven(given)
        setFamily(family)
      }
    )
  }

  private fun getTelecom(telecom: String): MutableList<ContactPoint> {
    return mutableListOf(ContactPoint().apply { value = telecom })
  }

  private fun getBirthDate(birthDate: String): Date? {
    return SimpleDateFormat("yyyy-MM-dd").parse(birthDate)
  }

  private fun getObservationJson(): String {
    return """
      {
        "resourceType": "Bundle",
        "id": "a1bb277a-c3d8-4017-92d0-288d2582a9cf",
        "meta": {
          "lastUpdated": "2020-06-28T23:06:53.520+00:00"
        },
        "type": "searchset",
        "link": [
          {
            "relation": "self",
            "url": "http://hapi.fhir.org/baseR4/Observation?_pretty=true"
          },
          {
            "relation": "next",
            "url": "http://hapi.fhir.org/baseR4?_getpages=a1bb277a-c3d8-4017-92d0-288d2582a9cf&_getpagesoffset=20&_count=20&_pretty=true&_bundletype=searchset"
          }
        ],
        "entry": [
          {
            "fullUrl": "http://hapi.fhir.org/baseR4/Observation/904f0963-7a78-4de5-8898-e2611d1bf2fb",
            "resource": {
              "resourceType": "Observation",
              "id": "904f0963-7a78-4de5-8898-e2611d1bf2fb",
              "meta": {
                "versionId": "1",
                "lastUpdated": "2020-03-24T23:05:16.702+00:00",
                "source": "#JWjpajHMJZboUAOz",
                "profile": [
                  "http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab"
                ],
                "tag": [
                  {
                    "system": "https://smarthealthit.org/tags",
                    "code": "Covid19 synthetic population from Synthea"
                  }
                ]
              },
              "status": "final",
              "category": [
                {
                  "coding": [
                    {
                      "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                      "code": "laboratory",
                      "display": "laboratory"
                    }
                  ]
                }
              ],
              "code": {
                "coding": [
                  {
                    "system": "http://loinc.org",
                    "code": "20565-8",
                    "display": "Carbon Dioxide"
                  }
                ],
                "text": "Carbon Dioxide"
              },
              "subject": {
                "reference": "Patient/175741f0-a23c-476d-901c-d84830331ae3"
              },
              "encounter": {
                "reference": "Encounter/9a623654-c35a-4612-80ee-5316bb6a0a4c"
              },
              "effectiveDateTime": "2002-07-28T15:08:13-05:00",
              "issued": "2002-07-28T15:08:13.434-05:00",
              "valueQuantity": {
                "value": 28.85,
                "unit": "mmol/L",
                "system": "http://unitsofmeasure.org",
                "code": "mmol/L"
              }
            },
            "search": {
              "mode": "match"
            }
          }
        ]
      }
    """.trimIndent()
  }

  val samplePatientBundle =
    """
    {
      "resourceType": "Bundle",
      "id": "4cf6362d-99b4-43cb-9b58-88b087656dce",
      "meta": {
        "lastUpdated": "2021-07-07T09:13:23.295+00:00"
      },
      "type": "searchset",
      "total": 53,
      "link": [ {
        "relation": "self",
        "url": "http://fhir.labs.smartregister.org/fhir/Patient?_count=2&_format=json&_pretty=true"
      }, {
        "relation": "next",
        "url": "http://fhir.labs.smartregister.org/fhir?_getpages=4cf6362d-99b4-43cb-9b58-88b087656dce&_getpagesoffset=2&_count=2&_format=json&_pretty=true&_bundletype=searchset"
      } ],
      "entry": [ {
        "fullUrl": "http://fhir.labs.smartregister.org/fhir/Patient/Lovelace",
        "resource": {
          "resourceType": "Patient",
          "id": "Lovelace",
          "meta": {
            "versionId": "6",
            "lastUpdated": "2021-06-23T20:05:33.939+00:00",
            "source": "#ce76474d06ebd145"
          },
          "text": {
            "status": "generated",
            "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><div class=\"hapiHeaderText\">Ada <b>LOVELACE </b></div><table class=\"hapiPropertyTable\"><tbody><tr><td>Address</td><td><span>Nairobi </span><span>Kenya </span></td></tr><tr><td>Date of birth</td><td><span>15 April 2021</span></td></tr></tbody></table></div>"
          },
          "active": true,
          "name": [ {
            "family": "Lovelace",
            "given": [ "Ada" ]
          } ],
          "telecom": [ {
            "system": "phone",
            "value": "4545454"
          } ],
          "gender": "female",
          "birthDate": "2021-04-15",
          "address": [ {
            "city": "Nairobi",
            "country": "Kenya"
          } ]
        },
        "search": {
          "mode": "match"
        }
      }, {
        "fullUrl": "http://fhir.labs.smartregister.org/fhir/Patient/1",
        "resource": {
          "resourceType": "Patient",
          "id": "1",
          "meta": {
            "versionId": "4",
            "lastUpdated": "2021-03-10T13:27:48.632+00:00",
            "source": "#14dfbe238f0933a5"
          },
          "text": {
            "status": "generated",
            "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><div class=\"hapiHeaderText\">John <b>DOE </b></div><table class=\"hapiPropertyTable\"><tbody><tr><td>Address</td><td><span>213,One Pademore </span><br/><span>Nairobi </span><span>Kenya </span></td></tr><tr><td>Date of birth</td><td><span>04 August 1988</span></td></tr></tbody></table></div>"
          },
          "name": [ {
            "use": "official",
            "family": "Doe",
            "given": [ "John" ]
          } ],
          "telecom": [ {
            "system": "phone",
            "value": "+254722123456",
            "use": "mobile"
          }, {
            "system": "email",
            "value": "jdoe@ona.io"
          } ],
          "gender": "male",
          "birthDate": "1988-08-04",
          "address": [ {
            "line": [ "213,One Pademore" ],
            "city": "Nairobi",
            "postalCode": "00100",
            "country": "Kenya"
          } ]
        },
        "search": {
          "mode": "match"
        }
      } ]
    }
    """.trimIndent()
}
