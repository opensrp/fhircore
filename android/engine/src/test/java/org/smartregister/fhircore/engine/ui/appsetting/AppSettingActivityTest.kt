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
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.util.APP_ID_KEY
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class AppSettingActivityTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) var activityScenarioRule = activityScenarioRule<AppSettingActivity>()

  val context: Context =
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
  @BindValue val sharedPreferencesHelper = SharedPreferencesHelper(context)
  @BindValue val secureSharedPreference = mockk<SecureSharedPreference>()
  @BindValue val accountAuthenticator = mockk<AccountAuthenticator>()
  @BindValue
  var configurationRegistry = Faker.buildTestConfigurationRegistry(defaultRepository = mockk())

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testAppSettingActivity_withAppId_hasNotBeenSubmitted() {
    every { accountAuthenticator.hasActiveSession() } returns false

    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      Assert.assertEquals(null, activity.sharedPreferencesHelper.read(APP_ID_KEY, null))
      Assert.assertEquals(false, activity.accountAuthenticator.hasActiveSession())
    }
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasNotLoggedIn() {
    sharedPreferencesHelper.write(APP_ID_KEY, "default")
    every { accountAuthenticator.hasActiveSession() } returns false

    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      Assert.assertEquals("default", activity.sharedPreferencesHelper.read(APP_ID_KEY, null))
      Assert.assertEquals(false, activity.accountAuthenticator.hasActiveSession())
    }
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasLoggedIn() {
    sharedPreferencesHelper.write(APP_ID_KEY, "default")
    every { accountAuthenticator.hasActiveSession() } returns true

    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      Assert.assertEquals("default", activity.sharedPreferencesHelper.read(APP_ID_KEY, null))
      Assert.assertEquals(true, activity.accountAuthenticator.hasActiveSession())
    }
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasLoggedIn_withSessionToken_hasExpired() {
    sharedPreferencesHelper.write(APP_ID_KEY, "default")
    every { accountAuthenticator.hasActiveSession() } returns false

    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      Assert.assertEquals("default", activity.sharedPreferencesHelper.read(APP_ID_KEY, null))
      Assert.assertEquals(false, activity.accountAuthenticator.hasActiveSession())
    }
  }

  @Test
  fun testAppSettingActivity_withConfig_hasBeenLoaded() {
    sharedPreferencesHelper.write(APP_ID_KEY, "default/debug")
    every { accountAuthenticator.hasActiveSession() } returns true

    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      activity.configurationRegistry.configsJsonMap.let { workflows ->
        Assert.assertTrue(workflows.isNotEmpty())
      }
    }
  }
}
