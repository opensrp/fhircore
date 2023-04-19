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

package org.smartregister.fhircore.quest.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.NavHostFragment
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission

@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class AppMainActivityTest : ActivityRobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue val fhirCarePlanGenerator: FhirCarePlanGenerator = mockk()

  lateinit var appMainActivity: AppMainActivity

  @Before
  fun setUp() {
    hiltRule.inject()
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

  @Test
  fun testOnSyncWithSyncStateInProgress() {
    appMainActivity.onSync(SyncJobStatus.InProgress(SyncOperation.DOWNLOAD))
    Assert.assertTrue(
      appMainActivity.appMainViewModel.appMainUiState.value.lastSyncTime.contains(
        "Sync in progress",
        ignoreCase = true
      )
    )
  }

  @Test
  fun testOnSyncWithSyncStateGlitch() {
    val viewModel = appMainActivity.appMainViewModel
    viewModel.sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      "2022-05-19"
    )
    appMainActivity.onSync(SyncJobStatus.Glitch(exceptions = emptyList()))
    Assert.assertNotNull(viewModel.retrieveLastSyncTimestamp())
    Assert.assertTrue(
      viewModel.appMainUiState.value.lastSyncTime.contains(
        viewModel.retrieveLastSyncTimestamp()!!,
        ignoreCase = true
      )
    )
  }

  @Test
  fun testOnSyncWithSyncStateFailedRetrievesTimestamp() {
    val viewModel = appMainActivity.appMainViewModel
    viewModel.sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      "2022-05-19"
    )
    appMainActivity.onSync(SyncJobStatus.Failed(listOf()))

    Assert.assertNotNull(viewModel.retrieveLastSyncTimestamp())
    Assert.assertEquals(
      viewModel.appMainUiState.value.lastSyncTime,
      viewModel.retrieveLastSyncTimestamp()
    )
  }

  @Test
  fun testOnSyncWithSyncStateFailedWhenTimestampIsNull() {
    val viewModel = appMainActivity.appMainViewModel
    appMainActivity.onSync(SyncJobStatus.Failed(listOf()))
    Assert.assertEquals(viewModel.appMainUiState.value.lastSyncTime, "")
  }

  @Test
  fun testOnSyncWithSyncStateFinished() {
    val viewModel = appMainActivity.appMainViewModel
    val stateFinished = SyncJobStatus.Finished()
    appMainActivity.onSync(stateFinished)

    Assert.assertEquals(
      viewModel.formatLastSyncTimestamp(timestamp = stateFinished.timestamp),
      viewModel.retrieveLastSyncTimestamp()
    )
  }

  @Test
  fun testOnSubmitQuestionnaireShouldUpdateLiveData() {
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

    val questionnaireSubmission =
      appMainActivity.appMainViewModel.questionnaireSubmissionLiveData.value
    Assert.assertNotNull(questionnaireSubmission)
    Assert.assertEquals("Task/12345", questionnaireSubmission?.questionnaireConfig?.taskId)
    Assert.assertEquals("questionnaireId", questionnaireSubmission?.questionnaireConfig?.id)
    Assert.assertEquals(
      QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS,
      questionnaireSubmission?.questionnaireResponse?.status
    )
  }

  @Test
  fun testOnSubmitQuestionnaireShouldUpdateDataRefreshLivedata() {
    val appMainViewModel = mockk<AppMainViewModel>()
    val questionnaireSubmissionLiveData = mockk<MutableLiveData<QuestionnaireSubmission?>>()
    every { questionnaireSubmissionLiveData.postValue(any()) } just runs
    every { appMainViewModel.questionnaireSubmissionLiveData } returns
      questionnaireSubmissionLiveData
    val refreshLiveDataMock = mockk<MutableLiveData<Boolean?>>()
    every { refreshLiveDataMock.postValue(true) } just runs
    every { appMainViewModel.dataRefreshLivedata } returns refreshLiveDataMock
    every { appMainActivity.appMainViewModel } returns appMainViewModel

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
            QuestionnaireConfig(
              taskId = "Task/12345",
              id = "questionnaireId",
              refreshContent = true
            )
          )
        }
      )
    )

    verify { refreshLiveDataMock.postValue(true) }
  }

  @Test
  fun testRunSyncWhenDeviceIsOnline() {

    mockkStatic(Context::isDeviceOnline)

    every { appMainActivity.isDeviceOnline() } returns true

    val syncBroadcaster =
      mockk<SyncBroadcaster> {
        every { runSync(any()) } returns Unit
        every { schedulePeriodicSync(any()) } returns Unit
      }

    ReflectionHelpers.callInstanceMethod<Unit>(
      appMainActivity,
      "runSync",
      ReflectionHelpers.ClassParameter(SyncBroadcaster::class.java, syncBroadcaster)
    )

    verify(exactly = 1) { syncBroadcaster.runSync(any()) }
    verify(exactly = 1) { syncBroadcaster.schedulePeriodicSync(any()) }
  }

  @Test
  fun testDoNotRunSyncWhenDeviceIsOffline() {

    mockkStatic(Context::isDeviceOnline)

    every { appMainActivity.isDeviceOnline() } returns false

    val syncBroadcaster = mockk<SyncBroadcaster>()

    ReflectionHelpers.callInstanceMethod<Unit>(
      appMainActivity,
      "runSync",
      ReflectionHelpers.ClassParameter(SyncBroadcaster::class.java, syncBroadcaster)
    )

    verify(exactly = 0) { syncBroadcaster.runSync(any()) }
    verify(exactly = 0) { syncBroadcaster.schedulePeriodicSync(any()) }
  }

  @Test
  fun testStartForResult() {
    val event = appMainActivity.startForResult

    Assert.assertNotNull(event)
  }
}
