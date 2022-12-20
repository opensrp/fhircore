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

package org.smartregister.fhircore.engine.ui.login

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.ui.pin.PinSetupActivity
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class LoginActivityTest : ActivityRobolectricTest() {

  private lateinit var loginActivity: LoginActivity

  @get:Rule(order = 1) var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 2) val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

  @get:Rule(order = 3) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @BindValue val repository: DefaultRepository = mockk()

  @BindValue var accountAuthenticator: AccountAuthenticator = mockk()

  @BindValue lateinit var loginViewModel: LoginViewModel

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var loginService: LoginService

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
    loginViewModel.navigateToHome()
    verify { loginService.navigateToHome() }
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntentWhenPinExists() {
    initLoginActivity()
    coEvery { accountAuthenticator.hasActivePin() } returns true
    loginViewModel.navigateToHome()
    verify { loginService.navigateToPinLogin(false) }
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
    loginViewModel.navigateToHome()

    verify { loginService.navigateToHome() }
  }

  @Test
  fun testNavigateToPinSetupShouldVerifyExpectedIntent() {
    initLoginActivity()
    loginViewModel.navigateToHome()
    val expectedIntent = Intent(getActivity(), PinSetupActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun `navigate to screen shows PIN activity if PIN is enabled and active`() {
    coEvery { accountAuthenticator.hasActivePin() } returns true
    every { loginViewModel.isPinEnabled() } returns true
    initLoginActivity()
    verify { loginService.navigateToPinLogin(false) }
  }

  override fun getActivity(): Activity {
    return loginActivity
  }

  class TestLoginService : LoginService {
    override lateinit var loginActivity: AppCompatActivity

    override fun navigateToHome() {}
  }

  private fun initLoginActivity() {
    val controller = Robolectric.buildActivity(LoginActivity::class.java)
    loginActivity = controller.create().resume().get()

    loginActivity.configurationRegistry = configurationRegistry
    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, "default")
    loginService = loginActivity.loginService
  }
}
