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

package org.smartregister.fhircore.quest.ui.main

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.gson.Gson
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.reformatDate
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel.Companion.SYNC_TIMESTAMP_OUTPUT_FORMAT
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission

@HiltAndroidTest
class AppMainViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var gson: Gson

  @Inject lateinit var workManager: WorkManager

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var fhirEngine: FhirEngine

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue val fhirCarePlanGenerator: FhirCarePlanGenerator = mockk()

  private val secureSharedPreference: SecureSharedPreference = mockk()
  private val navController = mockk<NavController>(relaxUnitFun = true)
  private val registerRepository: RegisterRepository = mockk()
  private val application: Context = ApplicationProvider.getApplicationContext()
  private val syncBroadcaster: SyncBroadcaster = mockk(relaxed = true)
  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private lateinit var appMainViewModel: AppMainViewModel

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()

    sharedPreferencesHelper = SharedPreferencesHelper(application, gson)

    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"

    appMainViewModel =
      spyk(
        AppMainViewModel(
          syncBroadcaster = syncBroadcaster,
          secureSharedPreference = secureSharedPreference,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          registerRepository = registerRepository,
          dispatcherProvider = dispatcherProvider,
          workManager = workManager,
          fhirCarePlanGenerator = fhirCarePlanGenerator,
          fhirEngine = fhirEngine,
        ),
      )
    runBlocking { configurationRegistry.loadConfigurations("app/debug", application) }
  }

  @Test
  fun testOnEventSwitchLanguage() {
    val appMainEvent =
      AppMainEvent.SwitchLanguage(
        Language("en", "English"),
        mockkClass(Activity::class, relaxed = true),
      )

    appMainViewModel.onEvent(appMainEvent)

    Assert.assertEquals("en", sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, ""))

    unmockkStatic(Activity::class)
  }

  @Test
  fun testOnEventSyncDataWhenDeviceIsOnline() {
    val appMainEvent = AppMainEvent.SyncData(application)
    appMainViewModel.onEvent(appMainEvent)

    coVerify(exactly = 1) { syncBroadcaster.runOneTimeSync() }
  }

  @Test
  fun testOnEventDoNotSyncDataWhenDeviceIsOffline() {
    mockkStatic(Context::isDeviceOnline)

    val context = mockk<Context>(relaxed = true) { every { isDeviceOnline() } returns false }
    val appMainEvent = AppMainEvent.SyncData(context)
    appMainViewModel.onEvent(appMainEvent)

    coVerify(exactly = 0) { syncBroadcaster.runOneTimeSync() }

    val errorMessage = context.getString(R.string.sync_failed)
    coVerify { context.showToast(errorMessage, Toast.LENGTH_LONG) }
    unmockkStatic(Context::isDeviceOnline)
  }

  @Test
  fun testOnEventUpdateSyncStates() {
    // Simulate sync state Finished
    val syncFinishedTimestamp = OffsetDateTime.now()
    val syncFinishedSyncJobStatus = mockk<CurrentSyncJobStatus.Succeeded>()
    every { syncFinishedSyncJobStatus.timestamp } returns syncFinishedTimestamp

    appMainViewModel.onEvent(
      AppMainEvent.UpdateSyncState(
        syncFinishedSyncJobStatus,
        appMainViewModel.formatLastSyncTimestamp(syncFinishedTimestamp),
      ),
    )
    Assert.assertEquals(
      "${appMainViewModel.formatLastSyncTimestamp(syncFinishedTimestamp)} (0s)",
      appMainViewModel.getSyncTime(),
    )
    coVerify { appMainViewModel.retrieveAppMainUiState() }
  }

  @Test
  fun testOnEventOpenProfile() {
    val resourceConfig = FhirResourceConfig(ResourceConfig(resource = ResourceType.Patient))
    appMainViewModel.onEvent(
      AppMainEvent.OpenProfile(
        navController = navController,
        profileId = "profileId",
        resourceId = "resourceId",
        resourceConfig = resourceConfig,
      ),
    )

    val intSlot = slot<Int>()
    val bundleSlot = slot<Bundle>()
    verify { navController.navigate(capture(intSlot), capture(bundleSlot)) }

    Assert.assertEquals(MainNavigationScreen.Profile.route, intSlot.captured)
    Assert.assertEquals(3, bundleSlot.captured.size())
    Assert.assertEquals("profileId", bundleSlot.captured.getString(NavigationArg.PROFILE_ID))
    Assert.assertEquals("resourceId", bundleSlot.captured.getString(NavigationArg.RESOURCE_ID))
    Assert.assertEquals(
      resourceConfig,
      bundleSlot.captured.getParcelable(NavigationArg.RESOURCE_CONFIG),
    )
  }

  @Test
  fun testOnEventTriggerWorkflow() {
    val action =
      listOf(
        ActionConfig(
          trigger = ActionTrigger.ON_CLICK,
          workflow = ApplicationWorkflow.LAUNCH_SETTINGS.name,
        ),
      )
    val navMenu = NavigationMenuConfig(id = "menuId", display = "Menu Item", actions = action)
    appMainViewModel.onEvent(
      AppMainEvent.TriggerWorkflow(navController = navController, navMenu = navMenu),
    )
    // We have triggered workflow for launching report
    val intSlot = slot<Int>()
    verify { navController.navigate(capture(intSlot)) }
    Assert.assertEquals(MainNavigationScreen.Settings.route, intSlot.captured)
  }

  @Test
  fun testOnEventOpenRegistersBottomSheet() {
    val controller = Robolectric.buildActivity(HiltActivityForTest::class.java).create().resume()
    val activityForTest = controller.get()
    every { navController.context } returns activityForTest
    appMainViewModel.onEvent(
      AppMainEvent.OpenRegistersBottomSheet(
        navController = navController,
        registersList = emptyList(),
      ),
    )

    // Assert fragment that was launched is RegisterBottomSheetFragment
    activityForTest.supportFragmentManager.executePendingTransactions()
    val fragments = activityForTest.supportFragmentManager.fragments
    Assert.assertEquals(1, fragments.size)
    Assert.assertTrue(fragments.first() is RegisterBottomSheetFragment)
    // Destroy the activity
    controller.destroy()
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testOnQuestionnaireSubmissionShouldSetTaskStatusCompletedWhenStatusIsNull() = runTest {
    coEvery { fhirCarePlanGenerator.updateTaskDetailsByResourceId(any(), any()) } just runs

    val questionnaireSubmission =
      QuestionnaireSubmission(
        questionnaireConfig = QuestionnaireConfig(taskId = "Task/12345", id = "questionnaireId"),
        questionnaireResponse = QuestionnaireResponse(),
      )
    appMainViewModel.onQuestionnaireSubmission(questionnaireSubmission)

    coVerify {
      fhirCarePlanGenerator.updateTaskDetailsByResourceId("12345", Task.TaskStatus.COMPLETED)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testOnSubmitQuestionnaireShouldSetTaskStatusToInProgressWhenQuestionnaireIsInProgress() =
    runTest {
      coEvery { fhirCarePlanGenerator.updateTaskDetailsByResourceId(any(), any()) } just runs

      val questionnaireSubmission =
        QuestionnaireSubmission(
          questionnaireConfig = QuestionnaireConfig(taskId = "Task/12345", id = "questionnaireId"),
          questionnaireResponse =
            QuestionnaireResponse().apply {
              status = QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS
            },
        )
      appMainViewModel.onQuestionnaireSubmission(questionnaireSubmission)

      coVerify {
        fhirCarePlanGenerator.updateTaskDetailsByResourceId("12345", Task.TaskStatus.INPROGRESS)
      }
    }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testOnSubmitQuestionnaireShouldSetTaskStatusToCompletedWhenQuestionnaireIsCompleted() =
    runTest {
      coEvery { fhirCarePlanGenerator.updateTaskDetailsByResourceId(any(), any()) } just runs
      val questionnaireSubmission =
        QuestionnaireSubmission(
          questionnaireConfig = QuestionnaireConfig(taskId = "Task/12345", id = "questionnaireId"),
          questionnaireResponse =
            QuestionnaireResponse().apply {
              status = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
            },
        )
      appMainViewModel.onQuestionnaireSubmission(questionnaireSubmission)

      coVerify {
        fhirCarePlanGenerator.updateTaskDetailsByResourceId("12345", Task.TaskStatus.COMPLETED)
      }
    }

  @Test
  fun testGetSyncTimeWithMillisTimestamp() {
    // Mocking a timestamp in milliseconds
    val mockTimestamp = OffsetDateTime.now().toInstant().toEpochMilli().toString()
    every { appMainViewModel.retrieveLastSyncTimestamp() } returns mockTimestamp
    every { appMainViewModel.applicationConfiguration.dateFormat } returns "yyyy-MM-dd HH:mm:ss"
    val syncTime = appMainViewModel.getSyncTime()
    val expectedFormattedDate =
      "${formatDate(mockTimestamp.toLong(), appMainViewModel.applicationConfiguration.dateFormat)} (0s)"
    Assert.assertEquals(expectedFormattedDate, syncTime)
  }

  @Test
  fun testGetSyncTimeWithFormattedDate() {
    val mockFormattedDate = "2023-10-10 10:10:10"
    every { appMainViewModel.retrieveLastSyncTimestamp() } returns mockFormattedDate
    every { appMainViewModel.applicationConfiguration.dateFormat } returns "yyyy-MM-dd"
    val syncTime = appMainViewModel.getSyncTime()
    val expectedReformattedDate =
      reformatDate(
        inputDateString = mockFormattedDate,
        currentFormat = SYNC_TIMESTAMP_OUTPUT_FORMAT,
        desiredFormat = appMainViewModel.applicationConfiguration.dateFormat,
      )
    Assert.assertEquals("$expectedReformattedDate (0s)", syncTime)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testOnSubmitQuestionnaireShouldNeverUpdateTaskStatusWhenQuestionnaireTaskIdIsNull() =
    runTest {
      coEvery { fhirCarePlanGenerator.updateTaskDetailsByResourceId(any(), any()) } just runs

      appMainViewModel.onQuestionnaireSubmission(
        QuestionnaireSubmission(
          questionnaireResponse = QuestionnaireResponse(),
          questionnaireConfig = QuestionnaireConfig(taskId = null, id = "qId"),
        ),
      )

      coVerify(inverse = true) { fhirCarePlanGenerator.updateTaskDetailsByResourceId(any(), any()) }
    }
}
