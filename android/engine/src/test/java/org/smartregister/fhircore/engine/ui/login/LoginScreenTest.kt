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

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
class LoginScreenTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

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

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var loginViewModel: LoginViewModel

  val loginConfig = loginViewConfigurationOf(showLogo = true)

  @Before
  fun setUp() {
    loginViewModel =
      mockk {
        every { username } returns MutableLiveData("demo")
        every { password } returns MutableLiveData("1234")
        every { loginError } returns MutableLiveData("login error")
        every { showProgressBar } returns MutableLiveData(false)
        every { loginViewConfiguration } returns MutableLiveData(loginConfig)
      }
  }

  @Test
  fun testLoginScreen() {
    composeRule.setContent { LoginScreen(loginViewModel = loginViewModel) }
    if (loginConfig.showLogo) {
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
        viewConfiguration = loginConfig,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated("test") },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() }
      )
    }
    if (loginConfig.showLogo) {
      composeRule.onNodeWithTag(APP_LOGO_TAG).assertExists()
    }
    composeRule.onNodeWithTag(APP_NAME_TEXT_TAG).assertExists()
    composeRule.onNodeWithTag(USERNAME_FIELD_TAG).assertExists()
    composeRule.onNodeWithTag(PASSWORD_FIELD_TAG).assertExists()

    composeRule.onNodeWithTag(LOGIN_BUTTON_TAG).assertExists().assertHasClickAction()
  }
}
