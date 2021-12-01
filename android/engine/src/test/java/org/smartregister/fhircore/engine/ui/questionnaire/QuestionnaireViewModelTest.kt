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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.app.Application
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.logicalId
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.StructureMap
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.FormConfigUtil
import org.smartregister.fhircore.engine.util.extension.deleteRelatedResources
import org.smartregister.fhircore.engine.util.extension.retainMetadata

class QuestionnaireViewModelTest : RobolectricTest() {

  private lateinit var fhirEngine: FhirEngine
  private lateinit var questionnaireViewModel: QuestionnaireViewModel
  private lateinit var context: Application
  private lateinit var defaultRepo: DefaultRepository

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule var coroutineRule = CoroutineTestRule()

  @Before
  fun setUp() {
    clearAllMocks()

    context = ApplicationProvider.getApplicationContext()

    fhirEngine = mockk()
    coEvery { fhirEngine.save(any()) } answers {}
    coEvery { fhirEngine.update(any()) } answers {}

    defaultRepo = spyk(DefaultRepository(fhirEngine))
    coEvery { defaultRepo.save(any()) } returns Unit
    coEvery { defaultRepo.addOrUpdate(any()) } returns Unit

    questionnaireViewModel = spyk(QuestionnaireViewModel(context))
    ReflectionHelpers.setField(questionnaireViewModel, "defaultRepository", defaultRepo)
  }

  @Test
  fun testLoadQuestionnaireShouldCallFhirEngine() {
    coEvery { fhirEngine.load(Questionnaire::class.java, "12345") } returns
      Questionnaire().apply { id = "12345" }
    val result = runBlocking { questionnaireViewModel.loadQuestionnaire("12345") }
    Assert.assertEquals("12345", result!!.logicalId)
  }

  @Test
  fun testLoadQuestionnaireShouldSetQuestionnaireQuestionsToReadOnly() {
    val questionnaire =
      Questionnaire().apply {
        id = "12345"
        item =
          listOf(
            Questionnaire.QuestionnaireItemComponent().apply {
              type = Questionnaire.QuestionnaireItemType.GROUP
              linkId = "q1-grp"
              item =
                listOf(
                  Questionnaire.QuestionnaireItemComponent().apply {
                    type = Questionnaire.QuestionnaireItemType.TEXT
                    linkId = "q1-name"
                  }
                )
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              type = Questionnaire.QuestionnaireItemType.CHOICE
              linkId = "q2-gender"
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              type = Questionnaire.QuestionnaireItemType.DATE
              linkId = "q3-date"
            }
          )
      }
    coEvery { fhirEngine.load(Questionnaire::class.java, "12345") } returns questionnaire
    questionnaireViewModel = spyk(QuestionnaireViewModel(context))
    ReflectionHelpers.setField(questionnaireViewModel, "defaultRepository", defaultRepo)

    val result = runBlocking { questionnaireViewModel.loadQuestionnaire("12345", true) }

    Assert.assertTrue(result!!.item[0].item[0].readOnly)
    Assert.assertEquals("q1-name", result!!.item[0].item[0].linkId)
    Assert.assertTrue(result.item[1].readOnly)
    Assert.assertEquals("q2-gender", result.item[1].linkId)
    Assert.assertTrue(result.item[2].readOnly)
    Assert.assertEquals("q3-date", result.item[2].linkId)
  }

