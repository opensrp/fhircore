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

package org.smartregister.fhircore.quest.app.fakes

import io.mockk.coEvery
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.ResultItem

object Faker {

  fun buildPatient(
    id: String = "sampleId",
    family: String = "Mandela",
    given: String = "Nelson",
    age: Int = 78,
    gender: Enumerations.AdministrativeGender = Enumerations.AdministrativeGender.MALE
  ): Patient {
    return Patient().apply {
      this.id = id
      this.identifierFirstRep.value = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.gender = gender
      this.birthDate = DateType(Date()).apply { add(Calendar.YEAR, -age) }.dateTimeValue().value

      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }
    }
  }

  fun initPatientRepositoryMocks(patientRepository: PatientRepository) {
    coEvery { patientRepository.fetchDemographics(any()) } returns
      Patient().apply {
        name =
          listOf(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            }
          )
        id = "5583145"
        gender = Enumerations.AdministrativeGender.MALE
        birthDate = SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01")
        address =
          listOf(
            Address().apply {
              city = "Nairobi"
              country = "Kenya"
            }
          )
        identifier = listOf(Identifier().apply { value = "12345" })
      }

    coEvery { patientRepository.fetchTestForms(any(), any()) } returns
      listOf(
        QuestionnaireConfig(
          appId = "quest",
          form = "sample-order-result",
          title = "Sample Order Result",
          identifier = "12345"
        ),
        QuestionnaireConfig(
          appId = "quest",
          form = "sample-test-result",
          title = "Sample Test Result",
          identifier = "67890"
        )
      )

    coEvery { patientRepository.fetchTestResults(any(), any(), any(), any()) } returns
      listOf(
        ResultItem(Pair(
          QuestionnaireResponse().apply {
            meta = Meta().apply { tag = listOf(Coding().apply { display = "Sample Order" }) }
            authored = Date()
          },
          Questionnaire().apply {
            name = "Sample Order"
            title = "Sample Order"
          }
        ), listOf()),
        ResultItem(Pair(
          QuestionnaireResponse().apply {
            meta = Meta().apply { tag = listOf(Coding().apply { display = "Sample Test" }) }
            authored = Date()
          },
          Questionnaire().apply {
            name = "Sample Test"
            title = "Sample Test"
          }
        ), listOf())
      )
  }

  fun initPatientRepositoryEmptyMocks(patientRepository: PatientRepository) {

    coEvery { patientRepository.fetchDemographics(any()) } returns Patient()
    coEvery { patientRepository.fetchTestForms(any(), any()) } returns emptyList()
    coEvery { patientRepository.fetchTestResults(any(), any(), any(), any()) } returns emptyList()
  }
}
