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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.APP_ID_CONFIG
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class AppSettingActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @BindValue val accountAuthenticator = mockk<AccountAuthenticator>(relaxed = true)

  @BindValue
  var configurationRegistry = Faker.buildTestConfigurationRegistry(defaultRepository = mockk())

  private lateinit var activityController: ActivityController<AppSettingActivity>

  private lateinit var appSettingActivity: AppSettingActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    activityController = Robolectric.buildActivity(AppSettingActivity::class.java)
    appSettingActivity = activityController.create().get()
  }

  @Test
  fun testAppSettingActivity_withAppId_hasNotBeenSubmitted() {
    every { accountAuthenticator.hasActiveSession() } returns false
    activityController.recreate()
    Assert.assertEquals(null, appSettingActivity.sharedPreferencesHelper.read(APP_ID_CONFIG, null))
    Assert.assertEquals(false, appSettingActivity.accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasNotLoggedIn() {
    sharedPreferencesHelper.write(APP_ID_CONFIG, "default")
    every { accountAuthenticator.hasActiveSession() } returns false
    activityController.recreate()
    Assert.assertEquals(
      "default",
      appSettingActivity.sharedPreferencesHelper.read(APP_ID_CONFIG, null)
    )
    Assert.assertEquals(false, appSettingActivity.accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasLoggedIn() {
    sharedPreferencesHelper.write(APP_ID_CONFIG, "default")
    every { accountAuthenticator.hasActiveSession() } returns true
    activityController.recreate()
    Assert.assertEquals(
      "default",
      appSettingActivity.sharedPreferencesHelper.read(APP_ID_CONFIG, null)
    )
    Assert.assertEquals(true, appSettingActivity.accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasLoggedIn_withSessionToken_hasExpired() {
    sharedPreferencesHelper.write(APP_ID_CONFIG, "default")
    every { accountAuthenticator.hasActiveSession() } returns false
    activityController.recreate()
    Assert.assertEquals(
      "default",
      appSettingActivity.sharedPreferencesHelper.read(APP_ID_CONFIG, null)
    )
    Assert.assertEquals(false, appSettingActivity.accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testAppSettingActivity_withConfig_hasBeenLoaded() {
    sharedPreferencesHelper.write(APP_ID_CONFIG, "default/debug")
    every { accountAuthenticator.hasActiveSession() } returns true
    activityController.recreate()
    appSettingActivity.configurationRegistry.workflowPointsMap.let { workflows ->
      Assert.assertEquals(9, workflows.size)
      Assert.assertEquals(true, workflows.containsKey("default|application"))
      Assert.assertEquals(true, workflows.containsKey("default|login"))
      Assert.assertEquals(true, workflows.containsKey("default|app_feature"))
      Assert.assertEquals(true, workflows.containsKey("default|patient_register"))
      Assert.assertEquals(true, workflows.containsKey("default|patient_task_register"))
      Assert.assertEquals(true, workflows.containsKey("default|pin"))
      Assert.assertEquals(true, workflows.containsKey("default|patient_details_view"))
      Assert.assertEquals(true, workflows.containsKey("default|result_details_navigation"))
      Assert.assertEquals(true, workflows.containsKey("default|sync"))
    }
  }
}
