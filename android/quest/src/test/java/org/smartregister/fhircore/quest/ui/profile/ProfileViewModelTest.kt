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

package org.smartregister.fhircore.quest.ui.profile

import android.content.Context
import android.os.Bundle
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItemConfig
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler

@HiltAndroidTest
class ProfileViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  @get:Rule(order = 1)
  val coroutineRule = CoroutineTestRule()
  private lateinit var registerRepository: RegisterRepository
  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor
  @Inject lateinit var rulesExecutor: RulesExecutor
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var resourceData: ResourceData
  private lateinit var expectedBaseResource: Patient

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()
    expectedBaseResource = Faker.buildPatient()
    resourceData =
      ResourceData(
        baseResourceId = expectedBaseResource.logicalId,
        baseResourceType = expectedBaseResource.resourceType,
        computedValuesMap = emptyMap()
      )
    registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = mockk(),
          dispatcherProvider = DefaultDispatcherProvider(),
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          fhirPathDataExtractor = fhirPathDataExtractor
        )
      )
    coEvery { registerRepository.loadProfileData(any(), any(), paramsList = emptyArray()) } returns
      RepositoryResourceData(
        queryResult = RepositoryResourceData.QueryResult.Search(resource = Faker.buildPatient())
      )
    runBlocking {
      configurationRegistry.loadConfigurations(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        appId = APP_DEBUG
      ) {}
    }

    profileViewModel =
      ProfileViewModel(
        registerRepository = registerRepository,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = coroutineRule.testDispatcherProvider,
        fhirPathDataExtractor = fhirPathDataExtractor,
        rulesExecutor = rulesExecutor
      )
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveProfileUiState() {
    runBlocking {
      profileViewModel.retrieveProfileUiState(
        "householdProfile",
        "sampleId",
        paramsList = emptyArray()
      )
    }

    assertNotNull(profileViewModel.profileUiState.value)
    val theResourceData = profileViewModel.profileUiState.value.resourceData
    assertNotNull(theResourceData)
    assertEquals(expectedBaseResource.logicalId, theResourceData.baseResourceId)
    assertEquals(expectedBaseResource.resourceType, theResourceData.baseResourceType)

    val profileConfiguration = profileViewModel.profileUiState.value.profileConfiguration
    assertEquals("app", profileConfiguration?.appId)
    assertEquals("profile", profileConfiguration?.configType)
    assertEquals("householdProfile", profileConfiguration?.id)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testShouldLaunchQuestionnaireWhenQuestionnaireResponseIsFromDb() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val resourceData =
      ResourceData(
        baseResourceId = "Patient/999",
        baseResourceType = ResourceType.Patient,
        computedValuesMap = emptyMap(),
      )
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444", type = QuestionnaireType.EDIT),
        params =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
              dataType = DataType.INTEGER,
              key = "maleCondomPreviousBalance",
              value = "100"
            )
          )
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

    val questionnaire =
      Questionnaire().apply {
        id = "Questionnaire/444"
        url = "Questionnaire/444"
      }
    coEvery { registerRepository.fhirEngine.loadResource<Questionnaire>("444") } returns
      questionnaire

    val questionnaireResponse = QuestionnaireResponse().apply { id = "QuestionnaireResponse/777" }
    coEvery {
      registerRepository.fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = "${ResourceType.Patient.name}/999" })
        filter(
          QuestionnaireResponse.QUESTIONNAIRE,
          { value = "${ResourceType.Questionnaire.name}/444" }
        )
      }
    } returns listOf(questionnaireResponse)

    val event =
      ProfileEvent.OverflowMenuClick(
        navController = navController,
        resourceData = resourceData,
        overflowMenuItemConfig = overflowMenuItemConfig,
      )

    runBlocking {
      profileViewModel.onEvent(event)
      delay(100)

      coVerify { registerRepository.fhirEngine.loadResource<Questionnaire>("444") }

      coVerify {
        registerRepository.fhirEngine.search<QuestionnaireResponse> {
          filter(QuestionnaireResponse.SUBJECT, { value = "${ResourceType.Patient.name}/999" })
          filter(
            QuestionnaireResponse.QUESTIONNAIRE,
            { value = "${ResourceType.Questionnaire.name}/444" }
          )
        }
      }

      val slot = slot<Bundle>()
      verify {
        (context as QuestionnaireHandler).launchQuestionnaire<Any>(
          context = context,
          intentBundle = capture(slot),
          questionnaireConfig = actionConfig.questionnaire,
          actionParams = actionConfig.params
        )
      }

      val capturedIntentBundle = slot.captured
      assertNotNull(capturedIntentBundle.getString(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE))
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testShouldHaveErrorSnackBarMessageWhenQuestionnaireConfigIsNull() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        params =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
              dataType = DataType.INTEGER,
              key = "maleCondomPreviousBalance",
              value = "100"
            )
          )
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

    val event =
      ProfileEvent.OverflowMenuClick(
        navController = navController,
        resourceData = null,
        overflowMenuItemConfig = overflowMenuItemConfig,
      )

    runBlocking {
      profileViewModel.onEvent(event)
      delay(100)
    }

    Assert.assertNotNull(profileViewModel.snackBarStateFlow)
    coVerify(inverse = true) { registerRepository.fhirEngine }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testShouldHaveErrorSnackBarMessageWhenQuestionnaireIsNull() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444", type = QuestionnaireType.EDIT),
        params =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
              dataType = DataType.INTEGER,
              key = "maleCondomPreviousBalance",
              value = "100"
            )
          )
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

    coEvery { registerRepository.fhirEngine.get(ResourceType.Questionnaire, "444") } throws
      ResourceNotFoundException("type", "id")

    val event =
      ProfileEvent.OverflowMenuClick(
        navController = navController,
        resourceData = null,
        overflowMenuItemConfig = overflowMenuItemConfig,
      )

    runBlocking {
      profileViewModel.onEvent(event)
      delay(100)
    }

    Assert.assertNotNull(profileViewModel.snackBarStateFlow)
    coVerify { registerRepository.fhirEngine.loadResource<Questionnaire>("444") }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testShouldNotLaunchQuestionnaireWhenQuestionnaireIsNotFoundInDb() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val resourceData =
      ResourceData(
        baseResourceId = "Patient/999",
        baseResourceType = ResourceType.Patient,
        computedValuesMap = emptyMap(),
      )
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444", type = QuestionnaireType.EDIT),
        params =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
              dataType = DataType.INTEGER,
              key = "maleCondomPreviousBalance",
              value = "100"
            )
          )
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

    coEvery { registerRepository.fhirEngine.loadResource<Questionnaire>("444") } returns null

    val event =
      ProfileEvent.OverflowMenuClick(
        navController = navController,
        resourceData = resourceData,
        overflowMenuItemConfig = overflowMenuItemConfig,
      )

    runBlocking {
      profileViewModel.onEvent(event)
      delay(100)

      coVerify { registerRepository.fhirEngine.loadResource<Questionnaire>("444") }
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testShouldLaunchQuestionnaireWhenQuestionnaireResponseIsFromPopulationAndPatientBaseResource() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val resourceData =
      ResourceData(
        baseResourceId = "Patient/999",
        baseResourceType = ResourceType.Patient,
        computedValuesMap = emptyMap(),
      )
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444", type = QuestionnaireType.READ_ONLY),
        params =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
              dataType = DataType.INTEGER,
              key = "maleCondomPreviousBalance",
              value = "100"
            )
          )
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

    val questionnaire =
      Questionnaire().apply {
        id = "Questionnaire/444"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "patient-assigned-id"
            type = Questionnaire.QuestionnaireItemType.TEXT
            extension =
              listOf(
                Extension(
                  "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                  Expression().apply {
                    language = "text/fhirpath"
                    expression = "Patient.id"
                  }
                )
              )
          }
        )
      }

    coEvery { registerRepository.fhirEngine.loadResource<Questionnaire>("444") } returns
      questionnaire

    coEvery {
      registerRepository.fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = "${ResourceType.Patient.name}/999" })
        filter(
          QuestionnaireResponse.QUESTIONNAIRE,
          { value = "${ResourceType.Questionnaire.name}/444" }
        )
      }
    } returns listOf()

    val patient = Patient().apply { id = "Patient/999" }
    coEvery { registerRepository.fhirEngine.loadResource<Patient>("999") } returns patient

    val relatedPerson = RelatedPerson().apply { id = "RelatedPerson/888" }
    coEvery {
      registerRepository.fhirEngine.search<RelatedPerson> {
        filter(RelatedPerson.PATIENT, { value = "${ResourceType.Patient.name}/999" })
      }
    } returns listOf(relatedPerson)

    val event =
      ProfileEvent.OverflowMenuClick(
        navController = navController,
        resourceData = resourceData,
        overflowMenuItemConfig = overflowMenuItemConfig,
      )

    runBlocking {
      profileViewModel.onEvent(event)
      delay(100)

      coVerify { registerRepository.fhirEngine.loadResource<Questionnaire>("444") }

      coVerify {
        registerRepository.fhirEngine.search<QuestionnaireResponse> {
          filter(QuestionnaireResponse.SUBJECT, { value = "${ResourceType.Patient.name}/999" })
          filter(
            QuestionnaireResponse.QUESTIONNAIRE,
            { value = "${ResourceType.Questionnaire.name}/444" }
          )
        }
      }

      coVerify { registerRepository.fhirEngine.loadResource<Patient>("999") }

      coVerify {
        registerRepository.fhirEngine.search<RelatedPerson> {
          filter(RelatedPerson.PATIENT, { value = "${ResourceType.Patient.name}/999" })
        }
      }

      val slot = slot<Bundle>()
      verify {
        (context as QuestionnaireHandler).launchQuestionnaire<Any>(
          context = context,
          intentBundle = capture(slot),
          questionnaireConfig = actionConfig.questionnaire,
          actionParams = actionConfig.params
        )
      }

      val capturedIntentBundle = slot.captured
      assertNotNull(capturedIntentBundle.getString(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE))
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testShouldNotLaunchQuestionnaireWhenContextIsNotQuestionnaireHandler() {
    val context = mockk<Context>()
    val navController = NavController(context)
    val resourceData =
      ResourceData(
        baseResourceId = "Patient/999",
        baseResourceType = ResourceType.Patient,
        computedValuesMap = emptyMap(),
      )
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444"),
        params =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
              dataType = DataType.INTEGER,
              key = "maleCondomPreviousBalance",
              value = "100"
            )
          )
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

    val event =
      ProfileEvent.OverflowMenuClick(
        navController = navController,
        resourceData = resourceData,
        overflowMenuItemConfig = overflowMenuItemConfig,
      )
    profileViewModel.onEvent(event)

    assertFalse { event.navController.context is QuestionnaireHandler }
  }

  @Test
  fun testShouldLaunchQuestionnaireWhenResourceDataIsNull() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val resourceData = null
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444"),
        params =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
              dataType = DataType.INTEGER,
              key = "maleCondomPreviousBalance",
              value = "100"
            )
          )
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

    val questionnaire = Questionnaire().apply { id = "Questionnaire/444" }
    coEvery { registerRepository.fhirEngine.loadResource<Questionnaire>("444") } returns
      questionnaire

    val event =
      ProfileEvent.OverflowMenuClick(
        navController = navController,
        resourceData = resourceData,
        overflowMenuItemConfig = overflowMenuItemConfig,
      )

    runBlocking {
      profileViewModel.onEvent(event)
      delay(100)

      val slot = slot<Bundle>()
      verify {
        (context as QuestionnaireHandler).launchQuestionnaire<Any>(
          context = context,
          intentBundle = capture(slot),
          questionnaireConfig = actionConfig.questionnaire,
          actionParams = actionConfig.params
        )
      }

      val capturedIntentBundle = slot.captured
      assertNull(capturedIntentBundle.getString(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE))
    }
  }

  @Test
  fun testProfileEventOnChangeManagingEntity() {
    profileViewModel.onEvent(
      ProfileEvent.OnChangeManagingEntity(
        ApplicationProvider.getApplicationContext(),
        eligibleManagingEntity =
          EligibleManagingEntity("groupId", "newId", memberInfo = "James Doe"),
        managingEntityConfig =
          ManagingEntityConfig(
            eligibilityCriteriaFhirPathExpression = "Patient.active",
            resourceType = ResourceType.Patient,
            nameFhirPathExpression = "Patient.name.given"
          )
      )
    )
    coVerify { registerRepository.changeManagingEntity(any(), any(), any()) }
  }
}
