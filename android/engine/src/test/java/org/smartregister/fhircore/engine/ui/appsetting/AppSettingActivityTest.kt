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
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.di.NetworkModule
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@UninstallModules(NetworkModule::class)
@HiltAndroidTest
class AppSettingActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val activityScenarioRule = activityScenarioRule<AppSettingActivity>()

  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry()

  val context: Context =
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testAppSettingActivity_withAppId_hasNotBeenSubmitted() {
    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      Assert.assertEquals(
        null,
        activity.sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)
      )
      Assert.assertEquals(false, activity.accountAuthenticator.hasActiveSession())
    }
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasNotLoggedIn() {
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "app")
    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      Assert.assertEquals(
        "app",
        activity.sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)
      )
      Assert.assertEquals(false, activity.accountAuthenticator.hasActiveSession())
    }
  }

  @Test
  @Ignore("Find a way to fake an access token to make hasActiveSession return true")
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasLoggedIn() {
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "app")
    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      Assert.assertEquals(
        "app",
        activity.sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)
      )
      Assert.assertEquals(true, activity.accountAuthenticator.hasActiveSession())
    }
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasLoggedIn_withSessionToken_hasExpired() {
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "app")
    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      Assert.assertEquals(
        "app",
        activity.sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)
      )
      Assert.assertEquals(false, activity.accountAuthenticator.hasActiveSession())
    }
  }

  @Test
  fun testAppSettingActivity_withConfig_hasBeenLoaded() {
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "app/debug")
    activityScenarioRule.scenario.recreate()
    activityScenarioRule.scenario.onActivity { activity ->
      activity.configurationRegistry.configsJsonMap.let { workflows ->
        Assert.assertTrue(workflows.isNotEmpty())
      }
    }
  }
}