  @Test
  fun testLoadQuestionnaireShouldMakeQuestionsReadOnlyAndAddInitialExpressionExtension() {
    val questionnaire =
      Questionnaire().apply {
        id = "12345"
        item =
          listOf(
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "patient-first-name"
              type = Questionnaire.QuestionnaireItemType.TEXT
              item =
                listOf(
                  Questionnaire.QuestionnaireItemComponent().apply {
                    linkId = "patient-last-name"
                    type = Questionnaire.QuestionnaireItemType.TEXT
                  }
                )
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "patient-age"
              type = Questionnaire.QuestionnaireItemType.INTEGER
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "patient-contact"
              type = Questionnaire.QuestionnaireItemType.GROUP
              item =
                listOf(
                  Questionnaire.QuestionnaireItemComponent().apply {
                    linkId = "patient-dob"
                    type = Questionnaire.QuestionnaireItemType.DATE
                  },
                  Questionnaire.QuestionnaireItemComponent().apply {
                    linkId = "patient-related-person"
                    type = Questionnaire.QuestionnaireItemType.GROUP
                    item =
                      listOf(
                        Questionnaire.QuestionnaireItemComponent().apply {
                          linkId = "rp-name"
                          type = Questionnaire.QuestionnaireItemType.TEXT
                        }
                      )
                  }
                )
            }
          )
      }

    coEvery { fhirEngine.load(Questionnaire::class.java, "12345") } returns questionnaire

    val result = runBlocking { questionnaireViewModel.loadQuestionnaire("12345", true) }

