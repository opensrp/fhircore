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
import com.google.gson.Gson
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.IS_LOGGED_IN
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class AppSettingActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  val context: Context =
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
  @Inject lateinit var gson: Gson
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @BindValue val secureSharedPreference = mockk<SecureSharedPreference>()
  @BindValue val accountAuthenticator = mockk<AccountAuthenticator>()
  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry()

  private lateinit var appSettingActivityActivity: AppSettingActivity

  private lateinit var appSettingActivityActivitySpy: AppSettingActivity

  @Before
  fun setUp() {
    hiltRule.inject()

    appSettingActivityActivity =
      Robolectric.buildActivity(AppSettingActivity::class.java).create().resume().get()

    appSettingActivityActivitySpy = spyk(appSettingActivityActivity, recordPrivateCalls = true)
    every { appSettingActivityActivitySpy.finish() } returns Unit
  }

  @Test
  fun testAppSettingActivity_withAppId_hasNotBeenSubmitted() {
    every { accountAuthenticator.hasActiveSession() } returns false

    Assert.assertEquals(
      false,
      appSettingActivityActivity.sharedPreferencesHelper.read(IS_LOGGED_IN, false)
    )
    Assert.assertEquals(
      null,
      appSettingActivityActivity.sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)
    )
    Assert.assertEquals(false, appSettingActivityActivity.accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasNotLoggedIn() {
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "default")
    every { accountAuthenticator.hasActiveSession() } returns false

    Assert.assertEquals(
      false,
      appSettingActivityActivity.sharedPreferencesHelper.read(IS_LOGGED_IN, false)
    )
    Assert.assertEquals(
      "default",
      appSettingActivityActivity.sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)
    )
    Assert.assertEquals(false, appSettingActivityActivity.accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasLoggedIn() {
    sharedPreferencesHelper.write(IS_LOGGED_IN, true)
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "default")
    every { accountAuthenticator.hasActiveSession() } returns true

    Assert.assertEquals(
      true,
      appSettingActivityActivity.sharedPreferencesHelper.read(IS_LOGGED_IN, false)
    )
    Assert.assertEquals(
      "default",
      appSettingActivityActivity.sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)
    )
    Assert.assertEquals(true, appSettingActivityActivity.accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testAppSettingActivity_withAppId_hasBeenSubmitted_withUser_hasLoggedIn_withSessionToken_hasExpired() {
    sharedPreferencesHelper.write(IS_LOGGED_IN, true)
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "default")
    every { accountAuthenticator.hasActiveSession() } returns false

    Assert.assertEquals(
      true,
      appSettingActivityActivity.sharedPreferencesHelper.read(IS_LOGGED_IN, false)
    )
    Assert.assertEquals(
      "default",
      appSettingActivityActivity.sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)
    )
    Assert.assertEquals(false, appSettingActivityActivity.accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testThatConfigsAreLoadedWhenAppSettingsIsLaunched() {
    appSettingActivityActivity.let { activity ->
      Assert.assertTrue(activity != null)
      Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    }
  }
}
