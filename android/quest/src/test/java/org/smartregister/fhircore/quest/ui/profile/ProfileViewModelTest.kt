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

package org.smartregister.fhircore.quest.ui.profile

import android.content.Context
import android.os.Bundle
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
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
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItemConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
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

  @get:Rule(order = 1) val coroutineRule = CoroutineTestRule()

  @Inject lateinit var registerRepository: RegisterRepository

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator

  private lateinit var profileViewModel: ProfileViewModel

  private lateinit var resourceData: ResourceData

  private lateinit var expectedBaseResource: Patient

  @Before
  fun setUp() {
    hiltRule.inject()
    expectedBaseResource = Faker.buildPatient()
    resourceData =
      ResourceData(
        baseResourceId = expectedBaseResource.logicalId,
        baseResourceType = expectedBaseResource.resourceType,
        computedValuesMap = emptyMap(),
        listResourceDataMap = emptyMap(),
      )
    registerRepository = mockk()
    coEvery { registerRepository.loadProfileData(any(), any()) } returns resourceData

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
        fhirPathDataExtractor = fhirPathDataExtractor
      )
  }

  @Test
  fun testRetrieveProfileUiState() {
    runBlocking { profileViewModel.retrieveProfileUiState("householdProfile", "sampleId") }

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
  fun testOnEventLaunchQuestionnaireWithQuestionnaireResponseFromDbShouldReturnQuestionnaireResponse() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val resourceData =
      ResourceData(
        baseResourceId = "Patient/999",
        baseResourceType = ResourceType.Patient,
        computedValuesMap = emptyMap(),
        listResourceDataMap = emptyMap(),
      )
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444")
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

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
    profileViewModel.onEvent(event)

    val slot = slot<Bundle>()
    verify {
      (context as QuestionnaireHandler).launchQuestionnaire<Any>(
        context = context,
        intentBundle = capture(slot),
        questionnaireConfig = actionConfig.questionnaire,
        computedValuesMap = resourceData.computedValuesMap
      )
    }

    val capturedIntentBundle = slot.captured
    assertNotNull(capturedIntentBundle.getString(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE))
  }

  @Test
  fun testOnEventLaunchQuestionnaireWhenQuestionnaireResponseFromDbIsNotFoundShouldReturnNullQuestionnaireResponse() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val resourceData =
      ResourceData(
        baseResourceId = "Patient/999",
        baseResourceType = ResourceType.Patient,
        computedValuesMap = emptyMap(),
        listResourceDataMap = emptyMap(),
      )

    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444")
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(visible = "", actions = listOf(actionConfig))

    coEvery {
      registerRepository.fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = "${ResourceType.Patient.name}/999" })
        filter(
          QuestionnaireResponse.QUESTIONNAIRE,
          { value = "${ResourceType.Questionnaire.name}/444" }
        )
      }
    } returns listOf()

    val event =
      ProfileEvent.OverflowMenuClick(
        navController = navController,
        resourceData = resourceData,
        overflowMenuItemConfig = overflowMenuItemConfig,
      )
    profileViewModel.onEvent(event)

    val slot = slot<Bundle>()
    verify {
      (context as QuestionnaireHandler).launchQuestionnaire<Any>(
        context = context,
        intentBundle = capture(slot),
        questionnaireConfig = actionConfig.questionnaire,
        computedValuesMap = resourceData.computedValuesMap
      )
    }

    val capturedIntentBundle = slot.captured
    assertNull(capturedIntentBundle.getString(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE))
  }

  @Test
  fun testOnEventLaunchQuestionnaireWhenContextIsNotQuestionnaireHandler() {
    val context = mockk<Context>()
    val navController = NavController(context)
    val resourceData =
      ResourceData(
        baseResourceId = "Patient/999",
        baseResourceType = ResourceType.Patient,
        computedValuesMap = emptyMap(),
        listResourceDataMap = emptyMap(),
      )
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444")
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
  fun testOnEventLaunchQuestionnaireWhenResourceDataIsNullShouldReturnNullQuestionnaireResponse() {
    val context = mockk<Context>(moreInterfaces = arrayOf(QuestionnaireHandler::class))
    val navController = NavController(context)
    val resourceData = null
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444")
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

    val slot = slot<Bundle>()
    verify {
      (context as QuestionnaireHandler).launchQuestionnaire<Any>(
        context = context,
        intentBundle = capture(slot),
        questionnaireConfig = actionConfig.questionnaire,
        computedValuesMap = event.resourceData?.computedValuesMap
      )
    }

    val capturedIntentBundle = slot.captured
    assertNull(capturedIntentBundle.getString(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE))
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
    coVerify { registerRepository.changeManagingEntity(any(), any()) }
  }
}
