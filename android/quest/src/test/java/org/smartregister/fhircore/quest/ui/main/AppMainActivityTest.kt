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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import java.io.Serializable
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity

@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class AppMainActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue val fhirCarePlanGenerator: FhirCarePlanGenerator = mockk()

  @BindValue val eventBus: EventBus = mockk()

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
    val viewModel = appMainActivity.appMainViewModel
    appMainActivity.onSync(SyncJobStatus.InProgress(SyncOperation.DOWNLOAD))

    // Timestamp will only updated for states Glitch, Finished or Failed. Defaults to empty
    Assert.assertTrue(viewModel.appMainUiState.value.lastSyncTime.isEmpty())
  }

  @Test
  fun testOnSyncWithSyncStateGlitch() {
    val viewModel = appMainActivity.appMainViewModel
    val timestamp = "2022-05-19"
    viewModel.sharedPreferencesHelper.write(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, timestamp)

    val syncJobStatus = SyncJobStatus.Glitch(exceptions = emptyList())
    val syncJobStatusTimestamp = syncJobStatus.timestamp

    appMainActivity.onSync(syncJobStatus)
    Assert.assertNotNull(viewModel.retrieveLastSyncTimestamp())

    // Timestamp updated to the SyncJobStatus timestamp
    Assert.assertEquals(
      viewModel.appMainUiState.value.lastSyncTime,
      viewModel.formatLastSyncTimestamp(syncJobStatusTimestamp)!!,
    )
  }

  @Test
  fun testOnSyncWithSyncStateFailedRetrievesTimestamp() {
    val viewModel = appMainActivity.appMainViewModel
    viewModel.sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      "2022-05-19",
    )
    appMainActivity.onSync(SyncJobStatus.Failed(listOf()))

    Assert.assertNotNull(viewModel.retrieveLastSyncTimestamp())
    Assert.assertEquals(
      viewModel.appMainUiState.value.lastSyncTime,
      viewModel.retrieveLastSyncTimestamp(),
    )
  }

  @Test
  fun testOnSyncWithSyncStateFailedWhenTimestampIsNotNull() {
    val viewModel = appMainActivity.appMainViewModel
    appMainActivity.onSync(SyncJobStatus.Failed(listOf()))
    Assert.assertNotNull(viewModel.appMainUiState.value.lastSyncTime)
  }

  @Test
  fun testOnSyncWithSyncStateFinished() {
    val viewModel = appMainActivity.appMainViewModel
    val stateFinished = SyncJobStatus.Finished()
    appMainActivity.onSync(stateFinished)

    Assert.assertEquals(
      viewModel.formatLastSyncTimestamp(timestamp = stateFinished.timestamp),
      viewModel.retrieveLastSyncTimestamp(),
    )
  }

  @Test
  fun testOnSubmitQuestionnaireShouldUpdateLiveData() = runTest {
    every { eventBus.events } returns mockk()
    coEvery { eventBus.triggerEvent(any()) } just runs
    appMainActivity.onSubmitQuestionnaire(
      ActivityResult(
        -1,
        Intent().apply {
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_RESPONSE,
            QuestionnaireResponse().apply {
              status = QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS
            },
          )
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_CONFIG,
            QuestionnaireConfig(taskId = "Task/12345", id = "questionnaireId") as Serializable,
          )
        },
      ),
    )

    val onSubmitQuestionnaireSlot = slot<AppEvent.OnSubmitQuestionnaire>()
    coVerify { eventBus.triggerEvent(capture(onSubmitQuestionnaireSlot)) }
    Assert.assertNotNull(onSubmitQuestionnaireSlot)
    val questionnaireSubmission = onSubmitQuestionnaireSlot.captured.questionnaireSubmission
    Assert.assertEquals("Task/12345", questionnaireSubmission?.questionnaireConfig?.taskId)
    Assert.assertEquals("questionnaireId", questionnaireSubmission?.questionnaireConfig?.id)
    Assert.assertEquals(
      QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS,
      questionnaireSubmission?.questionnaireResponse?.status,
    )
  }

  @Test
  fun testOnSubmitQuestionnaireShouldUpdateDataRefreshLivedata() = runTest {
    val appMainViewModel = mockk<AppMainViewModel>()
    val refreshLiveDataMock = mockk<MutableLiveData<Boolean?>>()
    every { refreshLiveDataMock.postValue(true) } just runs
    every { appMainActivity.appMainViewModel } returns appMainViewModel
    every { eventBus.events } returns mockk()
    coEvery { eventBus.triggerEvent(any()) } returns mockk()

    appMainActivity.onSubmitQuestionnaire(
      ActivityResult(
        -1,
        Intent().apply {
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_RESPONSE,
            QuestionnaireResponse().apply {
              status = QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS
            },
          )
          putExtra(
            QuestionnaireActivity.QUESTIONNAIRE_CONFIG,
            QuestionnaireConfig(taskId = "Task/12345", id = "questionnaireId") as Serializable,
          )
        },
      ),
    )

    coVerify { eventBus.triggerEvent(any()) }
  }

  @Test
  fun testStartForResult() {
    val event = appMainActivity.startForResult
    Assert.assertNotNull(event)
  }
}
