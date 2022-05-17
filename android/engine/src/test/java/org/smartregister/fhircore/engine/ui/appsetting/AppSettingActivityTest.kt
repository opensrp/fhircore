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

package org.smartregister.fhircore.engine.ui.appsetting

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowToast
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule

@HiltAndroidTest
class AppSettingActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()
  @get:Rule(order = 2) var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val testAppId = "default"

  val defaultRepository: DefaultRepository = mockk()
  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry(defaultRepository)
  private lateinit var appSettingActivity: AppSettingActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    appSettingActivity =
      spyk(Robolectric.buildActivity(AppSettingActivity::class.java).create().resume().get())
  }

  @Test
  fun testThatConfigsAreLoadedCorrectlyAndActivityIsFinished() {
    coroutineTestRule.runBlockingTest {
      appSettingActivity.appSettingViewModel.run {
        onApplicationIdChanged(testAppId)
        loadConfigurations(true)
      }
    }

    val workflowPointsMap = appSettingActivity.configurationRegistry.workflowPointsMap
    Assert.assertTrue(workflowPointsMap.isNotEmpty())
    Assert.assertTrue(workflowPointsMap.containsKey("default|application"))
    val configuration =
      appSettingActivity.configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
        AppConfigClassification.APPLICATION
      )
    Assert.assertEquals(testAppId, configuration.appId)
    Assert.assertEquals("application", configuration.classification)
    Assert.assertEquals("DefaultAppTheme", configuration.theme)
    Assert.assertTrue(configuration.languages.containsAll(listOf("en", "sw")))
    // TODO Assert.assertTrue(appSettingActivity.isFinishing)
  }

  @Test
  @Ignore
  fun testThatConfigsAreNotLoadedAndToastNotificationDisplayed() = runBlockingTest {
    coEvery { configurationRegistry.repository.searchCompositionByIdentifier("wrongAppId") } returns
      null

    appSettingActivity.appSettingViewModel.run {
      onApplicationIdChanged("wrongAppId")
      loadConfigurations(true)
    }
    val latestToast = ShadowToast.getLatestToast()
    Assert.assertEquals(Toast.LENGTH_LONG, latestToast.duration)
  }

  @Test
  fun testLocalConfig() {
    appSettingActivity.appSettingViewModel.run {
      onApplicationIdChanged("$testAppId/debug")
      loadConfigurations(true)
    }

    Assert.assertTrue(appSettingActivity.appSettingViewModel.hasDebugSuffix() == true)
    Assert.assertEquals("default/debug", appSettingActivity.appSettingViewModel.appId.value)
    Assert.assertEquals(8, appSettingActivity.configurationRegistry.workflowPointsMap.size)

    val workflows = appSettingActivity.configurationRegistry.workflowPointsMap
    Assert.assertTrue(workflows.containsKey("default|application"))
    Assert.assertTrue(workflows.containsKey("default|login"))
    Assert.assertTrue(workflows.containsKey("default|patient_register"))
    Assert.assertTrue(workflows.containsKey("default|patient_task_register"))
    Assert.assertTrue(workflows.containsKey("default|pin"))
    Assert.assertTrue(workflows.containsKey("default|patient_details_view"))
    Assert.assertTrue(workflows.containsKey("default|result_details_navigation"))
    Assert.assertTrue(workflows.containsKey("default|sync"))
  }

  override fun getActivity(): Activity = appSettingActivity
}
