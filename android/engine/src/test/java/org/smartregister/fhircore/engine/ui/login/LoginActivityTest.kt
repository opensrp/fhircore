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
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.ui.pin.PinSetupActivity
import org.smartregister.fhircore.engine.util.APP_ID_KEY
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class LoginActivityTest : ActivityRobolectricTest() {

  private lateinit var loginActivity: LoginActivity

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @BindValue val repository: DefaultRepository = mockk()

  lateinit var configurationRegistry: ConfigurationRegistry

  @BindValue lateinit var loginViewModel: LoginViewModel

  private val accountAuthenticator: AccountAuthenticator = mockk()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val resourceService: FhirResourceService = mockk()

  private lateinit var loginService: LoginService

  private lateinit var fhirResourceDataSource: FhirResourceDataSource

  @Before
  fun setUp() {
    hiltRule.inject()

    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }

    coEvery { accountAuthenticator.hasActivePin() } returns false

    fhirResourceDataSource = FhirResourceDataSource(resourceService)

    loginViewModel =
      LoginViewModel(
        accountAuthenticator = accountAuthenticator,
        dispatcher = DefaultDispatcherProvider(),
        sharedPreferences = sharedPreferencesHelper,
        fhirResourceDataSource = fhirResourceDataSource,
        configurationRegistry = configurationRegistry
      )

    loginActivity =
      spyk(Robolectric.buildActivity(LoginActivity::class.java).create().resume().get())

    configurationRegistry =
      ConfigurationRegistry(
        ApplicationProvider.getApplicationContext<Context>(),
        fhirResourceDataSource,
        sharedPreferencesHelper,
        DefaultDispatcherProvider(),
        repository
      )

    loginActivity.configurationRegistry = configurationRegistry
    sharedPreferencesHelper.write(APP_ID_KEY, "default")
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
    loginViewModel.navigateToHome()
    verify { loginService.navigateToHome() }
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntentWhenForcedLogin() {
    coEvery { accountAuthenticator.hasActivePin() } returns false
    loginViewModel.navigateToHome()

    verify { loginService.navigateToHome() }
  }

  @Test
  fun testNavigateToPinSetupShouldVerifyExpectedIntent() {
    loginViewModel.navigateToHome()
    val expectedIntent = Intent(getActivity(), PinSetupActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return loginActivity
  }

  class TestLoginService : LoginService {
    override lateinit var loginActivity: AppCompatActivity

    override fun navigateToHome() {}
  }
}
