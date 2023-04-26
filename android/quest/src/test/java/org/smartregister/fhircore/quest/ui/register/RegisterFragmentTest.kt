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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SnackbarDuration
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.interpolateActionParamsValue

@HiltAndroidTest
class RegisterFragmentTest : RobolectricTest() {
  @get:Rule var hiltRule = HiltAndroidRule(this)

  private val registerViewModel = RegisterViewModel(mockk(), mockk(), mockk(), mockk(), mockk())

  @OptIn(ExperimentalMaterialApi::class) lateinit var registerFragmentMock: RegisterFragment

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @OptIn(ExperimentalMaterialApi::class) lateinit var registerFragment: RegisterFragment
  @OptIn(ExperimentalMaterialApi::class) private lateinit var mainActivity: AppMainActivity
  @OptIn(ExperimentalMaterialApi::class)
  private val activityController = Robolectric.buildActivity(AppMainActivity::class.java)
  private lateinit var navController: TestNavHostController

  @OptIn(ExperimentalMaterialApi::class)
  @Before
  fun setUp() {
    hiltRule.inject()
    registerFragmentMock = mockk()

    registerFragment =
      RegisterFragment().apply {
        arguments =
          bundleOf(
            Pair(NavigationArg.REGISTER_ID, "householdRegister"),
            Pair(NavigationArg.SCREEN_TITLE, "All HouseHolds"),
            Pair(NavigationArg.TOOL_BAR_HOME_NAVIGATION, ToolBarHomeNavigation.NAVIGATE_BACK),
            Pair(
              NavigationArg.PARAMS,
              interpolateActionParamsValue(
                ActionConfig(trigger = ActionTrigger.ON_CLICK),
                ResourceData(
                  baseResourceId = "patient",
                  baseResourceType = ResourceType.Patient,
                  computedValuesMap = emptyMap()
                )
              )
            )
          )
      }

    activityController.create().resume()
    mainActivity = activityController.get()
    navController =
      TestNavHostController(mainActivity).apply { setGraph(R.navigation.application_nav_graph) }
    Navigation.setViewNavController(mainActivity.navHostFragment.requireView(), navController)
    mainActivity.supportFragmentManager.run {
      commitNow { add(registerFragment, RegisterFragment::class.java.simpleName) }
      executePendingTransactions()
    }
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun testOnStopClearsSearchText() {
    coEvery { registerFragmentMock.onStop() } just runs
    registerFragmentMock.onStop()
    verify { registerFragmentMock.onStop() }
    Assert.assertEquals(registerViewModel.searchText.value, "")
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun testOnSyncState() {
    val syncJobStatus = SyncJobStatus.Finished()
    coEvery { registerFragmentMock.onSync(syncJobStatus) } just runs
    registerFragmentMock.onSync(syncJobStatus = syncJobStatus)
    verify { registerFragmentMock.onSync(syncJobStatus) }
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
  fun `test On changed emits a snack bar message`() {
    val snackBarMessageConfig =
      SnackBarMessageConfig(
        message = "Household member has been added",
        actionLabel = "UNDO",
        duration = SnackbarDuration.Short,
        snackBarActions = emptyList()
      )
    val questionnaireResponse = QuestionnaireResponse()
    val questionnaireConfig = mockk<QuestionnaireConfig>()
    val questionnaireSubmission =
      QuestionnaireSubmission(
        questionnaireConfig = questionnaireConfig,
        questionnaireResponse = questionnaireResponse
      )
    val registerViewModel = mockk<RegisterViewModel>()
    coEvery {
      registerFragmentMock.onChanged(questionnaireSubmission = questionnaireSubmission)
    } just runs
    registerFragmentMock.onChanged(questionnaireSubmission = questionnaireSubmission)
    verify { registerFragmentMock.onChanged(questionnaireSubmission = questionnaireSubmission) }
    coroutineTestRule.launch {
      registerViewModel.emitSnackBarState(snackBarMessageConfig = snackBarMessageConfig)
    }
    coEvery {
      registerViewModel.emitSnackBarState(snackBarMessageConfig = snackBarMessageConfig)
    } just runs
    coVerify { registerViewModel.emitSnackBarState(snackBarMessageConfig = snackBarMessageConfig) }
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
  fun `test On Sync Progress emits progress percentage`() = runTest {
    val downloadProgressSyncStatus: SyncJobStatus =
      SyncJobStatus.InProgress(SyncOperation.DOWNLOAD, 1000, 300)
    val uploadProgressSyncStatus: SyncJobStatus =
      SyncJobStatus.InProgress(SyncOperation.UPLOAD, 100, 85)
    val registerFragment = mockk<RegisterFragment>()

    coEvery { registerFragment.onSync(downloadProgressSyncStatus) } answers { callOriginal() }
    coEvery { registerFragment.onSync(uploadProgressSyncStatus) } answers { callOriginal() }
    coEvery { registerFragment.emitPercentageProgress(any(), any()) } just runs

    registerFragment.onSync(downloadProgressSyncStatus)
    registerFragment.onSync(uploadProgressSyncStatus)

    coVerify(exactly = 1) { registerFragment.emitPercentageProgress(30, false) }
    coVerify(exactly = 1) { registerFragment.emitPercentageProgress(85, true) }
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class)
  fun testHandleRefreshLiveDataCallsRetrieveRegisterUiState() {
    val registerFragmentSpy = spyk(registerFragment)
    val registerViewModel = mockk<RegisterViewModel>()
    every { registerViewModel.retrieveRegisterUiState(any(), any(), any(), any()) } just runs
    every { registerFragmentSpy getProperty "registerViewModel" } returns registerViewModel

    registerFragmentSpy.handleRefreshLiveData()

    verify {
      registerViewModel.retrieveRegisterUiState(
        registerId = "householdRegister",
        screenTitle = "All HouseHolds",
        params = emptyArray(),
        clearCache = true
      )
    }
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun testOnViewCreatedCallsHandleRefreshLiveData() {
    val registerFragmentSpy = spyk(registerFragment)
    registerFragmentSpy.onViewCreated(mockk(), mockk())

    verify { registerFragmentSpy.handleRefreshLiveData() }
  }
}
