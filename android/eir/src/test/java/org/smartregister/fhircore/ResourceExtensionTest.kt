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

package org.smartregister.fhircore

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.StringType
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.deleteRelatedResources
import org.smartregister.fhircore.engine.util.extension.retainMetadata
import org.smartregister.fhircore.engine.util.extension.updateFrom

class ResourceExtensionTest : RobolectricTest() {

  @Test
  fun `JSONObject#updateFrom() should ignore nulls and update fields`() {
    val originalObject = JSONObject()
    originalObject.put("property1", "ValueProperty1")
    originalObject.put("property2", "ValueProperty2")
    originalObject.put("prop3", "Medical")
    originalObject.put("prop4", "VP4")
    originalObject.putOpt("prop5", null)

    val updatedObject = JSONObject()
    updatedObject.put("prop3", "ValueProp3")
    updatedObject.put("prop4", null)
    updatedObject.put("prop5", "ValueProp5")

    originalObject.updateFrom(updatedObject)

    Assert.assertEquals("ValueProp3", originalObject.get("prop3"))
    Assert.assertEquals("VP4", originalObject.get("prop4"))
    Assert.assertEquals("ValueProp5", originalObject.get("prop5"))
  }

  @Test
  fun `Resource#updateFrom() should ignore nulls`() {
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        name.apply {
          add(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            }
          )
        }
      }

    val updatedPatient =
      Patient().apply {
        deceased = BooleanType(true)
        birthDate = Date()
        gender = null
        name.apply {
          add(
            HumanName().apply {
              family = "Kamau"
              given = listOf(StringType("Andrew"))
            }
          )
        }
      }

    patient = patient.updateFrom(updatedPatient)

    Assert.assertNotNull(patient.birthDate)
    Assert.assertEquals(
      BooleanType(true).booleanValue(),
      (patient.deceased as BooleanType).booleanValue()
    )
    Assert.assertEquals("Kamau", patient.name[0].family)
    Assert.assertEquals("Andrew", patient.name[0].given[0].value)
  }

  @Test
  fun `QuestionnaireResponse#deleteRelatedResources() should call defaultRepository#deleteResource for resources in contained`() {
    val patient = Patient()
    val relatedPerson = RelatedPerson()
    val observation = Observation()
    val questionnaireResponse =
      QuestionnaireResponse().apply { contained = listOf(patient, relatedPerson, observation) }

    val defaultRepository = mockk<DefaultRepository>()

    coEvery { defaultRepository.delete(any()) } returns Unit

    runBlocking { questionnaireResponse.deleteRelatedResources(defaultRepository) }

    coVerify { defaultRepository.delete(patient) }
    coVerify { defaultRepository.delete(relatedPerson) }
    coVerify { defaultRepository.delete(observation) }
  }

  @Test
  fun `QuestionnaireResponse#retainMetadata() should call retain details from previous QR`() {
    val id = "qrId"
    val authoredDate = Date()
    val versionId = "5"
    val author = Reference()
    val oldQr =
      QuestionnaireResponse().apply {
        setId(id)
        authored = authoredDate
        setAuthor(author)
        meta.apply { setVersionId(versionId) }
      }
    val questionnaireResponse = QuestionnaireResponse()

    questionnaireResponse.retainMetadata(oldQr)

    Assert.assertEquals(id, oldQr.id)
    Assert.assertEquals(author, oldQr.author)
    Assert.assertEquals(authoredDate, oldQr.authored)
    Assert.assertEquals("6", oldQr.meta.versionId)
    Assert.assertNotNull(oldQr.meta.lastUpdated)
  }
}
