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

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission

@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class ProfileFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue val registerRepository: RegisterRepository = mockk(relaxUnitFun = true, relaxed = true)

  @BindValue lateinit var profileViewModel: ProfileViewModel

  private val activityController = Robolectric.buildActivity(AppMainActivity::class.java)

  private lateinit var navController: TestNavHostController

  private val patient = Faker.buildPatient()

  private val resourceConfig = mockk<FhirResourceConfig>()

  private lateinit var mainActivity: AppMainActivity

  lateinit var profileFragment: ProfileFragment

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    profileViewModel =
      spyk(
        ProfileViewModel(
          registerRepository,
          configurationRegistry = configurationRegistry,
          dispatcherProvider = dispatcherProvider,
          mockk(),
          mockk(),
        ),
      )
    profileFragment =
      ProfileFragment().apply {
        arguments =
          bundleOf(
            NavigationArg.PROFILE_ID to "defaultProfile",
            NavigationArg.RESOURCE_ID to patient.id,
            NavigationArg.RESOURCE_CONFIG to resourceConfig,
            NavigationArg.PARAMS to
              arrayOf(
                ActionParameter(
                  key = "anyId",
                  paramType = ActionParameterType.PARAMDATA,
                  value = "anyValue",
                ),
              ),
          )
      }
    activityController.create().resume()
    mainActivity = activityController.get()
    navController =
      TestNavHostController(mainActivity).apply { setGraph(R.navigation.application_nav_graph) }

    // Simulate the returned value of loadProfile
    coEvery { registerRepository.loadProfileData(any(), any(), paramsMap = emptyMap()) } returns
      RepositoryResourceData(resource = Faker.buildPatient())
    mainActivity.supportFragmentManager.run {
      commitNow { add(profileFragment, ProfileFragment::class.java.simpleName) }
      executePendingTransactions()
    }
  }

  @Test
  fun testProfileFragmentCreation() {
    Assert.assertTrue(profileFragment.view is ComposeView)
    activityController.destroy()
  }

  @Test
  fun testHandleQuestionnaireSubmissionCallsProfileViewModelRetrieveProfileUiStateAndEmitSnackBarState() {
    val snackBarMessageConfig = SnackBarMessageConfig(message = "Family member added")
    val questionnaireConfig =
      QuestionnaireConfig(id = "add-member", snackBarMessage = snackBarMessageConfig)
    val questionnaireResponse = QuestionnaireResponse().apply { id = "1234" }
    val questionnaireSubmission =
      QuestionnaireSubmission(
        questionnaireConfig = questionnaireConfig,
        questionnaireResponse = questionnaireResponse,
      )

    every { profileViewModel.retrieveProfileUiState(any(), any(), any(), any()) } just runs
    coEvery { profileViewModel.emitSnackBarState(any()) } just runs

    runBlocking { profileFragment.handleQuestionnaireSubmission(questionnaireSubmission) }

    coVerify {
      profileViewModel.retrieveProfileUiState(
        context = ApplicationProvider.getApplicationContext(),
        profileId = "defaultProfile",
        resourceId = "sampleId",
        fhirResourceConfig = any(),
        paramsList = any(),
      )
    }
    coVerify { profileViewModel.emitSnackBarState(snackBarMessageConfig) }
  }

  @Test
  fun testReloadingProfileUIStateWhenChangeManagingEntityCompletes() {
    coEvery { profileViewModel.retrieveProfileUiState(any(), any(), any(), any()) } just runs

    coEvery { registerRepository.changeManagingEntity(any(), any(), any()) } just runs

    coEvery { profileViewModel.emitSnackBarState(any()) } just runs

    profileViewModel.onEvent(
      ProfileEvent.OnChangeManagingEntity(
        ApplicationProvider.getApplicationContext(),
        eligibleManagingEntity =
          EligibleManagingEntity("groupId", "newId", memberInfo = "James Doe"),
        managingEntityConfig =
          ManagingEntityConfig(
            eligibilityCriteriaFhirPathExpression = "Patient.active",
            resourceType = ResourceType.Patient,
            nameFhirPathExpression = "Patient.name.given",
          ),
      ),
    )

    coVerify { registerRepository.changeManagingEntity(any(), any(), any()) }

    coVerify {
      profileViewModel.retrieveProfileUiState(
        context = ApplicationProvider.getApplicationContext(),
        profileId = "defaultProfile",
        resourceId = "sampleId",
        fhirResourceConfig = any(),
        paramsList = any(),
      )
    }
  }
}
