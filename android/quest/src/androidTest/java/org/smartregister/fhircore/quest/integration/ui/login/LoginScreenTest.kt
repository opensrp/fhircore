/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.integration.ui.login

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.LoginConfig
import org.smartregister.fhircore.quest.ui.login.APP_LOGO_TAG
import org.smartregister.fhircore.quest.ui.login.APP_NAME_TEXT_TAG
import org.smartregister.fhircore.quest.ui.login.ForgotPasswordDialog
import org.smartregister.fhircore.quest.ui.login.LOGIN_BUTTON_TAG
import org.smartregister.fhircore.quest.ui.login.LoginErrorState
import org.smartregister.fhircore.quest.ui.login.LoginPage
import org.smartregister.fhircore.quest.ui.login.PASSWORD_FIELD_TAG
import org.smartregister.fhircore.quest.ui.login.PASSWORD_FORGOT_DIALOG
import org.smartregister.fhircore.quest.ui.login.USERNAME_FIELD_TAG

@ExperimentalCoroutinesApi
class LoginScreenTest {

  @get:Rule(order = 1) val composeRule = createComposeRule()

  private val listenerObjectSpy =
    object {
      // Imitate click action by doing nothing
      fun onUsernameUpdated() {}

      fun onPasswordUpdated() {}

      fun forgotPassword() {}

      fun attemptRemoteLogin() {}
    }

  private val applicationConfiguration =
    ApplicationConfiguration(
      appTitle = "My app",
      appId = "app/debug",
      loginConfig = LoginConfig(showLogo = true, supervisorContactNumber = "123-456-7890"),
    )

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun testLoginPage() {
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = applicationConfiguration,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated() },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        appVersionPair = Pair(1, "1.0.1"),
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

  @Test
  fun testForgotPasswordDialog() {
    composeRule.setContent {
      ForgotPasswordDialog(
        forgotPassword = {},
        supervisorContactNumber = applicationConfiguration.loginConfig.supervisorContactNumber,
        onDismissDialog = {},
      )
    }
    composeRule.onNodeWithTag(PASSWORD_FORGOT_DIALOG).assertExists()
  }

  @Test
  fun testForgotPasswordDialog_DisplayedCorrectly() {
    composeRule.setContent {
      ForgotPasswordDialog(
        supervisorContactNumber = applicationConfiguration.loginConfig.supervisorContactNumber,
        forgotPassword = {},
        onDismissDialog = {},
      )
    }
    assertDialogContent()
  }

