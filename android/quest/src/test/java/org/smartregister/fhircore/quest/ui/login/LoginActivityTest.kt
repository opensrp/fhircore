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

package org.smartregister.fhircore.quest.ui.login

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowIntent
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.pin.PinLoginActivity
import org.smartregister.p2p.P2PLibrary

@ExperimentalCoroutinesApi
@HiltAndroidTest
class LoginActivityTest : ActivityRobolectricTest() {

  private lateinit var loginActivity: LoginActivity

  @get:Rule(order = 1) var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var secureSharedPreference: SecureSharedPreference

  @BindValue val repository: DefaultRepository = mockk()

  @BindValue var accountAuthenticator: AccountAuthenticator = mockk()

  @BindValue lateinit var loginViewModel: LoginViewModel

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  @Before
  fun setUp() {
    hiltRule.inject()

    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }

    coEvery { accountAuthenticator.hasActivePin() } returns false
    coEvery { accountAuthenticator.hasActiveSession() } returns true

    loginViewModel =
      spyk(
        LoginViewModel(
          accountAuthenticator = accountAuthenticator,
          dispatcher = coroutineTestRule.testDispatcherProvider,
          sharedPreferences = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          defaultRepository = mockk(),
          configService = mockk()
        )
      )
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    coEvery { accountAuthenticator.hasActiveSession() } returns
      false andThen
      false andThen
      true // to test this specific scenario
    every { loginViewModel.isPinEnabled() } returns false
    initLoginActivity()
    loginViewModel.updateNavigateHome()
    verify { loginActivity.navigateToHome() }
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntentWhenPinExists() {
    initLoginActivity()
    coEvery { accountAuthenticator.hasActivePin() } returns true
    loginViewModel.updateNavigateHome()
    verify { loginActivity.navigateToPinLogin(false) }
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntentWhenForcedLogin() {
    coEvery { accountAuthenticator.hasActiveSession() } returns
      false andThen
      false andThen
      true // to test this specific scenario
    coEvery { accountAuthenticator.hasActivePin() } returns false
    every { loginViewModel.isPinEnabled() } returns false

    initLoginActivity()
    loginViewModel.updateNavigateHome()

    verify { loginActivity.navigateToHome() }
  }

  @Test
  fun testNavigateToPinSetupShouldVerifyExpectedIntent() {
    initLoginActivity()
    loginViewModel.updateNavigateHome()
    val expectedIntent = Intent(getActivity(), PinSetupActivity::class.java)
    val actualIntent = shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun `navigate to screen shows PIN activity if PIN is enabled and active`() {
    coEvery { accountAuthenticator.hasActivePin() } returns true
    every { loginViewModel.isPinEnabled() } returns true
    initLoginActivity()
    verify { loginActivity.navigateToPinLogin(false) }
  }

  @Test
  fun testLaunchDialPadShouldStartActionDialActivity() {
    initLoginActivity()
    ReflectionHelpers.callInstanceMethod<Unit>(
      loginActivity,
      "launchDialPad",
      ReflectionHelpers.ClassParameter.from(String::class.java, "1234567")
    )

    val resultIntent = shadowOf(application).nextStartedActivity
    Assert.assertEquals(Intent.ACTION_DIAL, resultIntent.action)
    Assert.assertEquals("1234567", resultIntent.data.toString())
  }

  override fun getActivity(): Activity {
    return loginActivity
  }

  private fun initLoginActivity() {
    val controller = Robolectric.buildActivity(LoginActivity::class.java)
    loginActivity = spyk(controller.create().resume().get())

    loginActivity.configurationRegistry = configurationRegistry
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "default")
  }

  @Test
  fun testNavigateToPinLoginNavigateToPinLoginScreen() {
    loginActivity.navigateToPinLogin()
    val startedIntent: Intent = shadowOf(loginActivity).nextStartedActivity
    val shadowIntent: ShadowIntent = shadowOf(startedIntent)
    Assert.assertEquals(PinLoginActivity::class.java, shadowIntent.intentClass)
  }

  @Test
  fun testNavigateToPinSetupNavigateToPinSetupScreen() {
    loginActivity.navigateToPinLogin(launchSetup = true)
    val startedIntent: Intent = shadowOf(loginActivity).nextStartedActivity
    val shadowIntent: ShadowIntent = shadowOf(startedIntent)
    Assert.assertEquals(PinSetupActivity::class.java, shadowIntent.intentClass)
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun testNavigateHomeShouldDirectToAppMainActivity() {
    mockkObject(P2PLibrary)
    every { P2PLibrary.init(any()) } returns mockk()

    secureSharedPreference.saveCredentials(Faker.authCredentials)
    loginActivity.navigateToHome()

    val startedIntent: Intent = shadowOf(loginActivity).nextStartedActivity
    val shadowIntent: ShadowIntent = shadowOf(startedIntent)
    Assert.assertEquals(AppMainActivity::class.java, shadowIntent.intentClass)

    unmockkObject(P2PLibrary)
  }
}
