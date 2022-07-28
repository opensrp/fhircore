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

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.LoginConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
@ExperimentalCoroutinesApi
class LoginScreenTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()
  @BindValue
  var configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry(mockk())

  private val listenerObjectSpy =
    spyk(
      object {
        // Imitate click action by doing nothing
        fun onUsernameUpdated(userName: String) {}
        fun onPasswordUpdated() {}
        fun forgotPassword() {}
        fun attemptRemoteLogin() {}
      }
    )

  private lateinit var loginViewModel: LoginViewModel

  val applicationConfiguration =
    ApplicationConfiguration(
      appTitle = "My app",
      appId = "app/debug",
      loginConfig = LoginConfig(showLogo = true)
    )

  @Before
  fun setUp() {

    loginViewModel =
      mockk {
        every { username } returns MutableLiveData("demo")
        every { password } returns MutableLiveData("1234")
        every { loginErrorState } returns MutableLiveData(null)
        every { showProgressBar } returns MutableLiveData(false)
      }

    coEvery { loginViewModel.applicationConfiguration } returns applicationConfiguration
  }

  @Test
  fun testLoginScreen() {
    composeRule.setContent {
      LoginScreen(loginViewModel = loginViewModel, appVersionPair = Pair(1, "1.0.1"))
    }
    if (applicationConfiguration.loginConfig.showLogo) {
      composeRule.onNodeWithTag(APP_LOGO_TAG).assertExists()
    }
    composeRule.onNodeWithTag(APP_NAME_TEXT_TAG).assertExists()
    composeRule.onNodeWithTag(USERNAME_FIELD_TAG).assertExists()
    composeRule.onNodeWithTag(PASSWORD_FIELD_TAG).assertExists()
    composeRule.onNodeWithTag(LOGIN_BUTTON_TAG).assertExists().assertHasClickAction()
  }

  @Test
  fun testLoginPage() {
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = applicationConfiguration,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated("test") },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        appVersionPair = Pair(1, "1.0.1")
      )
    }
    if (applicationConfiguration.loginConfig.showLogo) {
      composeRule.onNodeWithTag(APP_LOGO_TAG).assertExists()
    }
    composeRule.onNodeWithTag(APP_NAME_TEXT_TAG).assertExists()
    composeRule.onNodeWithTag(USERNAME_FIELD_TAG).assertExists()
    composeRule.onNodeWithTag(PASSWORD_FIELD_TAG).assertExists()

    composeRule.onNodeWithTag(LOGIN_BUTTON_TAG).assertExists().assertHasClickAction()
  }
}
