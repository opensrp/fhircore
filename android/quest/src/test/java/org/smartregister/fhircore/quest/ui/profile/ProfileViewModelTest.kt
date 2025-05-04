/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.verifyAll
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.ContentCache
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItemConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.BLACK_COLOR_HEX_CODE
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.profile.bottomSheet.ProfileBottomSheetFragment
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

@HiltAndroidTest
class ProfileViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var rulesExecutor: RulesExecutor

  @Inject lateinit var configRulesExecutor: ConfigRulesExecutor

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var parser: IParser

  @Inject lateinit var contentCache: ContentCache

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var resourceData: ResourceData
  private lateinit var expectedBaseResource: Patient
  private lateinit var registerRepository: RegisterRepository
  val mockProfileViewModel = mockk<ProfileViewModel>()

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()
    expectedBaseResource = Faker.buildPatient()
    resourceData =
      ResourceData(
        baseResourceId = expectedBaseResource.logicalId,
        baseResourceType = expectedBaseResource.resourceType,
        computedValuesMap = emptyMap(),
      )
    registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = mockk(),
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          fhirPathDataExtractor = mockk(),
          parser = parser,
          context = ApplicationProvider.getApplicationContext(),
          dispatcherProvider = dispatcherProvider,
          contentCache = contentCache,
          configRulesExecutor = configRulesExecutor,
        ),
      )
    coEvery {
      registerRepository.loadProfileData(
        profileId = any(),
        resourceId = any(),
        paramsMap = emptyMap(),
      )
    } returns RepositoryResourceData(resource = Faker.buildPatient())

    runBlocking {
      configurationRegistry.loadConfigurations(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        appId = APP_DEBUG,
      ) {}
    }

    profileViewModel =
      ProfileViewModel(
        registerRepository = registerRepository,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = dispatcherProvider,
        fhirPathDataExtractor = fhirPathDataExtractor,
        rulesExecutor = rulesExecutor,
      )
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveProfileUiState() = runTest {
    profileViewModel.retrieveProfileUiState(
      context = ApplicationProvider.getApplicationContext(),
      profileId = "householdProfile",
      resourceId = "sampleId",
      paramsList = emptyArray(),
    )

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
  fun testProfileEventOnChangeManagingEntity() = runTest {
    coEvery { registerRepository.changeManagingEntity(any(), any(), any()) } just runs
    val managingEntityConfig =
      ManagingEntityConfig(
        eligibilityCriteriaFhirPathExpression = "Patient.active",
        resourceType = ResourceType.Patient,
        nameFhirPathExpression = "Patient.name.given",
      )
    profileViewModel.onEvent(
      ProfileEvent.OnChangeManagingEntity(
        ApplicationProvider.getApplicationContext(),
        eligibleManagingEntity =
          EligibleManagingEntity("groupId", "newId", memberInfo = "James Doe"),
        managingEntityConfig = managingEntityConfig,
      ),
    )
    coVerify { registerRepository.changeManagingEntity("newId", "groupId", managingEntityConfig) }
  }

  @Test
  fun testOverflowMenuClickChangeManagingEntity() {
    val mockEvent = mockk<ProfileEvent.OverflowMenuClick>()
    val mockActionConfig = mockk<ActionConfig>()
    val listActionConfig = mockk<List<ActionConfig>>()
    val mockWorkflow = ApplicationWorkflow.CHANGE_MANAGING_ENTITY
    val mockManagingEntity = mockk<ManagingEntityConfig>()

    every { mockEvent.overflowMenuItemConfig?.actions } returns listActionConfig
    every { mockActionConfig.managingEntity } returns mockManagingEntity
    every { mockActionConfig.interpolate(any()) } returns mockActionConfig
    every { mockActionConfig.workflow } returns mockWorkflow.name

    val resourceData =
      ResourceData("baseResourceId", ResourceType.MeasureReport, emptyMap(), emptyMap())
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(
        id = 1,
        title = "myFlowMenu",
        confirmAction = false,
        icon = null,
        titleColor = BLACK_COLOR_HEX_CODE,
        backgroundColor = null,
        visible = "true",
        showSeparator = false,
        enabled = "true",
        actions = emptyList(),
      )
    val navController = mockk<NavController>()
    val event = ProfileEvent.OverflowMenuClick(navController, resourceData, overflowMenuItemConfig)
    val managingEntity =
      ManagingEntityConfig(
        eligibilityCriteriaFhirPathExpression = "Patient.active",
        resourceType = ResourceType.Patient,
        nameFhirPathExpression = "Patient.name.given",
      )
    every { mockProfileViewModel.changeManagingEntity(event, managingEntity) } just runs
    every { mockActionConfig.interpolate(any()) } returns mockActionConfig
    mockActionConfig.interpolate(emptyMap())
    every { listActionConfig.handleClickEvent(navController, resourceData) } just runs
    profileViewModel.onEvent(event)
    every { mockProfileViewModel.onEvent(any()) } just runs

    verifyAll {
      mockActionConfig.interpolate(any())
      profileViewModel.onEvent(event)
    }
  }

  @Test
  fun testThatManagingEntityProfileBottomSheetIsShownOnActionTriggered() = runTest {
    val fragmentManager = mockk<FragmentManager>()
    val fragmentManagerTransaction = mockk<FragmentTransaction>()
    val activity =
      mockk<AppCompatActivity> {
        every { supportFragmentManager } returns fragmentManager
        every { supportFragmentManager.beginTransaction() } returns fragmentManagerTransaction
      }
    val navController = mockk<NavController> { every { context } returns activity }
    mockkConstructor(ProfileBottomSheetFragment::class)
    every {
      anyConstructed<ProfileBottomSheetFragment>().show(any<FragmentManager>(), any<String>())
    } just runs

    val memberPatient =
      Patient().apply {
        id = "entity1"
        active = true
        addName(
          HumanName().apply { given = listOf(StringType("member 1")) },
        )
      }
    val managingEntityResource =
      mockk<Group.GroupMemberComponent>() {
        every { id } returns "entity1"
        every { entity } returns Reference().apply { reference = "Patient/entity1" }
      }

    val group =
      Group().apply {
        managingEntity = managingEntity.apply { reference = "patient/1424251" }
        member = listOf(managingEntityResource)
      }

    val viewModel =
      ProfileViewModel(
        registerRepository,
        configurationRegistry,
        dispatcherProvider,
        fhirPathDataExtractor,
        rulesExecutor,
      )

    val managingEntityConfig =
      ManagingEntityConfig(
        nameFhirPathExpression = "name",
        eligibilityCriteriaFhirPathExpression = "active",
        resourceType = ResourceType.Patient,
        dialogTitle = "Change Managing Entity",
        dialogWarningMessage = "Warning",
        dialogContentMessage = "Select a new managing entity",
        noMembersErrorMessage = "No members found",
        managingEntityReassignedMessage = "Managing entity reassigned",
      )

    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.CHANGE_MANAGING_ENTITY.name,
        managingEntity = managingEntityConfig,
      )
    val overflowMenuItemConfig =
      OverflowMenuItemConfig(
        id = 1,
        title = "open profile bottom sheet",
        confirmAction = false,
        icon = null,
        titleColor = BLACK_COLOR_HEX_CODE,
        backgroundColor = null,
        visible = "true",
        showSeparator = false,
        enabled = "true",
        actions = listOf(actionConfig),
      )

    coEvery { registerRepository.loadResource<Group>("group1") } returns group
    coEvery { registerRepository.loadResource("entity1", ResourceType.Patient) } returns
      memberPatient

    val groupResourceData =
      ResourceData(
        baseResourceId = "group1",
        baseResourceType = ResourceType.Group,
        computedValuesMap = emptyMap(),
      )

    viewModel.onEvent(
      ProfileEvent.OverflowMenuClick(
        navController,
        groupResourceData,
        overflowMenuItemConfig,
      ),
    )
    verifyAll {
      navController.context
      anyConstructed<ProfileBottomSheetFragment>()
        .show(any<FragmentManager>(), ProfileBottomSheetFragment.TAG)
    }

    unmockkConstructor(ProfileBottomSheetFragment::class)
  }
}
