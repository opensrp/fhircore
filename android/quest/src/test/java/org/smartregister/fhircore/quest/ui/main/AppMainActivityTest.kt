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

package org.smartregister.fhircore.quest.ui.main

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.navigation.fragment.NavHostFragment
import androidx.work.WorkManager
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
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity

@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class AppMainActivityTest : ActivityRobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue val fhirCarePlanGenerator: FhirCarePlanGenerator = mockk()

  @BindValue lateinit var appMainViewModel: AppMainViewModel

  val sharedPreferencesHelper: SharedPreferencesHelper = mockk()

  val secureSharedPreference: SecureSharedPreference = mockk()

  lateinit var appMainActivity: AppMainActivity

  val workManager: WorkManager = mockk()

  val registerRepository: RegisterRepository = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()

    every { sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, "") } returns "AppId.Test"
    every {
      sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, Locale.ENGLISH.toLanguageTag())
    } returns ""
    every { secureSharedPreference.retrieveSessionUsername() } returns "testUser"
    every {
      sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)
    } returns ""
    every {
      sharedPreferencesHelper.read(SharedPreferenceKey.FHIR_TASK_EXPIRE_WORKER_VERSION.name, 0)
    } returns 1
    every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } returns mockk()
    coEvery { registerRepository.countRegisterData(any()) } returns 2

    appMainViewModel =
      spyk(
        AppMainViewModel(
          accountAuthenticator = mockk(),
          syncBroadcaster = mockk(),
          secureSharedPreference = secureSharedPreference,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          registerRepository = registerRepository,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
          workManager = workManager,
          fhirCarePlanGenerator = fhirCarePlanGenerator
        )
      )

    appMainActivity =
      spyk(Robolectric.buildActivity(AppMainActivity::class.java).create().resume().get())
  }

  @Test
  fun testActivityIsStartedCorrectly() {
    Assert.assertNotNull(appMainActivity)
    val fragments = appMainActivity.supportFragmentManager.fragments
    Assert.assertEquals(1, fragments.size)
    Assert.assertTrue(fragments.first() is NavHostFragment)
  }

  override fun getActivity(): Activity {
    return appMainActivity
  }

  /* @Test
  fun testOnSyncWithSyncStateStarted() {
    appMainActivity.onSync(State.Started)
    Assert.assertNotNull(ShadowToast.getLatestToast())
    Assert.assertTrue(ShadowToast.getTextOfLatestToast().contains("Syncing", ignoreCase = true))
  }*/

  /*@Test
  fun testOnSyncWithSyncStateInProgress() {
    appMainActivity.onSync(State.InProgress(resourceType = null))
    Assert.assertTrue(
      appMainActivity.appMainViewModel.appMainUiState.value.lastSyncTime.contains(
        "Sync in progress",
        ignoreCase = true
      )
    )
  }*/

  /*@Test
  fun testOnSyncWithSyncStateGlitch() {
    val viewModel = appMainActivity.appMainViewModel
    viewModel.sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      "2022-05-19"
    )
    appMainActivity.onSync(State.Glitch(exceptions = emptyList()))
    Assert.assertNotNull(viewModel.retrieveLastSyncTimestamp())
    Assert.assertTrue(
      viewModel.appMainUiState.value.lastSyncTime.contains(
        viewModel.retrieveLastSyncTimestamp()!!,
        ignoreCase = true
      )
    )
  }*/

  /*@Test
  fun testOnSyncWithSyncStateFailedRetrievesTimestamp() {
    val viewModel = appMainActivity.appMainViewModel
    viewModel.sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      "2022-05-19"
    )
    appMainActivity.onSync(State.Failed(result = Result.Error(emptyList())))
    Assert.assertNotNull(ShadowToast.getLatestToast())
    Assert.assertTrue(
      ShadowToast.getTextOfLatestToast()
        .contains("Sync failed. Check internet connection or try again later.", ignoreCase = true)
    )
    Assert.assertNotNull(viewModel.retrieveLastSyncTimestamp())
    Assert.assertEquals(
      viewModel.appMainUiState.value.lastSyncTime,
      viewModel.retrieveLastSyncTimestamp()
    )
  }*/

  /*@Test
  fun testOnSyncWithSyncStateFailedWhenTimestampIsNull() {
    val viewModel = appMainActivity.appMainViewModel
    appMainActivity.onSync(State.Failed(result = Result.Error(emptyList())))
    Assert.assertNotNull(ShadowToast.getLatestToast())
    Assert.assertTrue(
      ShadowToast.getTextOfLatestToast()
        .contains("Sync failed. Check internet connection or try again later.", ignoreCase = true)
    )
    Assert.assertEquals(viewModel.appMainUiState.value.lastSyncTime, "")
  }

  @Test
  fun testOnSyncWithSyncStateFinished() {
    val viewModel = appMainActivity.appMainViewModel
    val stateFinished = State.Finished(result = Result.Success())
    appMainActivity.onSync(stateFinished)
    Assert.assertNotNull(ShadowToast.getLatestToast())
    Assert.assertTrue(
      ShadowToast.getTextOfLatestToast().contains("Sync complete", ignoreCase = true)
    )
    Assert.assertEquals(
      viewModel.formatLastSyncTimestamp(timestamp = stateFinished.result.timestamp),
      viewModel.retrieveLastSyncTimestamp()
    )
  }*/

  @Test
  fun `handleTaskActivityResult should set task status in-progress when response status is in-progress`() =
      runTest {
    coEvery { fhirCarePlanGenerator.transitionTaskTo(any(), any()) } just runs

    appMainActivity.onSubmitQuestionnaire(
      ActivityResult(
        -1,
        Intent().apply {
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_RESPONSE,
            QuestionnaireResponse().apply {
              status = QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS
            }
          )
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_CONFIG,
            QuestionnaireConfig(taskId = "Task/12345", id = "questionnaireId")
          )
        }
      )
    )

    coVerify { fhirCarePlanGenerator.transitionTaskTo(any(), any()) }
  }

  @Test
  fun `handleTaskActivityResult should set task status completed when response status is completed`() =
      runTest {
    coEvery { fhirCarePlanGenerator.transitionTaskTo(any(), any()) } just runs

    appMainActivity.onSubmitQuestionnaire(
      ActivityResult(
        -1,
        Intent().apply {
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_RESPONSE,
            QuestionnaireResponse().apply {
              status = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
            }
          )
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_CONFIG,
            QuestionnaireConfig(taskId = "Task/12345", id = "questionnaireId")
          )
        }
      )
    )

    coVerify { fhirCarePlanGenerator.transitionTaskTo(any(), any()) }
  }

  @Test
  fun `handleTaskActivityResult should set task status completed when response status is null`() =
      runTest {
    coEvery { fhirCarePlanGenerator.transitionTaskTo(any(), any()) } just runs

    appMainActivity.onSubmitQuestionnaire(
      ActivityResult(
        AppCompatActivity.RESULT_OK,
        Intent().apply {
          putExtra(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE, QuestionnaireResponse())
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_CONFIG,
            QuestionnaireConfig(taskId = "Task/12345", id = "questionnaireId")
          )
        }
      )
    )

    coVerify { fhirCarePlanGenerator.transitionTaskTo(any(), any()) }
  }

  @Test
  fun `handleTaskActivityResult should not set task status when response does not exists`() =
      runTest {
    coEvery { fhirCarePlanGenerator.transitionTaskTo(any(), any()) } just runs

    appMainActivity.onSubmitQuestionnaire(ActivityResult(-1, Intent()))

    coVerify(inverse = true) { fhirCarePlanGenerator.transitionTaskTo(any(), any()) }
  }

  /*  @Test
  fun `onSync with StateFailed and auth error calls appMainViewModel with RefreshAuthToken event`() {
    val exception: HttpException = mockk()
    val stateFailed =
      SyncJobStatus.Failed(listOf(ResourceSyncException(ResourceType.Questionnaire, exception)))
    every { exception.code() } returns 401
    every { exception.message } returns "Unauthorized"

    appMainActivity.onSync(stateFailed)

    verify { appMainViewModel.onEvent(AppMainEvent.RefreshAuthToken(appMainActivity)) }
  }
  */
}
