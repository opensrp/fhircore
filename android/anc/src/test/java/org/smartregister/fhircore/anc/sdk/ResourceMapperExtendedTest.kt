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

package org.smartregister.fhircore.anc.sdk

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.data.local.DefaultRepository

class ResourceMapperExtendedTest : RobolectricTest() {

  @Test
  fun testSaveParsedResourceShouldVerifySavedResourceData() = runBlocking {
    val patient = getPatient()
    val relatedPatient = getRelatedPatient()
    val questionnaire = getQuestionnaire()
    val questionnaireResponse = getQuestionnaireResponse()

    val fhirEngine = mockk<FhirEngine>()
    val defaultRepository = mockk<DefaultRepository>()
    coEvery { fhirEngine.load(Patient::class.java, "patient_id_1") } returns patient
    coEvery { fhirEngine.load(Patient::class.java, "related_patient_id_2") } returns relatedPatient
    coEvery { fhirEngine.save(any()) } returns Unit

    val resourceMapperExtended = ResourceMapperExtended(defaultRepository)
    resourceMapperExtended.saveParsedResource(
      questionnaireResponse,
      questionnaire,
      "patient_id_1",
      "related_patient_id_2"
    )

    coVerify(exactly = 4) { defaultRepository.addOrUpdate(any()) }

    assertEquals("patient_id_1", patient.logicalId)
    assertEquals(questionnaire.item.size, patient.meta.tag.size)
    assertEquals("Patient/${relatedPatient.logicalId}", patient.link.first().other.reference)
    assertEquals(Patient.LinkType.REFER, patient.link.first().type)
    assertEquals(relatedPatient.addressFirstRep.district, patient.addressFirstRep.district)
    assertEquals(relatedPatient.addressFirstRep.city, patient.addressFirstRep.city)
    assertEquals(2, patient.extension.size)
  }

  private fun getQuestionnaire(): Questionnaire {
    return Questionnaire().apply {
      addSubjectType("Patient")
      item =
        listOf(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "1"
            definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
            code = mutableListOf(Coding("system_1", "code_1", "display_1"))
            addExtension(
              "http://hl7.org/fhir/StructureDefinition/flag-detail",
              StringType("display_1")
            )
            addItem().apply {
              linkId = "1.1"
              definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
              code = mutableListOf(Coding("system_1_1", "code_1_1", "display_1_1"))
              addExtension(
                "http://hl7.org/fhir/StructureDefinition/flag-detail",
                StringType("display_1_1")
              )
              type = Questionnaire.QuestionnaireItemType.INTEGER
            }
          },
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "2"
            definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
          },
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "3"
            definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
          },
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "4"
            definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
            type = Questionnaire.QuestionnaireItemType.GROUP
            addExtension(QuestionnaireUtils.ITEM_CONTEXT_EXTENSION_URL, BooleanType(true))
          }
        )
    }
  }

  private fun getQuestionnaireResponse(): QuestionnaireResponse {
    return QuestionnaireResponse().apply {
      item =
        listOf(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "1"
            addAnswer().apply { value = BooleanType(true) }
            addItem().apply {
              linkId = "1.1"
              addAnswer().apply { value = BooleanType(true) }
            }
          },
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "2"
            addAnswer().apply { value = Coding("system_2", "code_2", "display_2") }
          },
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "3"
            addAnswer().apply { value = StringType("") }
          },
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "4"
            addAnswer().apply { value = Coding("system_4", "code_4", "display_4") }
          }
        )
    }
  }

  private fun getPatient(): Patient {
    return Patient()
  }

  private fun getRelatedPatient(): Patient {
    return Patient().apply {
      id = "related_patient_id_2"
      addAddress().apply {
        district = "Central"
        city = "Nairobi"
      }
    }
  }
}
