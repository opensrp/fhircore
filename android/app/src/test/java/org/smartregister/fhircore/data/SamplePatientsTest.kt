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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.viewmodel.PatientListViewModel

class SamplePatientsTest {

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
    val observations = samplePatients.getObservationItems(getObservationJson())

    Assert.assertEquals(1, observations.size)
    Assert.assertEquals("1", observations[0].id)
    Assert.assertEquals("Carbon Dioxide", observations[0].code)
    Assert.assertEquals("2002-07-28T15:08:13-05:00", observations[0].effective)
    Assert.assertEquals("28.85 mmol/L", observations[0].value)
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
}
