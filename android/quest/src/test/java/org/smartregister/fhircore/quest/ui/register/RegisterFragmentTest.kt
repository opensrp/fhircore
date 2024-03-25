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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SnackbarDuration
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Patient
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
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.interpolateActionParamsValue

@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class RegisterFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var eventBus: EventBus

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue lateinit var registerViewModel: RegisterViewModel

  private lateinit var navController: TestNavHostController
  private lateinit var registerFragment: RegisterFragment
  private lateinit var mainActivity: AppMainActivity
  private lateinit var registerFragmentMock: RegisterFragment
  private val activityController = Robolectric.buildActivity(AppMainActivity::class.java)

  @Before
  fun setUp() {
    hiltRule.inject()
    registerViewModel =
      spyk(
        RegisterViewModel(
          registerRepository = mockk(relaxed = true),
          configurationRegistry = configurationRegistry,
          sharedPreferencesHelper = Faker.buildSharedPreferencesHelper(),
          dispatcherProvider = dispatcherProvider,
          resourceDataRulesExecutor = mockk(),
        ),
      )
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
                  computedValuesMap = emptyMap(),
                ),
              ),
            ),
          )
      }

    activityController.create().resume()
    mainActivity = activityController.get()
    navController =
      TestNavHostController(mainActivity).apply {
        setGraph(org.smartregister.fhircore.quest.R.navigation.application_nav_graph)
      }
    Navigation.setViewNavController(mainActivity.navHostFragment.requireView(), navController)
    mainActivity.supportFragmentManager.run {
      commitNow { add(registerFragment, RegisterFragment::class.java.simpleName) }
      executePendingTransactions()
    }
  }

  @Test
  fun testOnStopClearsSearchText() {
    coEvery { registerFragmentMock.onStop() } just runs
    registerFragmentMock.onStop()
    verify { registerFragmentMock.onStop() }
    Assert.assertEquals(registerViewModel.searchText.value, "")
  }

  @Test
  fun testOnSyncState() {
    val syncJobStatus = CurrentSyncJobStatus.Succeeded(OffsetDateTime.now())
    coEvery { registerFragmentMock.onSync(syncJobStatus) } just runs
    registerFragmentMock.onSync(syncJobStatus = syncJobStatus)
    verify { registerFragmentMock.onSync(syncJobStatus) }
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `test On changed emits a snack bar message`() = runTest {
    val snackBarMessageConfig =
      SnackBarMessageConfig(
        message = "Household member has been added",
        actionLabel = "UNDO",
        duration = SnackbarDuration.Short,
        snackBarActions = emptyList(),
      )
    val registerViewModel = mockk<RegisterViewModel>(relaxUnitFun = true)
    registerViewModel.emitSnackBarState(snackBarMessageConfig = snackBarMessageConfig)
    coEvery {
      registerViewModel.emitSnackBarState(snackBarMessageConfig = snackBarMessageConfig)
    } just runs
    coVerify { registerViewModel.emitSnackBarState(snackBarMessageConfig = snackBarMessageConfig) }
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
  fun `test On Sync Progress emits progress percentage`() = runTest {
    val downloadProgressSyncStatus =
      CurrentSyncJobStatus.Running(SyncJobStatus.InProgress(SyncOperation.DOWNLOAD, 1000, 300))
    val uploadProgressSyncStatus: CurrentSyncJobStatus.Running =
      CurrentSyncJobStatus.Running(SyncJobStatus.InProgress(SyncOperation.UPLOAD, 100, 85))

    val registerFragment = spyk(registerFragment)

    coEvery { registerFragment.onSync(downloadProgressSyncStatus) } answers { callOriginal() }
    coEvery { registerFragment.onSync(uploadProgressSyncStatus) } answers { callOriginal() }

    registerFragment.onSync(downloadProgressSyncStatus)
    registerFragment.onSync(uploadProgressSyncStatus)

    coVerify(exactly = 1) { registerViewModel.emitPercentageProgressState(30, false) }

    coVerify(exactly = 1) { registerViewModel.emitPercentageProgressState(85, true) }

    coVerify(exactly = 1) {
      registerFragment.emitPercentageProgress(
        downloadProgressSyncStatus.inProgressSyncJob as SyncJobStatus.InProgress,
        false,
      )
    }
    coVerify(exactly = 1) {
      registerFragment.emitPercentageProgress(
        uploadProgressSyncStatus.inProgressSyncJob as SyncJobStatus.InProgress,
        true,
      )
    }
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
  fun `test On Sync Progress emits correct download progress percentage after a glitch`() =
    runTest {
      val downloadProgressSyncStatus: SyncJobStatus.InProgress =
        SyncJobStatus.InProgress(SyncOperation.DOWNLOAD, 1000, 300)
      val downloadProgressSyncStatusAfterGlitchReset: SyncJobStatus.InProgress =
        SyncJobStatus.InProgress(SyncOperation.DOWNLOAD, 200, 100)

      val registerFragment = spyk(registerFragment)

      registerFragment.onSync(CurrentSyncJobStatus.Running(downloadProgressSyncStatus))
      registerFragment.onSync(
        CurrentSyncJobStatus.Running(downloadProgressSyncStatusAfterGlitchReset),
      )

      coVerify(exactly = 1) {
        registerFragment.emitPercentageProgress(downloadProgressSyncStatus, false)
      }

      coVerify(exactly = 1) {
        registerFragment.emitPercentageProgress(downloadProgressSyncStatusAfterGlitchReset, false)
      }

      coVerify(exactly = 1) { registerViewModel.emitPercentageProgressState(30, false) }
      coVerify(exactly = 1) { registerViewModel.emitPercentageProgressState(90, false) }
    }

  @Test
  fun testHandleQuestionnaireSubmissionCallsRegisterViewModelPaginateRegisterDataAndEmitSnackBarState() {
    val snackBarMessageConfig = SnackBarMessageConfig(message = "Family member added")
    val questionnaireConfig =
      QuestionnaireConfig(id = "add-member", snackBarMessage = snackBarMessageConfig)
    val questionnaireResponse = QuestionnaireResponse().apply { id = "1234" }
    val questionnaireSubmission =
      QuestionnaireSubmission(
        questionnaireConfig = questionnaireConfig,
        questionnaireResponse = questionnaireResponse,
      )
    val registerFragmentSpy = spyk(registerFragment)

    coEvery { registerViewModel.emitSnackBarState(any()) } just runs
    runBlocking { registerFragmentSpy.handleQuestionnaireSubmission(questionnaireSubmission) }
    coVerify { registerFragmentSpy.refreshRegisterData() }
    coVerify { registerViewModel.emitSnackBarState(snackBarMessageConfig) }
  }

  @Test
  fun testHandleQuestionnaireSubmissionUpdatesFilterResourceConfig() {
    val questionnaireConfig =
      QuestionnaireConfig(id = "add-member", saveQuestionnaireResponse = false)
    val questionnaireResponse = QuestionnaireResponse().apply { id = "1234" }
    val questionnaireSubmission =
      QuestionnaireSubmission(
        questionnaireConfig = questionnaireConfig,
        questionnaireResponse = questionnaireResponse,
      )
    val registerFragmentSpy = spyk(registerFragment)

    runBlocking { registerFragmentSpy.handleQuestionnaireSubmission(questionnaireSubmission) }

    // Refresh data is called with QuestionnaireResponse param
    val submissionSlot = slot<QuestionnaireResponse>()
    coVerify { registerFragmentSpy.refreshRegisterData(capture(submissionSlot)) }
    Assert.assertEquals(questionnaireResponse.id, submissionSlot.captured.id)
  }

  @Test
  fun testOnSyncWithFailedJobStatusNonAuthErrorRendersSyncFailedMessage() {
    val syncJobStatus = CurrentSyncJobStatus.Failed(OffsetDateTime.now())
    val registerFragmentSpy = spyk(registerFragment)
    registerFragmentSpy.onSync(syncJobStatus = syncJobStatus)
    verify { registerFragmentSpy.onSync(syncJobStatus) }
    verify {
      registerFragmentSpy.getString(
        org.smartregister.fhircore.engine.R.string.sync_completed_with_errors,
      )
    }
  }

  @Test
  fun testOnSyncWithFailedJobStatusNonAuthErrorNullExceptionsRendersSyncFailedMessage() {
    val syncJobStatus: CurrentSyncJobStatus.Failed = mockk()

    val registerFragmentSpy = spyk(registerFragment)
    registerFragmentSpy.onSync(syncJobStatus = syncJobStatus)
    verify { registerFragmentSpy.onSync(syncJobStatus) }
    verify {
      registerFragmentSpy.getString(
        org.smartregister.fhircore.engine.R.string.sync_completed_with_errors,
      )
    }
  }
}
