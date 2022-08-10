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
import androidx.compose.material.ExperimentalMaterialApi
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.sync.ResourceSyncException
import com.google.android.fhir.sync.Result
import com.google.android.fhir.sync.State
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest

@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class AppMainActivityTest : ActivityRobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  lateinit var appMainActivity: AppMainActivity

  @Before
  fun setUp() {
    hiltRule.inject()

    val controller = Robolectric.buildActivity(AppMainActivity::class.java)
    appMainActivity = controller.create().get()
    runBlocking {
      appMainActivity.appMainViewModel.configurationRegistry.loadConfigurations(
        "app/debug",
        appMainActivity.applicationContext
      )
    }
    appMainActivity = controller.resume().get()
  }

  @Test
  fun activityShouldNotNull() {
    Assert.assertNotNull(getActivity())
  }

  @Test
  fun onResumeShouldRefreshDataState() {
    Assert.assertTrue(appMainActivity.appMainViewModel.refreshDataState.value)
  }

  @Test
  fun onResumeShouldRetrieveAppMainUiState() {
    appMainActivity.appMainViewModel.appMainUiState.value.let {
      Assert.assertEquals("ECBIS", it.appTitle)
      Assert.assertEquals("English", it.currentLanguage)
      Assert.assertEquals("", it.username)
      Assert.assertEquals("", it.lastSyncTime)
      Assert.assertTrue(
        it.languages
          .map { language -> language.displayName }
          .containsAll(listOf("English", "Swahili", "French"))
      )
      Assert.assertNotNull(it.navigationConfiguration)
      Assert.assertNotNull(it.registerCountMap)
    }
  }

  @Test
  fun onSyncShouldStart() {
    val state = State.Started

    appMainActivity.onSync(state)

    Assert.assertEquals(
      "Sync initiated…",
      appMainActivity.appMainViewModel.appMainUiState.value.lastSyncTime
    )
  }

  @Test
  fun onSyncShouldInProgress() {
    val state = State.InProgress(ResourceType.Patient)

    appMainActivity.onSync(state)

    Assert.assertEquals(
      "Sync in progress",
      appMainActivity.appMainViewModel.appMainUiState.value.lastSyncTime
    )
  }

  @Test
  fun onSyncShouldGlitchAndLastSyncTimeIsEmpty() {
    val exceptions =
      listOf(
        ResourceSyncException(ResourceType.Patient, ResourceNotFoundException("Patient", "12345"))
      )
    val state = State.Glitch(exceptions)

    appMainActivity.onSync(state)

    Assert.assertEquals("", appMainActivity.appMainViewModel.appMainUiState.value.lastSyncTime)
  }

  @Test
  fun onSyncShouldFailedAndRefreshDataStateIsTrue() {
    val configServiceSpy = spyk(appMainActivity.configService)
    every { configServiceSpy.schedulePlan(appMainActivity) } just runs
    appMainActivity.configService = configServiceSpy

    val exceptions =
      listOf(
        ResourceSyncException(ResourceType.Patient, ResourceNotFoundException("Patient", "12345"))
      )
    val state = State.Failed(Result.Error(exceptions))

    appMainActivity.onSync(state)

    Assert.assertTrue(appMainActivity.appMainViewModel.refreshDataState.value)
  }

  @Test
  fun onSyncShouldFinishedAndRefreshDataStateIsTrue() {
    val configServiceSpy = spyk(appMainActivity.configService)
    every { configServiceSpy.schedulePlan(appMainActivity) } just runs
    appMainActivity.configService = configServiceSpy

    val state = State.Finished(Result.Success())

    appMainActivity.onSync(state)

    Assert.assertTrue(appMainActivity.appMainViewModel.refreshDataState.value)
  }

  override fun getActivity(): Activity {
    return appMainActivity
  }
}
