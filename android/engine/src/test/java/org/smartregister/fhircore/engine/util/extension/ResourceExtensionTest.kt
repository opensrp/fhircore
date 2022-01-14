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

package org.smartregister.fhircore.engine.util.extension

import com.google.android.fhir.logicalId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.UriType
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.questionnaire.FhirCoreQuestionnaireFragment

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
    updatedObject.put("prop4", null as Any?)
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
  fun `Resource#updateFrom() should preserve previous patient's meta tags`() {
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        meta.tag.apply {
          val codingList = arrayListOf<Coding>()
          codingList.add(Coding("abc", "xyz", "hello"))
          codingList.add(Coding("abc2", "xyz2", "hello2"))
          addAll(codingList)
        }
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
    Assert.assertEquals("hello", patient.meta.tag[0].display)
    Assert.assertEquals("hello2", patient.meta.tag[1].display)
  }

  @Test
  fun `Resource#updateFrom() should preserve previous patient's extensions`() {

    val extensionVal = Extension()
    val extensionVal2 = Extension()
    extensionVal.apply { this.urlElement = UriType("hello") }
    extensionVal2.apply { this.urlElement = UriType("hello2") }

    val extensionList = arrayListOf<Extension>()

    extensionList.add(extensionVal)
    extensionList.add(extensionVal2)

    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        extension.apply { addAll(extensionList) }
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
    Assert.assertEquals(2, patient.extension.size)
  }

  @Test
  fun `Resource#updateFrom() should preserve updated patient's extensions`() {

    val extensionVal = Extension()
    val extensionVal2 = Extension()
    extensionVal.apply { this.urlElement = UriType("hello") }
    extensionVal2.apply { this.urlElement = UriType("hello2") }

    val extensionList = arrayListOf<Extension>()

    extensionList.add(extensionVal)
    extensionList.add(extensionVal2)

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

        extension.apply { addAll(extensionList) }
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
    Assert.assertEquals(2, patient.extension.size)
  }

  @Test
  fun `Resource#updateFrom() should preserve both resource's extensions`() {

    val extensionVal = Extension()
    val extensionVal2 = Extension()
    extensionVal.apply { this.urlElement = UriType("hello") }
    extensionVal2.apply { this.urlElement = UriType("hello2") }

    val extensionList1 = arrayListOf<Extension>()
    val extensionList2 = arrayListOf<Extension>()

    extensionList1.add(extensionVal)
    extensionList2.add(extensionVal2)
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        extension.apply { addAll(extensionList1) }
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
        extension.apply { addAll(extensionList2) }
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
    Assert.assertEquals(2, patient.extension.size)
  }

  @Test
  fun `Resource#updateFrom() should preserve updated resource's meta tags`() {
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
        gender = Enumerations.AdministrativeGender.FEMALE
        meta.tag.apply {
          val codingList = arrayListOf<Coding>()
          codingList.add(Coding("abc", "xyz", "hello"))
          codingList.add(Coding("abc2", "xyz2", "hello2"))
          addAll(codingList)
        }
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
    Assert.assertEquals("hello", patient.meta.tag[0].display)
    Assert.assertEquals("hello2", patient.meta.tag[1].display)
  }

  @Test
  fun `Resource#updateFrom() should preserve meta tags of both resources`() {
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        meta.tag.apply {
          val codingList = arrayListOf<Coding>()
          codingList.add(Coding("abc3", "xyz3", "hello3"))
          addAll(codingList)
        }
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
        gender = Enumerations.AdministrativeGender.FEMALE
        meta.tag.apply {
          val codingList = arrayListOf<Coding>()
          codingList.add(Coding("abc", "xyz", "hello"))
          codingList.add(Coding("abc2", "xyz2", "hello2"))
          addAll(codingList)
        }
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
    Assert.assertEquals(3, patient.meta.tag.size)
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

  @Test
  fun `Resource#generateMissingId() should generate Id if empty`() {
    val resource = Patient()

    Assert.assertTrue(resource.logicalId.isEmpty())
    resource.generateMissingId()

    Assert.assertFalse(resource.logicalId.isEmpty())
  }

  @Test
  fun `Type#valueToString() should return string representation`() {
    Assert.assertEquals("12345", StringType("12345").valueToString())
    Assert.assertEquals("true", BooleanType(true).valueToString())
    Assert.assertEquals(Date().makeItReadable(), DateTimeType(Date()).valueToString())
    Assert.assertEquals("d", Coding("s", "c", "d").valueToString())
    Assert.assertEquals(
      "d",
      CodeableConcept().apply { addCoding(Coding("s", "c", "d")) }.valueToString()
    )
    Assert.assertEquals(
      "3.4",
      Quantity()
        .apply {
          this.value = BigDecimal.valueOf(3.4)
          this.unit = "G"
        }
        .valueToString()
    )
  }

  @Test
  fun `Resource#generateReferenceValue() should return correct reference`() {
    val resource = Patient().apply { id = "123456" }

    Assert.assertEquals("Patient/123456", resource.referenceValue())
  }

  @Test
  fun `Questionnaire#prepareQuestionsForReadingOrEditing should retain custom extension`() {
    val questionnaire = mutableListOf<Questionnaire.QuestionnaireItemComponent>()
    questionnaire.add(
      Questionnaire.QuestionnaireItemComponent().apply {
        text = "Group"
        type = Questionnaire.QuestionnaireItemType.GROUP
      }
    )
    questionnaire.add(
      Questionnaire.QuestionnaireItemComponent().apply {
        prefix = "1."
        text = "Photo of device"
        readOnly = false
        addExtension(
          Extension().apply {
            url = FhirCoreQuestionnaireFragment.PHOTO_CAPTURE_URL
            setValue(
              StringType().apply { value = FhirCoreQuestionnaireFragment.PHOTO_CAPTURE_NAME }
            )
          }
        )
      }
    )
    questionnaire.add(
      Questionnaire.QuestionnaireItemComponent().apply {
        prefix = "2."
        text = "Barcode"
        readOnly = false
        addExtension(
          Extension().apply {
            url = FhirCoreQuestionnaireFragment.BARCODE_URL
            setValue(StringType().apply { value = FhirCoreQuestionnaireFragment.BARCODE_NAME })
          }
        )
      }
    )

    questionnaire.prepareQuestionsForReadingOrEditing("path", true)

    Assert.assertTrue(
      questionnaire[1].hasExtension(FhirCoreQuestionnaireFragment.PHOTO_CAPTURE_URL)
    )
    Assert.assertTrue(questionnaire[1].readOnly)
    Assert.assertTrue(questionnaire[2].hasExtension(FhirCoreQuestionnaireFragment.BARCODE_URL))
    Assert.assertTrue(questionnaire[2].readOnly)
  }
}
