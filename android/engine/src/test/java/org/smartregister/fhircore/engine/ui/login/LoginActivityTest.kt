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

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
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
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.ui.pin.PinSetupActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP
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

    coEvery { accountAuthenticator.retrieveLastLoggedInUsername() } returns ""

    fhirResourceDataSource = FhirResourceDataSource(resourceService)

    loginViewModel =
      LoginViewModel(
        accountAuthenticator = accountAuthenticator,
        dispatcher = DefaultDispatcherProvider(),
        sharedPreferences = sharedPreferencesHelper,
        fhirResourceDataSource = fhirResourceDataSource
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
    loginActivity.configurationRegistry.appId = "default"
    loginService = loginActivity.loginService
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    loginViewModel.navigateToHome()
    verify { loginService.navigateToHome() }
  }

  @Test
  fun testNavigateToHomeForManualAuthUpdateSetsUsername() {
    val accountName = "testUser"
    val updateAuthIntent =
      Intent().apply {
        putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticator.AUTH_TOKEN_TYPE)
        putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName)
        putExtra(
          AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
          mockk<AccountAuthenticatorResponse>()
        )
        putExtra(AccountAuthenticator.AUTH_TOKEN_TYPE, AccountAuthenticator.AUTH_TOKEN_TYPE)
      }
    Robolectric.buildActivity(LoginActivity::class.java, updateAuthIntent).create().resume()
    Assert.assertEquals(accountName, loginViewModel.username.value)
  }

  @Test
  fun testNavigateToHomeForManualAuthUpdateShouldNotVerifyExpectedIntent() {
    val accountName = "testUser"
    val updateAuthIntent =
      Intent().apply {
        putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticator.AUTH_TOKEN_TYPE)
        putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName)
        putExtra(
          AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
          mockk<AccountAuthenticatorResponse>()
        )
        putExtra(AccountAuthenticator.AUTH_TOKEN_TYPE, AccountAuthenticator.AUTH_TOKEN_TYPE)
      }
    loginActivity =
      spyk(
        Robolectric.buildActivity(LoginActivity::class.java, updateAuthIntent)
          .create()
          .resume()
          .get()
      )
    loginActivity.configurationRegistry = configurationRegistry
    loginActivity.configurationRegistry.appId = "default"
    loginService = loginActivity.loginService

    loginViewModel.navigateToHome()
    verify(exactly = 0) { loginService.navigateToHome() }

    loginActivity =
      spyk(Robolectric.buildActivity(LoginActivity::class.java).create().resume().get())
    loginActivity.configurationRegistry = configurationRegistry
    loginActivity.configurationRegistry.appId = "default"
    loginService = loginActivity.loginService
  }

  @Test
  fun testNavigateToHomeForManualAuthUpdateWithUsernameEditedShouldVerifyExpectedIntent() {
    val accountName = "testUser"
    val updateAuthIntent =
      Intent().apply {
        putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticator.AUTH_TOKEN_TYPE)
        putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName)
        putExtra(
          AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
          mockk<AccountAuthenticatorResponse>()
        )
        putExtra(AccountAuthenticator.AUTH_TOKEN_TYPE, AccountAuthenticator.AUTH_TOKEN_TYPE)
      }
    loginActivity =
      spyk(
        Robolectric.buildActivity(LoginActivity::class.java, updateAuthIntent)
          .create()
          .resume()
          .get()
      )
    loginActivity.configurationRegistry = configurationRegistry
    loginActivity.configurationRegistry.appId = "default"
    loginService = loginActivity.loginService
    every { loginActivity.setResult(any()) } just runs
    loginViewModel.onUsernameUpdated("newTestUser")

    loginViewModel.navigateToHome()
    verify(exactly = 1) { loginService.navigateToHome() }

    loginActivity =
      spyk(Robolectric.buildActivity(LoginActivity::class.java).create().resume().get())
    loginActivity.configurationRegistry = configurationRegistry
    loginActivity.configurationRegistry.appId = "default"
    loginService = loginActivity.loginService
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntentWhenPinExists() {
    coEvery { accountAuthenticator.hasActivePin() } returns true
    val loginConfig = loginViewConfigurationOf(enablePin = true)
    loginViewModel.updateViewConfigurations(loginConfig)
    loginViewModel.navigateToHome()
    verify { loginService.navigateToHome() }
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntentWhenForcedLogin() {
    coEvery { accountAuthenticator.hasActivePin() } returns false
    sharedPreferencesHelper.write(FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP, true)
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
    runBlocking {
      configurationRegistry.loadConfigurationsLocally("${configurationRegistry.appId}/debug") {
        Assert.assertTrue(it)
      }
    }
    Assert.assertNotNull(loginActivity.getApplicationConfiguration())
  }

  @Test
  fun testOnBackPressesTwoTimes() {
    loginActivity.onBackPressed()
    assertTrue(ReflectionHelpers.getField(loginActivity, "backPressed"))
    loginActivity.onBackPressed()
    verify { loginActivity.finishAffinity() }
  }

  override fun getActivity(): Activity {
    return loginActivity
  }

  class TestLoginService : LoginService {
    override lateinit var loginActivity: AppCompatActivity

    override fun navigateToHome() {}
  }
}