  private fun assertDialogContent() {
    composeRule.onNodeWithTag(PASSWORD_FORGOT_DIALOG).assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.forgot_password_title))
      .assertIsDisplayed()
    composeRule.onNodeWithText(context.getString(R.string.call_supervisor)).assertIsDisplayed()
    composeRule
      .onNodeWithText(applicationConfiguration.loginConfig.supervisorContactNumber.toString())
      .assertIsDisplayed()
    composeRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()
    composeRule.onNodeWithText(context.getString(R.string.dial_number)).assertIsDisplayed()
  }

  @Test
  fun testForgotPasswordDialog_CancelButton_Click() {
    var dismissDialogClicked = false

    composeRule.setContent {
      ForgotPasswordDialog(
        supervisorContactNumber = applicationConfiguration.loginConfig.supervisorContactNumber,
        forgotPassword = {},
        onDismissDialog = { dismissDialogClicked = true },
      )
    }
    val cancelText = context.getString(R.string.cancel)
    composeRule.onNodeWithText(cancelText).performClick()
    assertTrue(dismissDialogClicked)
  }

  @Test
  fun testForgotPasswordDialog_DisplaysCorrectContactNumber() {
    composeRule.setContent {
      ForgotPasswordDialog(
        supervisorContactNumber = applicationConfiguration.loginConfig.supervisorContactNumber,
        forgotPassword = {},
        onDismissDialog = {},
      )
    }
    val contactNumber = applicationConfiguration.loginConfig.supervisorContactNumber
    composeRule.onNodeWithText(contactNumber.toString()).assertIsDisplayed()
  }

  @Test
  fun testForgotPasswordDialog_DialNumberButton_Click() {
    var forgotPasswordClicked = false
    var dismissDialogClicked = false
    composeRule.setContent {
      ForgotPasswordDialog(
        supervisorContactNumber = applicationConfiguration.loginConfig.supervisorContactNumber,
        forgotPassword = { forgotPasswordClicked = true },
        onDismissDialog = { dismissDialogClicked = true },
      )
    }
    val dialNumber = context.getString(R.string.dial_number)
    composeRule.onNodeWithText(dialNumber).performClick()
    assert(dismissDialogClicked)
    assert(forgotPasswordClicked)
  }

  @Test
  fun testOnDoneKeyboardActionPerformsLoginButtonClicked() {
    listenerObjectSpy.attemptRemoteLogin()
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = applicationConfiguration,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated() },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        appVersionPair = Pair(1, "1.0.1"),
      )
    }
    composeRule
      .onNodeWithTag(USERNAME_FIELD_TAG)
      .assertExists()
      .performTextInput("usernameFieldTag")
    composeRule
      .onNodeWithTag(PASSWORD_FIELD_TAG)
      .assertExists()
      .performTextInput("passwordFieldTag")
  }

  @Test
  fun testLoginFailsWithUnknownTextErrorMessage() {
    verifyUnknownTextErrorMessage(
      LoginErrorState.UNKNOWN_HOST,
      R.string.login_call_fail_error_message,
    )
  }

  @Test
  fun testLoginFailsWithInvalidCredentialsErrorMessage() {
    verifyInvalidCredentialsErrorMessage(
      LoginErrorState.INVALID_CREDENTIALS,
      R.string.invalid_login_credentials,
    )
  }

  @Test
  fun testLoginFailsWithMultiUserLoginErrorMessage() {
    verifyMultiUserLoginErrorMessage(
      LoginErrorState.MULTI_USER_LOGIN_ATTEMPT,
      R.string.multi_user_login_attempt,
    )
  }

  @Test
  fun testLoginFailsWithErrorFetchingUserMessage() {
    verifyErrorFetchingUser(
      LoginErrorState.ERROR_FETCHING_USER,
      org.smartregister.fhircore.quest.R.string.error_fetching_user_details,
    )
  }

  @Test
  fun testLoginFailsWithInvalidOfflineStateErrorMessage() {
    verifyInvalidOfflineState(
      LoginErrorState.INVALID_OFFLINE_STATE,
      R.string.invalid_offline_login_state,
    )
  }

  @Test
  fun testAppTitleLoginConfigShouldHideAppTitleWhenFalse() {
    val appConfigs =
      ApplicationConfiguration(
        appTitle = "My app",
        appId = "app/debug",
        loginConfig = LoginConfig(showLogo = true, showAppTitle = false),
      )
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = appConfigs,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated() },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        appVersionPair = Pair(1, "1.0.1"),
      )
    }
    composeRule.onNodeWithTag(APP_NAME_TEXT_TAG).assertDoesNotExist()
  }

  private fun verifyUnknownTextErrorMessage(loginErrorState: LoginErrorState, errorMessageId: Int) {
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = applicationConfiguration,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated() },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        loginErrorState = loginErrorState,
        appVersionPair = Pair(1, "1.0.1"),
      )
    }
    composeRule
      .onNodeWithText(context.getString(R.string.login_error, context.getString(errorMessageId)))
      .assertIsDisplayed()
  }

  private fun verifyInvalidCredentialsErrorMessage(
    loginErrorState: LoginErrorState,
    errorMessageId: Int,
  ) {
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = applicationConfiguration,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated() },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        loginErrorState = loginErrorState,
        appVersionPair = Pair(1, "1.0.1"),
      )
    }
    composeRule
      .onNodeWithText(context.getString(R.string.login_error, context.getString(errorMessageId)))
      .assertIsDisplayed()
  }

  private fun verifyMultiUserLoginErrorMessage(
    loginErrorState: LoginErrorState,
    errorMessageId: Int,
  ) {
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = applicationConfiguration,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated() },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        loginErrorState = loginErrorState,
        appVersionPair = Pair(1, "1.0.1"),
      )
    }
    composeRule
      .onNodeWithText(context.getString(R.string.login_error, context.getString(errorMessageId)))
      .assertIsDisplayed()
  }

  private fun verifyErrorFetchingUser(loginErrorState: LoginErrorState, errorMessageId: Int) {
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = applicationConfiguration,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated() },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        loginErrorState = loginErrorState,
        appVersionPair = Pair(1, "1.0.1"),
      )
    }
    composeRule
      .onNodeWithText(context.getString(R.string.login_error, context.getString(errorMessageId)))
      .assertIsDisplayed()
  }

  private fun verifyInvalidOfflineState(loginErrorState: LoginErrorState, errorMessageId: Int) {
    composeRule.setContent {
      LoginPage(
        applicationConfiguration = applicationConfiguration,
        username = "user",
        onUsernameChanged = { listenerObjectSpy.onUsernameUpdated() },
        password = "password",
        onPasswordChanged = { listenerObjectSpy.onPasswordUpdated() },
        forgotPassword = { listenerObjectSpy.forgotPassword() },
        onLoginButtonClicked = { listenerObjectSpy.attemptRemoteLogin() },
        loginErrorState = loginErrorState,
        appVersionPair = Pair(1, "1.0.1"),
      )
    }
    composeRule
      .onNodeWithText(context.getString(R.string.login_error, context.getString(errorMessageId)))
      .assertIsDisplayed()
  }
}
