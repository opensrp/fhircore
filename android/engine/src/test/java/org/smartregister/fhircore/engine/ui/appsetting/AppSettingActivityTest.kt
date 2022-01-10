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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowToast
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest

@HiltAndroidTest
class AppSettingActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val testAppId = "appId"

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
    appSettingActivity.appSettingViewModel.run {
      onApplicationIdChanged(testAppId)
      loadConfigurations(true)
    }
    val configurationsMap = appSettingActivity.configurationRegistry.configurationsMap
    Assert.assertTrue(configurationsMap.isNotEmpty())
    Assert.assertTrue(configurationsMap.containsKey("appId|application"))
    val configuration = configurationsMap.getValue("appId|application")
    Assert.assertTrue(configuration is ApplicationConfiguration)
    val applicationConfiguration = configuration as ApplicationConfiguration
    Assert.assertEquals(testAppId, applicationConfiguration.appId)
    Assert.assertEquals("application", applicationConfiguration.classification)
    Assert.assertEquals("AppTheme", applicationConfiguration.theme)
    Assert.assertTrue(applicationConfiguration.languages.containsAll(listOf("en", "sw")))
    Assert.assertTrue(appSettingActivity.isFinishing)
  }

  @Test
  fun testThatConfigsAreNotLoadedAndToastNotificationDisplayed() {
    appSettingActivity.appSettingViewModel.run {
      onApplicationIdChanged("fakeAppId")
      loadConfigurations(true)
    }
    val latestToast = ShadowToast.getLatestToast()
    Assert.assertEquals(Toast.LENGTH_LONG, latestToast.duration)
  }

  override fun getActivity(): Activity = appSettingActivity
}
