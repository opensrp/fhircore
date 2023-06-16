/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.questionnaire

import android.app.Application
import android.content.Context
import android.os.Looper
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Age
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.StructureMap
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.extractType
import org.smartregister.fhircore.engine.util.extension.generateMissingItems
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.extension.retainMetadata
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.BuildConfig
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.model.practitioner.FhirPractitionerDetails
import org.smartregister.model.practitioner.KeycloakUserDetails
import org.smartregister.model.practitioner.PractitionerDetails

@HiltAndroidTest
class QuestionnaireViewModelTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator
  @Inject lateinit var jsonParser: IParser
  @Inject lateinit var configService: ConfigService
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private val fhirEngine: FhirEngine = mockk()
  private val context: Application = ApplicationProvider.getApplicationContext()
  private lateinit var questionnaireViewModel: QuestionnaireViewModel
  private lateinit var defaultRepo: DefaultRepository
  private val libraryEvaluator: LibraryEvaluator = mockk()
  private lateinit var samplePatientRegisterQuestionnaire: Questionnaire
  private lateinit var questionnaireConfig: QuestionnaireConfig
  private val configRulesExecutor: ConfigRulesExecutor = mockk()

  @Before
  @ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()

    // Write practitioner and organization to shared preferences
    sharedPreferencesHelper.write(
      SharedPreferenceKey.PRACTITIONER_ID.name,
      practitionerDetails().fhirPractitionerDetails.practitionerId.valueToString()
    )

    sharedPreferencesHelper.write(ResourceType.Organization.name, listOf("105"))

    defaultRepo =
      spyk(
        DefaultRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          configService = configService,
          configRulesExecutor = configRulesExecutor
        )
      )

    val configurationRegistry = mockk<ConfigurationRegistry>()

    questionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        type = QuestionnaireType.READ_ONLY,
        setPractitionerDetails = false,
        setOrganizationDetails = false,
        resourceIdentifier = "2",
        resourceType = ResourceType.Patient
      )

    questionnaireViewModel =
      spyk(
        QuestionnaireViewModel(
          defaultRepository = defaultRepo,
          configurationRegistry = configurationRegistry,
          transformSupportServices = mockk(),
          dispatcherProvider = defaultRepo.dispatcherProvider,
          sharedPreferencesHelper = sharedPreferencesHelper,
          libraryEvaluator = libraryEvaluator,
          fhirCarePlanGenerator = fhirCarePlanGenerator
        )
      )

    coEvery { fhirEngine.create(any()) } answers { listOf() }
    coEvery { fhirEngine.update(any()) } answers {}

    coEvery { defaultRepo.create(any()) } returns emptyList()
    coEvery { defaultRepo.addOrUpdate(resource = any()) } just runs

    // Setup sample resources
    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val qJson =
      context.assets.open("sample_patient_registration.json").bufferedReader().use { it.readText() }

    samplePatientRegisterQuestionnaire = iParser.parseResource(qJson) as Questionnaire
  }

  @Test
  fun testLoadQuestionnaireShouldCallDefaultRepoLoadResource() {
    coEvery { fhirEngine.get(ResourceType.Questionnaire, "12345") } returns
      Questionnaire().apply { id = "12345" }

    val result = runBlocking {
      questionnaireViewModel.loadQuestionnaire("12345", QuestionnaireType.DEFAULT)
    }

    coVerify { fhirEngine.get(ResourceType.Questionnaire, "12345") }
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
    coEvery { fhirEngine.get(ResourceType.Questionnaire, "12345") } returns questionnaire

    ReflectionHelpers.setField(questionnaireViewModel, "defaultRepository", defaultRepo)

    val result = runBlocking {
      questionnaireViewModel.loadQuestionnaire("12345", QuestionnaireType.READ_ONLY)
    }

    assertTrue(result!!.item[0].item[0].readOnly)
    Assert.assertEquals("q1-name", result.item[0].item[0].linkId)
    assertTrue(result.item[1].readOnly)
    Assert.assertEquals("q2-gender", result.item[1].linkId)
    assertTrue(result.item[2].readOnly)
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

    coEvery { fhirEngine.get(ResourceType.Questionnaire, "12345") } returns questionnaire

    val result = runBlocking {
      questionnaireViewModel.loadQuestionnaire("12345", QuestionnaireType.READ_ONLY)
    }

    Assert.assertEquals("12345", result!!.logicalId)
    assertTrue(result.item[0].readOnly)
    Assert.assertEquals("patient-first-name", result.item[0].linkId)
    Assert.assertEquals("patient-last-name", result.item[0].item[0].linkId)
    assertTrue(result.item[1].readOnly)
    Assert.assertFalse(result.item[2].readOnly)
    Assert.assertEquals(0, result.item[2].extension.size)
    assertTrue(result.item[2].item[0].readOnly)
    Assert.assertFalse(result.item[2].item[1].readOnly)
    assertTrue(result.item[2].item[1].item[0].readOnly)
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

    coEvery { fhirEngine.get(ResourceType.Questionnaire, "12345") } returns questionnaire

    val result = runBlocking {
      questionnaireViewModel.loadQuestionnaire("12345", QuestionnaireType.EDIT)
    }

    Assert.assertEquals("12345", result!!.logicalId)
    Assert.assertFalse(result.item[0].readOnly)
    Assert.assertEquals("patient-first-name", result.item[0].linkId)
    Assert.assertEquals("patient-last-name", result.item[0].item[0].linkId)
    Assert.assertFalse(result.item[1].readOnly)
    Assert.assertFalse(result.item[2].readOnly)
    Assert.assertEquals(0, result.item[2].extension.size)
    Assert.assertFalse(result.item[2].item[0].readOnly)
    Assert.assertFalse(result.item[2].item[1].readOnly)
    Assert.assertFalse(result.item[2].item[1].item[0].readOnly)
  }

  @Test
  fun testLoadQuestionnaireShouldMakeQuestionsEditableWithReadonlyAndAddInitialExpressionExtension() {
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
              readOnly = true
            },
          )
      }

    coEvery { fhirEngine.get(ResourceType.Questionnaire, "12345") } returns questionnaire

    val result = runBlocking {
      questionnaireViewModel.loadQuestionnaire("12345", QuestionnaireType.EDIT)
    }

    Assert.assertEquals("12345", result!!.logicalId)
    Assert.assertFalse(result.item[0].readOnly)
    Assert.assertEquals("patient-first-name", result.item[0].linkId)
    Assert.assertEquals("patient-last-name", result.item[0].item[0].linkId)
    assertTrue(result.item[1].readOnly)
  }

  @Test
  fun testLoadQuestionnaireShouldPrepopulateFieldsWithPrepopulationParams() {

    val prePopulationParams =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "patient-age",
          dataType = Enumerations.DataType.INTEGER,
          key = "patientAge",
          value = "100"
        )
      )

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
              readOnly = true
            },
          )
      }

    coEvery { fhirEngine.get(ResourceType.Questionnaire, "12345") } returns questionnaire

    val result = runBlocking {
      questionnaireViewModel.loadQuestionnaire("12345", QuestionnaireType.EDIT, prePopulationParams)
    }

    Assert.assertEquals("12345", result!!.logicalId)
    Assert.assertEquals("100", result.item[1].initial[0].value.valueToString())
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testExtractAndSaveResourcesWithTargetStructureMapShouldCallExtractionService() {
    mockkObject(ResourceMapper)
    val patient = samplePatient()

    coEvery { fhirEngine.get(ResourceType.Patient, any()) } returns Patient()
    coEvery { fhirEngine.get(ResourceType.StructureMap, any()) } returns StructureMap()
    coEvery { fhirEngine.get(ResourceType.Group, any()) } returns Group()
    coEvery { ResourceMapper.extract(any(), any(), any()) } returns
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

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    runTest {
      val questionnaireResponse = QuestionnaireResponse()

      questionnaireViewModel.extractAndSaveResources(
        context = context,
        questionnaireResponse = questionnaireResponse,
        questionnaire = questionnaire,
        questionnaireConfig = questionnaireConfig
      )

      coVerify { defaultRepo.addOrUpdate(resource = patient) }
      coVerify { defaultRepo.addOrUpdate(resource = questionnaireResponse) }
      coVerify(timeout = 10000) { ResourceMapper.extract(any(), any(), any()) }
    }
    unmockkObject(ResourceMapper)
  }

  @Test
  @Ignore("Fix java.lang.NullPointerException")
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

    val questionnaireResponse = QuestionnaireResponse().apply { subject = Reference("12345") }

    questionnaireViewModel.extractAndSaveResources(
      context = context,
      questionnaireResponse = questionnaireResponse,
      questionnaire = questionnaire,
      questionnaireConfig = questionnaireConfig
    )

    coVerify { defaultRepo.addOrUpdate(resource = any()) }

    unmockkObject(ResourceMapper)
  }

  @Test
  fun testExtractAndSaveResourcesWithResourceIdShouldSaveQuestionnaireResponse() {
    coEvery { fhirEngine.get(ResourceType.Patient, "2") } returns samplePatient()

    val questionnaireResponseSlot = slot<QuestionnaireResponse>()
    val questionnaire =
      Questionnaire().apply {
        id = "12345"
        addUseContext().apply {
          code = Coding().apply { code = "focus" }
          value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
        }
      }

    questionnaireViewModel.extractAndSaveResources(
      context = context,
      questionnaireResponse =
        QuestionnaireResponse().apply { subject = Reference().apply { reference = "Patient/2" } },
      questionnaire = questionnaire,
      questionnaireConfig = questionnaireConfig
    )

    coVerify(timeout = 2000) {
      defaultRepo.addOrUpdate(resource = capture(questionnaireResponseSlot))
    }

    Assert.assertEquals(
      "2",
      questionnaireResponseSlot.captured.subject.reference.replace("Patient/", "")
    )
    Assert.assertEquals("1234567", questionnaireResponseSlot.captured.meta.tagFirstRep.code)
  }

  @Test
  fun testExtractAndSaveResourcesWithEditModeShouldSaveQuestionnaireResponse() {
    mockkObject(ResourceMapper)

    coEvery { ResourceMapper.extract(any(), any(), any()) } returns
      Bundle().apply { addEntry().resource = samplePatient() }
    coEvery { fhirEngine.get(ResourceType.Patient, "2") } returns samplePatient().apply { id = "2" }
    coEvery { defaultRepo.addOrUpdate(resource = any()) } just runs

    val questionnaireResponseSlot = slot<QuestionnaireResponse>()
    val patientSlot = slot<Resource>()
    val questionnaire =
      Questionnaire().apply {
        id = "12345"
        addUseContext().apply {
          code = Coding().apply { code = "focus" }
          value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
        }
        addExtension().url = "sdc-questionnaire-itemExtractionContext"
      }
    questionnaireConfig = questionnaireConfig.copy(type = QuestionnaireType.EDIT)

    questionnaireViewModel.extractAndSaveResources(
      context = context,
      questionnaireResponse =
        QuestionnaireResponse().apply { subject = Reference().apply { reference = "Patient/2" } },
      questionnaire = questionnaire,
      questionnaireConfig = questionnaireConfig
    )

    coVerifyOrder {
      defaultRepo.addOrUpdate(resource = capture(patientSlot))
      defaultRepo.addOrUpdate(resource = capture(questionnaireResponseSlot))
    }

    Assert.assertEquals("2", patientSlot.captured.id)
    unmockkObject(ResourceMapper)
  }

  @Test
  fun testSaveResourceShouldVerifyResourceSaveMethodCall() {
    coEvery { defaultRepo.create(any()) } returns emptyList()
    questionnaireViewModel.saveResource(mockk())
    coVerify(exactly = 1) { defaultRepo.create(any(), any()) }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testSaveQuestionnaireResponseShouldCallAddOrUpdateWhenResourceIdIsNotBlank() {
    val questionnaire = Questionnaire().apply { id = "qId" }
    val questionnaireResponse = QuestionnaireResponse().apply { subject = Reference("12345") }
    coEvery { defaultRepo.addOrUpdate(resource = any()) } returns Unit

    runBlocking {
      questionnaireViewModel.saveQuestionnaireResponse(questionnaire, questionnaireResponse)
    }

    coVerify { defaultRepo.addOrUpdate(resource = questionnaireResponse) }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testSaveQuestionnaireResponseWithExperimentalQuestionnaireShouldNotSave() {
    val questionnaire = Questionnaire().apply { experimental = true }
    val questionnaireResponse = QuestionnaireResponse()

    runBlocking {
      questionnaireViewModel.saveQuestionnaireResponse(questionnaire, questionnaireResponse)
    }

    coVerify(inverse = true) { defaultRepo.addOrUpdate(resource = questionnaireResponse) }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testSaveQuestionnaireResponseWithActionParameterIdShouldAddOrUpdate() {
    val theId = "the Id"
    val uuid = "the uuid"
    val relatedResourceId = "Group/$uuid"
    val questionnaire = Questionnaire().apply { id = theId }
    val questionnaireResponse = QuestionnaireResponse()
    val resource = Group().apply { id = uuid }

    coEvery { fhirEngine.loadResource<Questionnaire>(theId) } returns questionnaire
    coEvery { defaultRepo.loadResource(uuid, ResourceType.Group) } returns resource

    runBlocking {
      questionnaireViewModel.loadQuestionnaire(
        theId,
        QuestionnaireType.DEFAULT,
        listOf(
          ActionParameter("key", ActionParameterType.UPDATE_DATE_ON_EDIT, value = relatedResourceId)
        )
      )
      questionnaireViewModel.saveQuestionnaireResponse(questionnaire, questionnaireResponse)
    }

    coVerify { defaultRepo.addOrUpdate(resource = questionnaireResponse) }
    coVerify { defaultRepo.addOrUpdate(resource = resource) }
  }

  @Test
  fun testExtractAndSaveResourcesWithExperimentalQuestionnaireShouldNotSave() {
    mockkObject(ResourceMapper)

    coEvery { ResourceMapper.extract(any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { resource = Patient() } }

    val questionnaire =
      Questionnaire().apply {
        experimental = true
        addExtension().url = "sdc-questionnaire-itemExtractionContext"
      }
    val questionnaireResponse = QuestionnaireResponse()

    runBlocking {
      questionnaireViewModel.extractAndSaveResources(
        context = ApplicationProvider.getApplicationContext(),
        questionnaireResponse = questionnaireResponse,
        questionnaire = questionnaire,
        questionnaireConfig = questionnaireConfig
      )
    }

    coVerify { ResourceMapper.extract(any(), any(), any()) }
    coVerify(inverse = true) { defaultRepo.addOrUpdate(resource = questionnaireResponse) }

    unmockkObject(ResourceMapper)
  }

  @Test
  fun testExtractQuestionnaireResponseShouldAddIdAndAuthoredWhenQuestionnaireResponseDoesNotHaveId() {

    val questionnaire = Questionnaire().apply { id = "qId" }
    val questionnaireResponse = QuestionnaireResponse().apply { subject = Reference("12345") }
    coEvery { defaultRepo.addOrUpdate(resource = any()) } returns Unit

    Assert.assertNull(questionnaireResponse.id)
    Assert.assertNull(questionnaireResponse.authored)

    runBlocking {
      questionnaireViewModel.extractAndSaveResources(
        context = context,
        questionnaireResponse = questionnaireResponse,
        questionnaire = questionnaire,
        questionnaireConfig = questionnaireConfig
      )
    }

    Assert.assertNotNull(questionnaireResponse.id)
    Assert.assertNotNull(questionnaireResponse.authored)
  }

  @Test
  fun testExtractQuestionnaireResponseShouldRetainIdAndAuthoredWhenQuestionnaireResponseHasId() {

    val authoredDate = Date()
    val questionnaire = Questionnaire().apply { id = "qId" }
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "qrId"
        authored = authoredDate
        subject = Reference("12345")
      }
    coEvery { defaultRepo.addOrUpdate(resource = any()) } returns Unit

    runBlocking {
      questionnaireViewModel.extractAndSaveResources(
        context,
        questionnaireResponse,
        questionnaire,
        questionnaireConfig
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
    val patient = samplePatient()

    coEvery { fhirEngine.get(ResourceType.Patient, any()) } returns Patient()
    coEvery { fhirEngine.get(ResourceType.StructureMap, any()) } returns StructureMap()
    coEvery { ResourceMapper.extract(any(), any(), any()) } returns
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
      context = context,
      questionnaireResponse = questionnaireResponse,
      questionnaire = questionnaire,
      questionnaireConfig = questionnaireConfig.copy(type = QuestionnaireType.EDIT)
    )

    verify { questionnaireResponse.retainMetadata(oldQuestionnaireResponse) }

    unmockkObject(ResourceMapper)
  }

  @Test
  fun testCalculateDobFromAge() {
    val expectedBirthDate = Calendar.getInstance()
    val ageInput = expectedBirthDate.get(Calendar.YEAR) - 2010
    expectedBirthDate.set(Calendar.YEAR, 2010)
    expectedBirthDate.set(Calendar.MONTH, 1)
    expectedBirthDate.set(Calendar.DAY_OF_YEAR, 1)
    val resultBirthDate = Calendar.getInstance()
    resultBirthDate.time = questionnaireViewModel.calculateDobFromAge(ageInput)
    Assert.assertEquals(expectedBirthDate.get(Calendar.YEAR), resultBirthDate.get(Calendar.YEAR))
  }

  @Test
  fun testGetAgeInputFromQuestionnaire() {
    val expectedAge = 25
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "12345"
        item =
          listOf(
            QuestionnaireResponseItemComponent().apply {
              linkId = "q1-grp"
              item =
                listOf(
                  QuestionnaireResponseItemComponent().apply {
                    linkId = QuestionnaireActivity.QUESTIONNAIRE_AGE
                    answer =
                      listOf(
                        QuestionnaireResponseItemAnswerComponent().apply { value = DecimalType(25) }
                      )
                  }
                )
            }
          )
      }
    Assert.assertEquals(expectedAge, questionnaireViewModel.getAgeInput(questionnaireResponse))
  }

  @Test
  fun `saveBundleResources() should call saveResources()`() {
    val bundle = Bundle()
    val size = 1

    for (i in 1..size) {
      val bundleEntry = Bundle.BundleEntryComponent()
      bundleEntry.resource =
        Patient().apply {
          name =
            listOf(
              HumanName().apply {
                family = "Doe"
                given = listOf(StringType("John"))
              }
            )
        }
      bundle.addEntry(bundleEntry)
    }
    bundle.total = size

    // call the method under test
    runBlocking { questionnaireViewModel.saveBundleResources(bundle) }

    coVerify(exactly = size) { defaultRepo.addOrUpdate(resource = any()) }
  }

  @Test
  fun `saveBundleResources() should call saveResources and inject resourceId()`() {
    val bundle = Bundle()
    val size = 5
    val resource = slot<Resource>()

    val bundleEntry = Bundle.BundleEntryComponent()
    bundleEntry.resource =
      Patient().apply {
        name =
          listOf(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            }
          )
      }
    bundle.addEntry(bundleEntry)
    bundle.total = size

    // call the method under test
    runBlocking { questionnaireViewModel.saveBundleResources(bundle) }

    coVerify(exactly = 1) { defaultRepo.addOrUpdate(resource = capture(resource)) }
  }

  @Test
  fun `fetchStructureMap() should call fhirEngine load and parse out the resourceId`() {
    val structureMap = StructureMap()
    val structureMapIdSlot = slot<String>()

    coEvery { fhirEngine.get(ResourceType.StructureMap, any()) } returns structureMap

    runBlocking {
      questionnaireViewModel.fetchStructureMap("https://someorg.org/StructureMap/678934")
    }

    coVerify(exactly = 1) { fhirEngine.get(ResourceType.StructureMap, capture(structureMapIdSlot)) }

    Assert.assertEquals("678934", structureMapIdSlot.captured)
  }

  @Test
  fun `extractAndSaveResources() should call saveBundleResources when Questionnaire uses Definition-based extraction`() {
    coEvery { fhirEngine.get(ResourceType.Questionnaire, any()) } returns
      samplePatientRegisterQuestionnaire
    coEvery { fhirEngine.get(ResourceType.Group, any()) } returns Group()

    val questionnaire = Questionnaire()
    questionnaire.extension.add(
      Extension(
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext",
        Expression().apply {
          language = "application/x-fhir-query"
          expression = "Patient"
        }
      )
    )
    questionnaire.addSubjectType("Patient")
    val questionnaireResponse = QuestionnaireResponse()
    val bundle = Bundle().apply { id = "1234" }
    runBlocking { questionnaireViewModel.saveBundleResources(bundle) }
    coEvery { questionnaireViewModel.saveBundleResources(bundle) } just runs
    runBlocking {
      questionnaireViewModel.performExtraction(context, questionnaire, questionnaireResponse)
    }
    coEvery { questionnaireViewModel.performExtraction(any(), any(), any()) } returns
      Bundle().apply { addEntry().resource = samplePatient() }
    runBlocking {
      questionnaireViewModel.saveQuestionnaireResponse(questionnaire, questionnaireResponse)
    }
    coEvery { questionnaireViewModel.saveQuestionnaireResponse(any(), any()) } just runs

    questionnaireViewModel.extractAndSaveResources(
      context = context,
      questionnaireResponse = questionnaireResponse,
      questionnaire = questionnaire,
      questionnaireConfig = questionnaireConfig
    )

    coVerify(exactly = 1, timeout = 2000) { questionnaireViewModel.saveBundleResources(bundle) }
    coVerify(exactly = 1, timeout = 2000) {
      questionnaireViewModel.saveQuestionnaireResponse(questionnaire, questionnaireResponse)
    }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testPerformExtractionOnNullBundle() {
    runTest {
      val bundle = null
      val questionnaire = Questionnaire()
      val questionnaireResponse = QuestionnaireResponse()
      val questionnaireConfig = questionnaireConfig
      questionnaireViewModel.performExtraction(
        questionnaireResponse,
        questionnaireConfig,
        questionnaire,
        bundle
      )
      coVerifyOrder(inverse = true) {
        questionnaireViewModel.extractCqlOutput(questionnaire, questionnaireResponse, bundle)
        questionnaireViewModel.extractCarePlan(questionnaireResponse, bundle, questionnaireConfig)
      }
    }
  }

  @Test
  fun testPerformExtractionOnSuccessReturnsABundleAndShowsSuccessToast() {
    val context = mockk<Context>(relaxed = true)
    val bundle = Bundle()
    val questionnaire = Questionnaire()
    questionnaire.name = "eCBIS Add Family Member Registration"
    coEvery {
      questionnaireViewModel.performExtraction(
        context,
        questionnaire = questionnaire,
        questionnaireResponse = QuestionnaireResponse()
      )
    } returns bundle
    Assert.assertNotNull(bundle)

    context.getString(R.string.structure_success)
    coVerify { context.getString(R.string.structure_success) }
    context.showToast(
      context.getString(R.string.structure_success, questionnaire.name),
      Toast.LENGTH_LONG
    )
    coVerify {
      context.showToast(
        context.getString(R.string.structure_success, questionnaire.name),
        Toast.LENGTH_LONG
      )
    }
  }

  @Test
  fun testPerformExtractionOnFailureShowsMissingStructureMapToast() {
    val context = mockk<Context>(relaxed = true)
    val questionnaire = Questionnaire()
    questionnaire.name = "eCBIS Add Family Member Registration"
    val missingStructureMapExceptionMessage =
      context.getString(R.string.structure_map_missing_message, questionnaire.name)
    val questionnaireResponse = QuestionnaireResponse()

    coEvery { questionnaireViewModel.retrieveStructureMapProvider() } throws
      NullPointerException(
        "NullPointerException when invoking StructureMap on Null Object reference"
      )

    coEvery {
      questionnaireViewModel.performExtraction(context, questionnaire, questionnaireResponse)
    }
    context.getString(R.string.structure_map_missing_message)
    context.showToast(missingStructureMapExceptionMessage, Toast.LENGTH_LONG)
    coVerify { context.getString(R.string.structure_map_missing_message) }

    coVerify { context.showToast(missingStructureMapExceptionMessage, Toast.LENGTH_LONG) }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testPerformExtractionOnFailureShowsErrorToast() = runTest {
    val context = mockk<Context>(relaxed = true)
    val questionnaire = Questionnaire()
    val questionnaireResponse = QuestionnaireResponse()
    questionnaire.name = "eCBIS Add Family Member Registration"
    val errorMessage = context.getString(R.string.structuremap_failed, questionnaire.name)
    coEvery { questionnaireViewModel.retrieveStructureMapProvider() } throws
      Exception("Failed to process resources")

    questionnaireViewModel.performExtraction(context, questionnaire, questionnaireResponse)
    coVerify {
      questionnaireViewModel.performExtraction(context, questionnaire, questionnaireResponse)
    }
    coVerify { context.getString(R.string.structuremap_failed, questionnaire.name) }
    coVerify { context.showToast(errorMessage, Toast.LENGTH_LONG) }
  }

  @Test
  fun `extractAndSaveResources() should call runCqlLibrary when Questionnaire uses cqf-library extension`() {
    coEvery { fhirEngine.get(ResourceType.Questionnaire, any()) } returns
      samplePatientRegisterQuestionnaire
    coEvery { fhirEngine.get(ResourceType.Group, any()) } returns Group()

    val questionnaire =
      Questionnaire().apply {
        addSubjectType("Patient")
        extension =
          mutableListOf(
            Extension(
              "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext",
              Expression().apply {
                language = "application/x-fhir-query"
                expression = "Patient"
              }
            ),
            Extension(
              "http://hl7.org/fhir/uv/sdc/StructureDefinition/cqf-library",
              CanonicalType("Library/1234")
            )
          )
      }
    val questionnaireResponse = QuestionnaireResponse()

    val patient = samplePatient().apply { id = "2" }
    coEvery { fhirEngine.get(ResourceType.Patient, "2") } returns patient
    coEvery { fhirEngine.get<Patient>(any()) } returns patient
    coEvery { questionnaireViewModel.saveBundleResources(any()) } just runs
    coEvery { questionnaireViewModel.performExtraction(any(), any(), any()) } returns
      Bundle().apply { addEntry().resource = patient }

    coEvery {
      libraryEvaluator.runCqlLibrary(
        eq("1234"),
        eq(patient),
        eq(Bundle().apply { addEntry().resource = patient }),
        eq(defaultRepo),
        eq(false)
      )
    } returns listOf()

    val slotQuestionnaire = slot<Questionnaire>()
    val slotQuestionnaireResponse = slot<QuestionnaireResponse>()
    coEvery {
      questionnaireViewModel.saveQuestionnaireResponse(
        capture(slotQuestionnaire),
        capture(slotQuestionnaireResponse)
      )
    } just runs

    questionnaireViewModel.extractAndSaveResources(
      context = context,
      questionnaireResponse = questionnaireResponse,
      questionnaire = questionnaire,
      questionnaireConfig = questionnaireConfig
    )
    coVerify(exactly = 1, timeout = 2000) { questionnaireViewModel.saveBundleResources(any()) }
    coVerify { fhirEngine.get(ResourceType.Patient, "2") }

    assertEquals(questionnaire, slotQuestionnaire.captured)
    assertEquals(questionnaireResponse, slotQuestionnaireResponse.captured)
  }

  @Test
  fun testSaveResourceShouldCallDefaultRepositorySave() {
    val sourcePatient = Patient().apply { id = "test_patient_1_id" }
    questionnaireViewModel.saveResource(sourcePatient)

    coVerify { defaultRepo.create(true, sourcePatient) }
  }

  @Test
  fun `getStructureMapProvider() should return valid provider`() {
    Assert.assertNull(questionnaireViewModel.structureMapProvider)

    Assert.assertNotNull(questionnaireViewModel.retrieveStructureMapProvider())
  }

  @Test
  fun `structureMapProvider should call fetchStructureMap()`() {
    val resourceUrl = "https://fhir.org/StructureMap/89"
    val structureMapProvider = questionnaireViewModel.retrieveStructureMapProvider()

    coEvery { questionnaireViewModel.fetchStructureMap(any()) } returns StructureMap()

    runBlocking { structureMapProvider.invoke(resourceUrl, SimpleWorkerContext()) }

    coVerify { questionnaireViewModel.fetchStructureMap(resourceUrl) }
  }

  fun testHandlePatientSubjectShouldReturnSetCorrectReference() {
    val questionnaire = Questionnaire().apply { addSubjectType("Patient") }
    val questionnaireResponse = QuestionnaireResponse()

    Assert.assertFalse(questionnaireResponse.hasSubject())

    questionnaireViewModel.handleQuestionnaireResponseSubject(
      "123",
      questionnaire,
      questionnaireResponse
    )

    Assert.assertEquals("Patient/123", questionnaireResponse.subject.reference)
  }

  @Test
  fun testHandleOrganizationSubjectShouldReturnSetCorrectReference() {
    val questionnaire = Questionnaire().apply { addSubjectType("Organization") }
    val questionnaireResponse = QuestionnaireResponse()

    Assert.assertFalse(questionnaireResponse.hasSubject())

    questionnaireViewModel.handleQuestionnaireResponseSubject(
      "123",
      questionnaire,
      questionnaireResponse
    )

    Assert.assertEquals("Organization/105", questionnaireResponse.subject.reference)
  }

  private fun practitionerDetails(): PractitionerDetails {
    return PractitionerDetails().apply {
      userDetail = KeycloakUserDetails().apply { id = "12345" }
      fhirPractitionerDetails =
        FhirPractitionerDetails().apply {
          id = "12345"
          practitionerId = StringType("12345")
        }
    }
  }

  private fun samplePatient() =
    Patient().apply {
      Patient@ this.id = "123456"
      this.birthDate = questionnaireViewModel.calculateDobFromAge(25)
    }

  @Test
  fun testPartialQuestionnaireResponseHasValues() {
    // empty QuestionnaireResponse
    Assert.assertFalse(
      questionnaireViewModel.partialQuestionnaireResponseHasValues(QuestionnaireResponse())
    )

    // empty item
    Assert.assertFalse(
      questionnaireViewModel.partialQuestionnaireResponseHasValues(
        QuestionnaireResponse().apply { item = mutableListOf(QuestionnaireResponseItemComponent()) }
      )
    )

    // with answer
    Assert.assertFalse(
      questionnaireViewModel.partialQuestionnaireResponseHasValues(
        QuestionnaireResponse().apply {
          item =
            mutableListOf(
              QuestionnaireResponseItemComponent().apply {
                answer = mutableListOf(QuestionnaireResponseItemAnswerComponent())
              }
            )
        }
      )
    )

    // with answer and value that is empty
    Assert.assertFalse(
      questionnaireViewModel.partialQuestionnaireResponseHasValues(
        QuestionnaireResponse().apply {
          item =
            mutableListOf(
              QuestionnaireResponseItemComponent().apply {
                answer =
                  mutableListOf(QuestionnaireResponseItemAnswerComponent().apply { value = Age() })
              }
            )
        }
      )
    )

    // with answer and value that is not empty
    assertTrue(
      questionnaireViewModel.partialQuestionnaireResponseHasValues(
        QuestionnaireResponse().apply {
          item =
            mutableListOf(
              QuestionnaireResponseItemComponent().apply {
                answer =
                  mutableListOf(
                    QuestionnaireResponseItemAnswerComponent().apply {
                      value = Age().apply { value = BigDecimal.ONE }
                    }
                  )
              }
            )
        }
      )
    )

    // second answer has non empty value
    assertTrue(
      questionnaireViewModel.partialQuestionnaireResponseHasValues(
        QuestionnaireResponse().apply {
          item =
            mutableListOf(
              QuestionnaireResponseItemComponent().apply {
                answer =
                  mutableListOf(
                    QuestionnaireResponseItemAnswerComponent(),
                    QuestionnaireResponseItemAnswerComponent().apply {
                      value = Age().apply { value = BigDecimal.ONE }
                    }
                  )
              }
            )
        }
      )
    )
  }

  @Test
  fun testSavePartialQuestionnaireResponseCallsSaveResponse() {
    val questionnaireResponse = QuestionnaireResponse()
    questionnaireViewModel.savePartialQuestionnaireResponse(Questionnaire(), questionnaireResponse)
    Assert.assertEquals(
      QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS,
      questionnaireResponse.status
    )
    coVerify { questionnaireViewModel.saveQuestionnaireResponse(any(), any()) }
  }

  @Test
  fun testAddPractitionerInfoShouldSetGeneralPractitionerReferenceToPatientResource() {
    val patient = samplePatient()

    questionnaireViewModel.appendPractitionerInfo(patient)

    Assert.assertEquals("Practitioner/12345", patient.generalPractitioner.first().reference)
  }

  @Test
  fun testAddOrganizationInfoShouldSetOrganizationToQuestionnaireResponse() {
    // For patient
    val patient = samplePatient()
    questionnaireViewModel.appendOrganizationInfo(patient)
    Assert.assertNotNull("Organization/105", patient.managingOrganization.reference)

    // For group
    val group = Group().apply { id = "123" }
    questionnaireViewModel.appendOrganizationInfo(group)
    Assert.assertEquals("Organization/105", group.managingEntity.reference)
  }

  @Test
  fun testAppVersionIsAppendedToPatientResource() {
    // Version name
    val versionName = BuildConfig.VERSION_NAME

    // For Patient
    val patient = samplePatient()
    questionnaireViewModel.appendAppVersion(context, resource = patient)
    val tag = patient.meta.tag
    val appVersionTag = tag[0]
    Assert.assertEquals("https://smartregister.org/app-version", appVersionTag.system)
    Assert.assertEquals(versionName, appVersionTag.code)
    Assert.assertEquals("Application Version", appVersionTag.display)
  }

  @Test
  fun testAddPractitionerInfoShouldSetIndividualPractitionerReferenceToEncounterResource() {
    val encounter = Encounter().apply { this.id = "123456" }
    questionnaireViewModel.appendPractitionerInfo(encounter)
    Assert.assertEquals("Practitioner/12345", encounter.participant.first().individual.reference)
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testAppendPatientsAndRelatedPersonsToGroupsShouldAddMembersToGroup() {
    runTest {
      val patient = samplePatient()
      val familyGroup =
        Group().apply {
          id = "grp1"
          name = "Mandela Family"
        }
      coEvery { fhirEngine.get<Group>(familyGroup.id) } returns familyGroup
      questionnaireViewModel.addGroupMember(patient, familyGroup.id)
      Assert.assertEquals(1, familyGroup.member.size)

      val familyGroup2 = Group().apply { id = "grp2" }
      coEvery { fhirEngine.get<Group>(familyGroup2.id) } returns familyGroup2
      // Sets the managing entity
      questionnaireViewModel.addGroupMember(RelatedPerson().apply { id = "rel1" }, familyGroup2.id)
      Assert.assertNotNull(familyGroup2.managingEntity)
    }
  }

  @Test
  fun testRemoveGroupCallsDefaultRepositoryRemoveGroup() {
    val groupId = "group-1"
    val deactivateMembers = false
    Assert.assertFalse(questionnaireViewModel.removeOperation.value!!)
    questionnaireViewModel.removeGroup(
      groupId = groupId,
      removeGroup = true,
      deactivateMembers = deactivateMembers
    )

    coVerify { defaultRepo.removeGroup(groupId, deactivateMembers, emptyMap()) }
    assertTrue(questionnaireViewModel.removeOperation.value!!)
  }

  @Test
  fun testRemoveGroupMemberCallsDefaultRepositoryRemoveGroupMember() {

    val memberId = "member-id"
    val groupIdentifier = "group_id"
    val memberResourceType = "Patient"
    val removeMember = true
    Assert.assertFalse(questionnaireViewModel.removeOperation.value!!)
    questionnaireViewModel.removeGroupMember(
      memberId = memberId,
      groupIdentifier = groupIdentifier,
      memberResourceType = memberResourceType,
      removeMember = removeMember
    )

    coVerify {
      defaultRepo.removeGroupMember(
        memberId = memberId,
        groupId = groupIdentifier,
        groupMemberResourceType = memberResourceType,
        emptyMap()
      )
    }
    assertTrue(questionnaireViewModel.removeOperation.value!!)
  }

  @Test
  fun testDeleteResourceCallsDefaultRepositoryDelete() {
    val resourceType = ResourceType.Patient
    val resourceIdentifier = "rdsfjkdfh-dfdf-dfsd"
    questionnaireViewModel.deleteResource(
      resourceType = resourceType,
      resourceIdentifier = resourceIdentifier
    )

    coVerify { defaultRepo.delete(resourceType = resourceType, resourceId = resourceIdentifier) }
  }

  @Test
  fun testGenerateMissingItemsForQuestionnaire() {
    val patientRegistrationQuestionnaire =
      "patient-registration-questionnaire/sample/missingitem-questionnaire.json".readFile()

    val patientRegistrationQuestionnaireResponse =
      "patient-registration-questionnaire/sample/missingitem-questionnaire-response.json".readFile()

    val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

    val questionnaire =
      iParser.parseResource(Questionnaire::class.java, patientRegistrationQuestionnaire)

    val questionnaireResponse =
      iParser.parseResource(
        QuestionnaireResponse::class.java,
        patientRegistrationQuestionnaireResponse
      )

    questionnaire.item.generateMissingItems(questionnaireResponse.item)

    assertTrue(questionnaireResponse.item.size <= questionnaire.item.size)
  }

  @Test
  fun testGenerateMissingItemsForQuestionnaireResponse() {
    val patientRegistrationQuestionnaire =
      "patient-registration-questionnaire/questionnaire.json".readFile()

    val patientRegistrationQuestionnaireResponse =
      "patient-registration-questionnaire/questionnaire-response.json".readFile()

    val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

    val questionnaire =
      iParser.parseResource(Questionnaire::class.java, patientRegistrationQuestionnaire)

    val questionnaireResponse =
      iParser.parseResource(
        QuestionnaireResponse::class.java,
        patientRegistrationQuestionnaireResponse
      )

    questionnaireResponse.generateMissingItems(questionnaire)

    assertTrue(questionnaireResponse.item.size <= questionnaire.item.size)
  }
  @Test
  fun testLoadQuestionnaireShouldUReturnCorrectItemsWithUpdateOnEdit() {
    val updateResourcesIdsParams =
      questionnaireViewModel::class.java.getDeclaredField("editQuestionnaireResourceParams")
    updateResourcesIdsParams.isAccessible = true
    val expected =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.UPDATE_DATE_ON_EDIT,
          linkId = "patient-age-3",
          dataType = Enumerations.DataType.INTEGER,
          key = "patientAge-three",
          value = "20"
        ),
        ActionParameter(
          paramType = ActionParameterType.UPDATE_DATE_ON_EDIT,
          linkId = "patient-age-4",
          dataType = Enumerations.DataType.INTEGER,
          key = "patientAge-four",
          value = "25"
        )
      )

    val prePopulationParams =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "patient-age-1",
          dataType = Enumerations.DataType.INTEGER,
          key = "patientAge-one",
          value = "10"
        ),
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "patient-age-2",
          dataType = Enumerations.DataType.INTEGER,
          key = "patientAge-two",
          value = "15"
        ),
        ActionParameter(
          paramType = ActionParameterType.UPDATE_DATE_ON_EDIT,
          linkId = "patient-age-3",
          dataType = Enumerations.DataType.INTEGER,
          key = "patientAge-three",
          value = "20"
        ),
        ActionParameter(
          paramType = ActionParameterType.UPDATE_DATE_ON_EDIT,
          linkId = "patient-age-4",
          dataType = Enumerations.DataType.INTEGER,
          key = "patientAge-four",
          value = "25"
        )
      )

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
              readOnly = true
            },
          )
      }

    coEvery { fhirEngine.get(ResourceType.Questionnaire, "12345") } returns questionnaire

    runBlocking {
      questionnaireViewModel.loadQuestionnaire("12345", QuestionnaireType.EDIT, prePopulationParams)
    }
    coVerify {
      questionnaireViewModel.loadQuestionnaire("12345", QuestionnaireType.EDIT, prePopulationParams)
    }
    assertEquals(expected, updateResourcesIdsParams.get(questionnaireViewModel))
  }

  @Test
  fun testExtractAndSaveResourcesShouldUpdateResourceLastUpdatedPatient() {
    mockkObject(ResourceMapper)
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "12345"
        subject = Reference("Patient/2511692c-74d5-401d-bc29-a70aff33d176")
      }
    val patient = Patient().apply { id = "2511692c-74d5-401d-bc29-a70aff33d176" }
    val resourceId = questionnaireResponse.subject.reference.extractLogicalIdUuid()
    val resourceType =
      questionnaireResponse
        .subject
        .extractType()
        .toString()
        .resourceClassType()
        .newInstance()
        .resourceType
    coEvery { ResourceMapper.extract(any(), any(), any()) } returns
      Bundle().apply { addEntry().resource = patient }
    coEvery { fhirEngine.get(ResourceType.Patient, "12345") } returns patient
    coEvery { defaultRepo.addOrUpdate(resource = any()) } just runs
    val questionnaire =
      Questionnaire().apply {
        id = "12345"
        addUseContext().apply {
          code = Coding().apply { code = "1234" }
          value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
        }
        addExtension().url = "sdc-questionnaire-itemExtractionContext"
      }

    questionnaireConfig = questionnaireConfig.copy(type = QuestionnaireType.DEFAULT)
    coEvery { fhirEngine.get(ResourceType.Patient, "2511692c-74d5-401d-bc29-a70aff33d176") } returns
      patient
    coEvery { defaultRepo.loadResource(resourceId, resourceType) } returns patient
    questionnaireViewModel.extractAndSaveResources(
      context = context,
      questionnaireResponse = questionnaireResponse,
      questionnaire = questionnaire,
      questionnaireConfig = questionnaireConfig
    )
    runBlocking { defaultRepo.loadResource(resourceId, resourceType) }
    coVerify { defaultRepo.loadResource(resourceId = resourceId, resourceType = resourceType) }
    val formatter = SimpleDateFormat("dd MM yyyy HH:mm:ss")
    Assert.assertEquals(formatter.format(patient.meta.lastUpdated), formatter.format(Date()))
    unmockkObject(ResourceMapper)
  }
  @Test
  fun testExtractAndSaveResourcesShouldUpdateResourceLastUpdatedGroup() {
    mockkObject(ResourceMapper)
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "12345"
        subject = Reference("Group/2511692c-74d5-401d-bc29-a70aff33d176")
      }
    val group = Group().apply { id = "2511692c-74d5-401d-bc29-a70aff33d176" }
    coEvery { ResourceMapper.extract(any(), any(), any()) } returns
      Bundle().apply { addEntry().resource = group }
    coEvery { fhirEngine.get(ResourceType.Patient, "12345") } returns group
    coEvery { defaultRepo.addOrUpdate(resource = any()) } just runs
    val questionnaire =
      Questionnaire().apply {
        id = "12345"
        addUseContext().apply {
          code = Coding().apply { code = "1234" }
          value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
        }
        addExtension().url = "sdc-questionnaire-itemExtractionContext"
      }

    questionnaireConfig = questionnaireConfig.copy(type = QuestionnaireType.DEFAULT)
    coEvery { fhirEngine.get(ResourceType.Patient, "2511692c-74d5-401d-bc29-a70aff33d176") } returns
      group
    val beforeTime = Date()
    questionnaireViewModel.extractAndSaveResources(
      context = context,
      questionnaireResponse = questionnaireResponse,
      questionnaire = questionnaire,
      questionnaireConfig = questionnaireConfig
    )
    val afterTime = Date()
    assertTrue(
      (group.meta.lastUpdated.equals(beforeTime) || group.meta.lastUpdated.after(beforeTime)) &&
        (group.meta.lastUpdated.equals(afterTime) || group.meta.lastUpdated.before(afterTime))
    )
    unmockkObject(ResourceMapper)
  }

  @Test
  fun `updateResourceLastUpdatedLinkedAsSubject() should catch ResourceNotFoundException`() {
    val resourceNotFoundException =
      ResourceNotFoundException(type = "Resource not found exception", id = "1234")
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "1234"
        subject.reference = "Patient/123467"
      }
    coEvery { defaultRepo.loadResource(any(), any()) } throws resourceNotFoundException
    runBlocking {
      questionnaireViewModel.updateResourceLastUpdatedLinkedAsSubject(questionnaireResponse)
    }
    // if no exception is thrown that is sufficient to show the test passed
  }

  @Test
  fun testGetQuestionnaireResponseFromDbOrPopulation_whenResourceMapIsNotEmpty_retrievePatientPlusRelatedPersonScenario_returnQuestionnaireResponseFromPopulation() {
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
    val patient = Patient().apply { id = "patient-1" }
    val relatedPerson = RelatedPerson().apply { id = "related-person-1" }
    val resourceMap: Map<ResourceType?, String> = mapOf(patient.resourceType to patient.id)

    // Mandatory get QR from DB, but only used when there's no population resource which triggers an
    // exception
    coEvery {
      fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = patient.id })
        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = questionnaire.id })
      }
    } returns listOf(QuestionnaireResponse())

    // Gets population resources from resource map
    coEvery { fhirEngine.get(patient.resourceType, patient.id) } returns patient
    coEvery {
      fhirEngine.search<RelatedPerson> {
        filter(QuestionnaireResponse.SUBJECT, { value = patient.id })
        filter(RelatedPerson.PATIENT, { value = "${patient.resourceType.name}/${patient.id}" })
      }
    } returns listOf(relatedPerson)

    val result = runBlocking {
      questionnaireViewModel.getQuestionnaireResponseFromDbOrPopulation(
        questionnaire = questionnaire,
        subjectId = patient.id,
        subjectType = patient.resourceType,
        questionnaireConfig = questionnaireConfig,
        resourceMap = resourceMap
      )
    }

    assertTrue(result.hasItem())
    coVerify { fhirEngine.get(patient.resourceType, patient.id) }
    coVerify {
      fhirEngine.search<RelatedPerson> {
        filter(QuestionnaireResponse.SUBJECT, { value = patient.id })
        filter(RelatedPerson.PATIENT, { value = "${patient.resourceType.name}/${patient.id}" })
      }
    }
    val slotPopulationResources = slot<ArrayList<Resource>>()
    coVerify {
      questionnaireViewModel.populateQuestionnaireResponse(any(), capture(slotPopulationResources))
    }
    assert(slotPopulationResources.captured.size == 2)
    assertIs<Patient>(slotPopulationResources.captured[0])
    assertIs<RelatedPerson>(slotPopulationResources.captured[1])
  }

  @Test
  fun testGetQuestionnaireResponseFromDbOrPopulation_whenResourceMapIsNotEmpty_retrieveThatResourceOnlyScenario_returnQuestionnaireResponseFromPopulation() {
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
    val group = Group().apply { id = "group-1" }
    val resourceMap: Map<ResourceType?, String> = mapOf(group.resourceType to group.id)

    // Mandatory get QR from DB, but only used when there's no population resource which triggers an
    // exception
    coEvery {
      fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = group.id })
        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = questionnaire.id })
      }
    } returns listOf()

    // Gets population resources from resource map
    coEvery { fhirEngine.get(group.resourceType, group.id) } returns group

    val result = runBlocking {
      questionnaireViewModel.getQuestionnaireResponseFromDbOrPopulation(
        questionnaire = questionnaire,
        subjectId = group.id,
        subjectType = group.resourceType,
        questionnaireConfig = questionnaireConfig,
        resourceMap = resourceMap
      )
    }

    assertTrue(result.hasItem())
    coVerify { fhirEngine.get(group.resourceType, group.id) }
    val slotPopulationResources = slot<ArrayList<Resource>>()
    coVerify {
      questionnaireViewModel.populateQuestionnaireResponse(any(), capture(slotPopulationResources))
    }
    assert(slotPopulationResources.captured.size == 1)
    assertIs<Group>(slotPopulationResources.captured[0])
  }

  @Test
  fun testGetQuestionnaireResponseFromDbOrPopulation_whenResourceMapIsEmpty_returnQuestionnaireResponseFromPopulation() {
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
    val group = Group().apply { id = "group-1" }
    val resourceMap: Map<ResourceType?, String> = mapOf()

    // Mandatory get QR from DB, but only used when there's no population resource which triggers an
    // exception
    coEvery {
      fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = group.id })
        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = questionnaire.id })
      }
    } returns listOf()

    // Gets population resources from subjectId and subjectType
    coEvery { fhirEngine.get(group.resourceType, group.id) } returns group
    coEvery {
      questionnaireViewModel.getQuestionnaireResponseFromDbOrPopulation(
        questionnaire = any(),
        subjectId = group.id,
        subjectType = group.resourceType,
        questionnaireConfig = questionnaireConfig,
        resourceMap = resourceMap
      )
    } returns QuestionnaireResponse().apply { item = listOf(QuestionnaireResponseItemComponent()) }
    coEvery { fhirEngine.get(group.resourceType, group.id) } returns Patient()
    val result = runBlocking {
      questionnaireViewModel.getQuestionnaireResponseFromDbOrPopulation(
          questionnaire = questionnaire,
          subjectId = group.id,
          subjectType = group.resourceType,
          questionnaireConfig = questionnaireConfig,
          resourceMap = resourceMap
        )
        .apply { item = listOf(QuestionnaireResponseItemComponent()) }
    }
    assertFalse(result.hasItem())
    coVerify { fhirEngine.get(group.resourceType, group.id) }
    val slotPopulationResources = slot<ArrayList<Resource>>()
    coVerify {
      questionnaireViewModel.populateQuestionnaireResponse(any(), capture(slotPopulationResources))
    }
    assert(slotPopulationResources.captured.size == 1)
    assertIs<Group>(slotPopulationResources.captured[0])
  }

  @Test
  fun testGetQuestionnaireResponseFromDbOrPopulation_whenPopulationResourceIsEmpty_returnQuestionnaireResponseFromDB() {
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
    val group = Group().apply { id = "group-1" }
    val resourceMap: Map<ResourceType?, String> = mapOf()

    // Mandatory get QR from DB, to be used when there's no population resource which triggers an
    // exception
    coEvery {
      fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = group.id })
        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = questionnaire.id })
      }
    } returns listOf(QuestionnaireResponse().apply { generateMissingItems(questionnaire) })

    // Population resource not found
    coEvery { fhirEngine.get(group.resourceType, group.id) } throws
      ResourceNotFoundException(group.resourceType.name, group.id)

    val result = runBlocking {
      questionnaireViewModel.getQuestionnaireResponseFromDbOrPopulation(
        questionnaire = questionnaire,
        subjectId = group.id,
        subjectType = group.resourceType,
        questionnaireConfig = questionnaireConfig,
        resourceMap = resourceMap
      )
    }

    assertTrue(result.hasItem())
    coVerify { fhirEngine.get(group.resourceType, group.id) }
    val slotPopulationResources = slot<ArrayList<Resource>>()
    coVerify {
      questionnaireViewModel.populateQuestionnaireResponse(any(), capture(slotPopulationResources))
    }
    assertTrue(slotPopulationResources.captured.isEmpty())
  }
}
