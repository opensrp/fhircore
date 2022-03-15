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
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.ui.pin.PinSetupActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class LoginActivityTest : ActivityRobolectricTest() {

  private lateinit var loginActivity: LoginActivity

  @get:Rule var hiltRule = HiltAndroidRule(this)

  val accountAuthenticator = mockk<AccountAuthenticator>()

  lateinit var loginService: LoginService

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val repository: DefaultRepository = mockk()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  val defaultRepository: DefaultRepository = mockk()
  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry(defaultRepository)
  @BindValue
  val loginViewModel =
    LoginViewModel(
      accountAuthenticator,
      DefaultDispatcherProvider(),
      sharedPreferencesHelper,
      ApplicationProvider.getApplicationContext()
    )

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    coEvery { accountAuthenticator.hasActivePin() } returns false
    coEvery { sharedPreferencesHelper.read(FORCE_LOGIN_VIA_USERNAME, false) } returns false
    coEvery { sharedPreferencesHelper.read("shared_pref_theme", "") } returns ""
    coEvery { sharedPreferencesHelper.write(FORCE_LOGIN_VIA_USERNAME, false) } returns Unit

    loginActivity =
      spyk(Robolectric.buildActivity(LoginActivity::class.java).create().resume().get())
    loginService = loginActivity.loginService
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    loginViewModel.navigateToHome()

    verify { loginService.navigateToHome() }
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntentWhenPinExists() {
    coEvery { accountAuthenticator.hasActivePin() } returns true
    coEvery { sharedPreferencesHelper.read(FORCE_LOGIN_VIA_USERNAME, false) } returns true
    coEvery { sharedPreferencesHelper.read("shared_pref_theme", "") } returns ""
    coEvery { sharedPreferencesHelper.write(FORCE_LOGIN_VIA_USERNAME, false) } returns Unit
    val loginConfig = loginViewConfigurationOf(enablePin = true)
    loginViewModel.updateViewConfigurations(loginConfig)
    loginViewModel.navigateToHome()

    verify { loginService.navigateToHome() }
  }

  @Test
  fun testNavigateToPinSetupShouldVerifyExpectedIntent() {
    val loginConfig = loginViewConfigurationOf(enablePin = true)
    loginViewModel.updateViewConfigurations(loginConfig)
    loginViewModel.navigateToHome()
    val expectedIntent = Intent(getActivity(), PinSetupActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testGetApplicationConfiguration() {
    Assert.assertNotNull(loginActivity.getApplicationConfiguration())
  }

  override fun getActivity(): Activity {
    return loginActivity
  }

  class TestLoginService : LoginService {
    override lateinit var loginActivity: AppCompatActivity
    override fun navigateToHome() {}
  }
}
