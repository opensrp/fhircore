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

package org.smartregister.fhircore.engine.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.login.APP_NAME_TEXT_TAG
import org.smartregister.fhircore.engine.ui.login.ForgotPasswordDialog
import org.smartregister.fhircore.engine.ui.login.LOGIN_BUTTON_TAG
import org.smartregister.fhircore.engine.ui.login.LOGIN_FOOTER
import org.smartregister.fhircore.engine.ui.login.LoginScreen
import org.smartregister.fhircore.engine.ui.login.LoginViewModel
import org.smartregister.fhircore.engine.ui.login.PASSWORD_FIELD_TAG
import org.smartregister.fhircore.engine.ui.login.USERNAME_FIELD_TAG

class LoginScreenTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var loginViewModel: LoginViewModel
  private val app = ApplicationProvider.getApplicationContext<Application>()
  private val username = MutableLiveData("")
  private val password = MutableLiveData("")
  private val loginError = MutableLiveData("")
  private val showProgressBar = MutableLiveData(false)
  private val loginConfig = loginViewConfigurationOf()

  @Before
  fun setUp() {
    loginViewModel =
      mockk {
        every { loginViewConfiguration } returns MutableLiveData(loginConfig)
        every { username } returns this@LoginScreenTest.username
        every { password } returns this@LoginScreenTest.password
        every { loginError } returns this@LoginScreenTest.loginError
        every { showProgressBar } returns this@LoginScreenTest.showProgressBar
        every { onUsernameUpdated(any()) } answers
          {
            this@LoginScreenTest.username.value = firstArg()
          }
        every { onPasswordUpdated(any()) } answers
          {
            this@LoginScreenTest.password.value = firstArg()
          }
        every { attemptRemoteLogin() } returns Unit
      }
  }

  @Test
  fun testLoginScreenComponents() {

    composeRule.setContent { LoginScreen(loginViewModel) }

    // verifying app name heading properties
    composeRule.onNodeWithTag(APP_NAME_TEXT_TAG).assertExists()
    composeRule.onNodeWithTag(APP_NAME_TEXT_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(APP_NAME_TEXT_TAG).assertTextEquals(loginConfig.applicationName)

    // verify username input field properties
    composeRule.onNodeWithTag(USERNAME_FIELD_TAG).assertExists()
    composeRule.onNodeWithTag(USERNAME_FIELD_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(USERNAME_FIELD_TAG, useUnmergedTree = true).assertTextEquals("")
    composeRule.onNodeWithTag(USERNAME_FIELD_TAG, useUnmergedTree = true).performTextInput("demo")
    composeRule
      .onNodeWithTag(USERNAME_FIELD_TAG, useUnmergedTree = true)
      .assertTextEquals(username.value!!)
    verify(exactly = 1) { loginViewModel.onUsernameUpdated(username.value!!) }

    // verify password input field properties
    composeRule.onNodeWithTag(PASSWORD_FIELD_TAG).assertExists()
    composeRule.onNodeWithTag(PASSWORD_FIELD_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(PASSWORD_FIELD_TAG, useUnmergedTree = true).assertTextEquals("")
    composeRule.onNodeWithTag(PASSWORD_FIELD_TAG, useUnmergedTree = true).performTextInput("12345")
    composeRule
      .onNodeWithTag(PASSWORD_FIELD_TAG, useUnmergedTree = true)
      .assertTextEquals(password.value!!)
    verify(exactly = 1) { loginViewModel.onPasswordUpdated(password.value!!) }

    // verify login button properties and behaviour
    composeRule.onNodeWithTag(LOGIN_BUTTON_TAG).assertExists()
    composeRule.onNodeWithTag(LOGIN_BUTTON_TAG).assertTextEquals(app.getString(R.string.login_text))

    // verify login footer group properties
    composeRule.onNodeWithTag(LOGIN_FOOTER).assertExists()
  }

  @Test
  fun testForgotPasswordDialog() {
    composeRule.setContent { ForgotPasswordDialog(forgotPassword = {}, onDismissDialog = {}) }

    // verify forgot password dialog title is displayed
    composeRule.onNodeWithText("Forgot Password!").assertExists()
    composeRule.onNodeWithText("Forgot Password!").assertIsDisplayed()

    // verify forgot password dialog content
    composeRule.onNodeWithText("Please call your supervisor at 012-3456-789").assertExists()
    composeRule.onNodeWithText("Please call your supervisor at 012-3456-789").assertIsDisplayed()

    // verify cancel button is displayed
    composeRule.onNodeWithText("CANCEL").assertExists()
    composeRule.onNodeWithText("CANCEL").assertIsDisplayed()

    // verify dial number button is displayed
    composeRule.onNodeWithText("DIAL NUMBER").assertExists()
    composeRule.onNodeWithText("DIAL NUMBER").assertIsDisplayed()
  }
}