    Assert.assertEquals("12345", result!!.logicalId)
    Assert.assertTrue(result!!.item[0].readOnly)
    Assert.assertEquals("patient-first-name", result!!.item[0].linkId)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-first-name').answer.value",
      (result!!.item[0].extension[0].value as Expression).expression
    )
    Assert.assertEquals("patient-last-name", result!!.item[0].item[0].linkId)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-first-name').answer.item.where(linkId = 'patient-last-name').answer.value",
      (result!!.item[0].item[0].extension[0].value as Expression).expression
    )
    Assert.assertTrue(result!!.item[1].readOnly)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-age').answer.value",
      (result!!.item[1].extension[0].value as Expression).expression
    )
    Assert.assertFalse(result!!.item[2].readOnly)
    Assert.assertEquals(0, result!!.item[2].extension.size)
    Assert.assertTrue(result!!.item[2].item[0].readOnly)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-contact').item.where(linkId = 'patient-dob').answer.value",
      (result!!.item[2].item[0].extension[0].value as Expression).expression
    )
    Assert.assertFalse(result!!.item[2].item[1].readOnly)
    Assert.assertTrue(result!!.item[2].item[1].item[0].readOnly)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-contact').item.where(linkId = 'patient-related-person').item.where(linkId = 'rp-name').answer.value",
      (result!!.item[2].item[1].item[0].extension[0].value as Expression).expression
    )
  }

  @Test
  fun testLoadQuestionnaireShouldMakeQuestionsEditableAndAddInitialExpressionExtension() {
    val questionnaire =
      Questionnaire().apply {
        id = "12345"
        item =
          listOf(
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "patient-first-name"
              type = Questionnaire.QuestionnaireItemType.TEXT
              item =
                listOf(
                  Questionnaire.QuestionnaireItemComponent().apply {
                    linkId = "patient-last-name"
                    type = Questionnaire.QuestionnaireItemType.TEXT
                  }
                )
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "patient-age"
              type = Questionnaire.QuestionnaireItemType.INTEGER
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "patient-contact"
              type = Questionnaire.QuestionnaireItemType.GROUP
              item =
                listOf(
                  Questionnaire.QuestionnaireItemComponent().apply {
                    linkId = "patient-dob"
                    type = Questionnaire.QuestionnaireItemType.DATE
                  },
                  Questionnaire.QuestionnaireItemComponent().apply {
                    linkId = "patient-related-person"
                    type = Questionnaire.QuestionnaireItemType.GROUP
                    item =
                      listOf(
                        Questionnaire.QuestionnaireItemComponent().apply {
                          linkId = "rp-name"
                          type = Questionnaire.QuestionnaireItemType.TEXT
                        }
                      )
                  }
                )
            }
          )
      }

    coEvery { fhirEngine.load(Questionnaire::class.java, "12345") } returns questionnaire

    val result = runBlocking { questionnaireViewModel.loadQuestionnaire("12345", editMode = true) }

    Assert.assertEquals("12345", result!!.logicalId)
    Assert.assertFalse(result!!.item[0].readOnly)
    Assert.assertEquals("patient-first-name", result!!.item[0].linkId)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-first-name').answer.value",
      (result!!.item[0].extension[0].value as Expression).expression
    )
    Assert.assertEquals("patient-last-name", result!!.item[0].item[0].linkId)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-first-name').answer.item.where(linkId = 'patient-last-name').answer.value",
      (result!!.item[0].item[0].extension[0].value as Expression).expression
    )
    Assert.assertFalse(result!!.item[1].readOnly)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-age').answer.value",
      (result!!.item[1].extension[0].value as Expression).expression
    )
    Assert.assertFalse(result!!.item[2].readOnly)
    Assert.assertEquals(0, result!!.item[2].extension.size)
    Assert.assertFalse(result!!.item[2].item[0].readOnly)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-contact').item.where(linkId = 'patient-dob').answer.value",
      (result!!.item[2].item[0].extension[0].value as Expression).expression
    )
    Assert.assertFalse(result!!.item[2].item[1].readOnly)
    Assert.assertFalse(result!!.item[2].item[1].item[0].readOnly)
    Assert.assertEquals(
      "QuestionnaireResponse.item.where(linkId = 'patient-contact').item.where(linkId = 'patient-related-person').item.where(linkId = 'rp-name').answer.value",
      (result!!.item[2].item[1].item[0].extension[0].value as Expression).expression
    )
  }

  @Test
  fun testGetQuestionnaireConfigShouldLoadRightConfig() {
    mockkObject(FormConfigUtil)
    every { FormConfigUtil.loadConfig(any(), any()) } returns
      listOf(
        QuestionnaireConfig("appId", "my-form", "My Form", "0001"),
        QuestionnaireConfig("appId", "patient-registration", "Add Patient", "1903")
      )

    val result = runBlocking {
      questionnaireViewModel.getQuestionnaireConfig("patient-registration")
    }
    Assert.assertEquals("patient-registration", result.form)
    Assert.assertEquals("Add Patient", result.title)
    Assert.assertEquals("1903", result.identifier)

    unmockkObject(FormConfigUtil)
  }

  @Test
  fun testExtractAndSaveResourcesWithTargetStructureMapShouldCallExtractionService() {
    mockkObject(ResourceMapper)
    val patient = Patient().apply { id = "123456" }

    coEvery { fhirEngine.load(Patient::class.java, any()) } returns Patient()
    coEvery { fhirEngine.load(StructureMap::class.java, any()) } returns StructureMap()
    coEvery { ResourceMapper.extract(any(), any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { this.resource = patient } }

    val questionnaire =
      Questionnaire().apply {
        addUseContext().apply {
          code = Coding().apply { code = "focus" }
          value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
        }
        addExtension(
          "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-targetStructureMap",
          CanonicalType("1234")
        )
      }

    val questionnaireResponse = QuestionnaireResponse()

    questionnaireViewModel.extractAndSaveResources(
      "12345",
      context,
      questionnaire,
      questionnaireResponse
    )

    coVerify { defaultRepo.addOrUpdate(patient) }
    coVerify { defaultRepo.addOrUpdate(questionnaireResponse) }
    coVerify(timeout = 2000) { ResourceMapper.extract(any(), any(), any(), any()) }

    unmockkObject(ResourceMapper)
  }

  @Test
  fun testExtractAndSaveResourcesWithExtractionExtensionAndNullResourceShouldAssignTags() {
    val questionnaire =
      Questionnaire().apply {
        addUseContext().apply {
          code = Coding().apply { code = "focus" }
          value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
        }
        addExtension(
          Extension(
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext",
            Expression().apply {
              language = "application/x-fhir-query"
              expression = "Patient"
              name = "Patient"
            }
          )
        )
      }

    questionnaireViewModel.extractAndSaveResources(
      null,
      context,
      questionnaire,
      QuestionnaireResponse()
    )

    val patientSlot = slot<Patient>()

    coVerify { defaultRepo.addOrUpdate(capture(patientSlot)) }

    Assert.assertEquals("1234567", patientSlot.captured.meta.tagFirstRep.code)

    unmockkObject(ResourceMapper)
  }

  @Test
  fun testExtractAndSaveResourcesWithResourceIdShouldSaveQuestionnaireResponse() {
    coEvery { fhirEngine.load(Patient::class.java, any()) } returns Patient()

    val questionnaireResponseSlot = slot<QuestionnaireResponse>()
    val questionnaire =
      Questionnaire().apply {
        addUseContext().apply {
          code = Coding().apply { code = "focus" }
          value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
        }
      }

    questionnaireViewModel.extractAndSaveResources(
      "12345",
      context,
      questionnaire,
      QuestionnaireResponse()
    )

    coVerify(timeout = 2000) { defaultRepo.addOrUpdate(capture(questionnaireResponseSlot)) }

    Assert.assertEquals(
      "12345",
      questionnaireResponseSlot.captured.subject.reference.replace("Patient/", "")
    )
    Assert.assertEquals("1234567", questionnaireResponseSlot.captured.meta.tagFirstRep.code)
  }

  @Test
  fun testLoadPatientShouldReturnPatientResource() {
    val patient =
      Patient().apply {
        name =
          listOf(
            HumanName().apply {
              given = listOf(StringType("John"))
              family = "Doe"
            }
          )
      }
    coEvery { fhirEngine.load(Patient::class.java, "1") } returns patient

    runBlocking {
      val loadedPatient = questionnaireViewModel.loadPatient("1")

      Assert.assertEquals(
        patient.name.first().given.first().value,
        loadedPatient?.name?.first()?.given?.first()?.value
      )
      Assert.assertEquals(patient.name.first().family, loadedPatient?.name?.first()?.family)
    }
  }

  @Test
  fun testLoadRelatedPersonShouldReturnOnlyOneItemList() {
    val relatedPerson =
      RelatedPerson().apply {
        name =
          listOf(
            HumanName().apply {
              given = listOf(StringType("John"))
              family = "Doe"
            }
          )
      }

    coEvery { defaultRepo.loadRelatedPersons("1") } returns listOf(relatedPerson)

    runBlocking {
      val list = questionnaireViewModel.loadRelatedPerson("1")
      Assert.assertEquals(1, list?.size)
      val result = list?.get(0)
      Assert.assertEquals(
        relatedPerson.name.first().given.first().value,
        result?.name?.first()?.given?.first()?.value
      )
      Assert.assertEquals(relatedPerson.name.first().family, result?.name?.first()?.family)
    }
  }

  @Test
  fun testSaveResourceShouldVerifyResourceSaveMethodCall() {
    coEvery { defaultRepo.save(any()) } returns Unit
    questionnaireViewModel.saveResource(mockk())
    coVerify(exactly = 1) { defaultRepo.save(any()) }
  }

  @Test
  fun testGetPopulationResourcesShouldReturnListOfResources() {

    coEvery { fhirEngine.load(Patient::class.java, "2") } returns Patient().apply { id = "2" }
    coEvery { defaultRepo.loadRelatedPersons("2") } returns
      listOf(RelatedPerson().apply { id = "3" })

    val intent = Intent()
    intent.putStringArrayListExtra(
      QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES,
      arrayListOf(
        "{\"resourceType\":\"Patient\",\"id\":\"1\",\"text\":{\"status\":\"generated\",\"div\":\"\"}}"
      )
    )
    intent.putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "2")

    runBlocking {
      val resourceList = questionnaireViewModel.getPopulationResources(intent)
      Assert.assertEquals(3, resourceList.size)
    }
  }

  @Test
  fun testSaveQuestionnaireResponseShouldCallAddOrUpdateWhenResourceIdIsNotBlank() {

    val questionnaire = Questionnaire().apply { id = "qId" }
    val questionnaireResponse = QuestionnaireResponse()
    coEvery { defaultRepo.addOrUpdate(any()) } returns Unit
    coEvery { fhirEngine.load(Patient::class.java, "12345") } returns
      Patient().apply { id = "12345" }

    runBlocking {
      questionnaireViewModel.saveQuestionnaireResponse(
        "12345",
        questionnaire,
        questionnaireResponse
      )
    }

    coVerify { defaultRepo.addOrUpdate(questionnaireResponse) }
  }

  @Test
  fun testSaveQuestionnaireResponseShouldAddIdAndAuthoredWhenQuestionnaireResponseDoesNotHaveId() {

    val questionnaire = Questionnaire().apply { id = "qId" }
    val questionnaireResponse = QuestionnaireResponse()
    coEvery { defaultRepo.addOrUpdate(any()) } returns Unit
    coEvery { fhirEngine.load(Patient::class.java, "12345") } returns
      Patient().apply { id = "12345" }

    Assert.assertNull(questionnaireResponse.id)
    Assert.assertNull(questionnaireResponse.authored)

    runBlocking {
      questionnaireViewModel.saveQuestionnaireResponse(
        "12345",
        questionnaire,
        questionnaireResponse
      )
    }

    Assert.assertNotNull(questionnaireResponse.id)
    Assert.assertNotNull(questionnaireResponse.authored)
  }

  @Test
  fun testSaveQuestionnaireResponseShouldRetainIdAndAuthoredWhenQuestionnaireResponseHasId() {

    val authoredDate = Date()
    val questionnaire = Questionnaire().apply { id = "qId" }
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "qrId"
        authored = authoredDate
      }
    coEvery { defaultRepo.addOrUpdate(any()) } returns Unit
    coEvery { fhirEngine.load(Patient::class.java, "12345") } returns
      Patient().apply { id = "12345" }

    runBlocking {
      questionnaireViewModel.saveQuestionnaireResponse(
        "12345",
        questionnaire,
        questionnaireResponse
      )
    }

    Assert.assertEquals("qrId", questionnaireResponse.id)
    Assert.assertEquals(authoredDate, questionnaireResponse.authored)
  }

  @Test
  fun testExtractAndSaveResourcesShouldCallDeleteRelatedResourcesWhenEditModeIsTrue() {
    mockkObject(ResourceMapper)
    val id = "qrId"
    val authoredDate = Date()
    val versionId = "5"
    val author = Reference()
    val patient = Patient().apply { Patient@ this.id = "123456" }

    coEvery { fhirEngine.load(Patient::class.java, any()) } returns Patient()
    coEvery { fhirEngine.load(StructureMap::class.java, any()) } returns StructureMap()
    coEvery { ResourceMapper.extract(any(), any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { this.resource = patient } }

    val questionnaire =
      spyk(
        Questionnaire().apply {
          addUseContext().apply {
            code = Coding().apply { code = "focus" }
            value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
          }
          addExtension(
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-targetStructureMap",
            CanonicalType("1234")
          )
        }
      )

    val oldQuestionnaireResponse =
      QuestionnaireResponse().apply {
        setId(id)
        authored = authoredDate
        setAuthor(author)
        meta.apply { setVersionId(versionId) }
      }
    val questionnaireResponse = spyk(QuestionnaireResponse())

    questionnaireViewModel.editQuestionnaireResponse = oldQuestionnaireResponse
    questionnaireViewModel.extractAndSaveResources(
      "12345",
      context,
      questionnaire,
      questionnaireResponse,
      true
    )

    verify { questionnaireResponse.retainMetadata(oldQuestionnaireResponse) }
    coVerify { questionnaireResponse.deleteRelatedResources(defaultRepo) }
    Assert.assertEquals(patient, questionnaireResponse.contained[0])

    unmockkObject(ResourceMapper)
  }
}
