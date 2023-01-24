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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.theme.LoginBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.LoginButtonColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.engine.ui.theme.LoginFieldBackgroundColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.appVersion

const val APP_NAME_TEXT_TAG = "aapNameTextTag"
const val USERNAME_FIELD_TAG = "usernameFieldTag"
const val PASSWORD_FIELD_TAG = "passwordFieldTag"
const val LOGIN_BUTTON_TAG = "loginButtonTag"
const val LOGIN_ERROR_TEXT_TAG = "loginErrorTextTag"
const val LOGIN_FOOTER = "loginFooter"
const val APP_LOGO_TAG = "appLogoTag"
const val PASSWORD_FORGOT_DIALOG = "forgotPassWordDialog"

@Composable
fun LoginScreen(loginViewModel: LoginViewModel, appVersionPair: Pair<Int, String>? = null) {
  val applicationConfiguration = remember { loginViewModel.applicationConfiguration }
  val username by loginViewModel.username.observeAsState("")
  val password by loginViewModel.password.observeAsState("")
  val loginErrorState by loginViewModel.loginErrorState.observeAsState(null)
  val showProgressBar by loginViewModel.showProgressBar.observeAsState(false)

  LoginPage(
    applicationConfiguration = applicationConfiguration,
    username = username,
    onUsernameChanged = { loginViewModel.onUsernameUpdated(it) },
    password = password,
    onPasswordChanged = { loginViewModel.onPasswordUpdated(it) },
    forgotPassword = { loginViewModel.forgotPassword() },
    onLoginButtonClicked = { loginViewModel.attemptRemoteLogin() },
    loginErrorState = loginErrorState,
    showProgressBar = showProgressBar,
    appVersionPair = appVersionPair
  )
}

@Composable
fun LoginPage(
  applicationConfiguration: ApplicationConfiguration,
  username: String,
  onUsernameChanged: (String) -> Unit,
  password: String,
  onPasswordChanged: (String) -> Unit,
  forgotPassword: () -> Unit,
  onLoginButtonClicked: () -> Unit,
  modifier: Modifier = Modifier,
  loginErrorState: LoginErrorState? = null,
  showProgressBar: Boolean = false,
  appVersionPair: Pair<Int, String>? = null
) {
  var showPassword by remember { mutableStateOf(false) }
  val backgroundColor =
    if (applicationConfiguration.useDarkTheme) LoginBackgroundColor else Color.White
  val contentColor = if (applicationConfiguration.useDarkTheme) Color.White else LoginDarkColor
  val textFieldBackgroundColor =
    if (applicationConfiguration.useDarkTheme) LoginFieldBackgroundColor else Color.Unspecified
  val forgotPasswordColor =
    if (applicationConfiguration.useDarkTheme) Color.White else LoginButtonColor
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val (versionCode, versionName) = remember { appVersionPair ?: context.appVersion() }

  Surface(
    modifier =
      modifier
        .fillMaxSize()
        .scrollable(orientation = Orientation.Vertical, state = rememberScrollState()),
    color = backgroundColor,
    contentColor = contentColorFor(backgroundColor = contentColor)
  ) {
    if (showForgotPasswordDialog) {
      ForgotPasswordDialog(
        forgotPassword = forgotPassword,
        onDismissDialog = { showForgotPasswordDialog = false }
      )
    }
    Column(
      modifier =
        modifier.padding(horizontal = 16.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(modifier = modifier.height(20.dp))
      Column(modifier = modifier.padding(4.dp), verticalArrangement = Arrangement.Center) {
        // TODO Add configurable logo. Images to be downloaded from server
        if (applicationConfiguration.loginConfig.showLogo) {
          Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = stringResource(id = R.string.app_logo),
            modifier =
              modifier
                .align(Alignment.CenterHorizontally)
                .requiredHeight(120.dp)
                .requiredWidth(140.dp)
                .testTag(APP_LOGO_TAG),
          )
        }
        Text(
          color = if (applicationConfiguration.useDarkTheme) Color.White else LoginDarkColor,
          text = applicationConfiguration.appTitle,
          fontWeight = FontWeight.Bold,
          fontSize = 32.sp,
          modifier =
            modifier
              .wrapContentWidth()
              .padding(vertical = 8.dp)
              .align(Alignment.CenterHorizontally)
              .testTag(APP_NAME_TEXT_TAG)
        )
        Spacer(modifier = modifier.height(40.dp))
        Text(
          text = stringResource(R.string.username),
          color = contentColor,
          modifier = modifier.padding(vertical = 8.dp)
        )
        OutlinedTextField(
          colors = TextFieldDefaults.outlinedTextFieldColors(textColor = contentColor),
          value = username,
          onValueChange = onUsernameChanged,
          maxLines = 1,
          singleLine = true,
          placeholder = {
            Text(
              color = Color.LightGray,
              text = stringResource(R.string.username_input_hint),
            )
          },
          modifier =
            modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .background(color = textFieldBackgroundColor)
              .testTag(USERNAME_FIELD_TAG)
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = modifier.fillMaxWidth()) {
          Text(
            text = stringResource(R.string.password),
            color = contentColor,
            modifier = modifier.wrapContentWidth().padding(vertical = 8.dp)
          )
          Text(
            text = stringResource(R.string.forgot_password),
            color = forgotPasswordColor,
            style = TextStyle(textDecoration = TextDecoration.Underline, color = contentColor),
            modifier =
              modifier.wrapContentWidth().padding(vertical = 8.dp).clickable {
                showForgotPasswordDialog = !showForgotPasswordDialog
              }
          )
        }
        OutlinedTextField(
          value = password,
          colors = TextFieldDefaults.outlinedTextFieldColors(textColor = contentColor),
          onValueChange = onPasswordChanged,
          maxLines = 1,
          singleLine = true,
          placeholder = {
            Text(
              color = Color.LightGray,
              text = stringResource(R.string.password_input_hint),
            )
          },
          visualTransformation =
            if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier =
            modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .background(color = textFieldBackgroundColor)
              .testTag(PASSWORD_FIELD_TAG),
          trailingIcon = {
            val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(imageVector = image, "", tint = contentColor)
            }
          }
        )
        Spacer(modifier = modifier.height(10.dp))
        Text(
          fontSize = 14.sp,
          color = MaterialTheme.colors.error,
          text =
            when (loginErrorState) {
              LoginErrorState.UNKNOWN_HOST ->
                stringResource(
                  id = R.string.login_error,
                  stringResource(R.string.login_call_fail_error_message)
                )
              LoginErrorState.INVALID_CREDENTIALS ->
                stringResource(
                  id = R.string.login_error,
                  stringResource(R.string.invalid_login_credentials)
                )
              null -> ""
              LoginErrorState.MULTI_USER_LOGIN_ATTEMPT ->
                stringResource(
                  id = R.string.login_error,
                  stringResource(R.string.multi_user_login_attempt)
                )
            },
          modifier =
            modifier
              .wrapContentWidth()
              .padding(0.dp)
              .align(Alignment.Start)
              .testTag(LOGIN_ERROR_TEXT_TAG)
        )
        Spacer(modifier = modifier.height(30.dp))
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
          Button(
            enabled = !showProgressBar && username.isNotEmpty() && password.isNotEmpty(),
            colors =
              ButtonDefaults.buttonColors(
                backgroundColor = LoginButtonColor,
                disabledBackgroundColor =
                  if (applicationConfiguration.useDarkTheme) LoginFieldBackgroundColor
                  else Color.LightGray
              ),
            onClick = onLoginButtonClicked,
            modifier = modifier.fillMaxWidth().testTag(LOGIN_BUTTON_TAG)
          ) {
            Text(
              color = Color.White,
              text = stringResource(id = R.string.login_text),
              modifier = modifier.padding(8.dp)
            )
          }
          if (showProgressBar) {
            CircularProgressBar(modifier = modifier.matchParentSize().padding(4.dp))
          }
        }
      }
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        Column {
          Text(
            color = contentColor,
            text = stringResource(id = R.string.powered_by),
            modifier = modifier.wrapContentWidth().padding(vertical = 8.dp).align(Alignment.Start)
          )
          Image(
            painter = painterResource(id = R.drawable.ic_opensrp_logo),
            contentDescription = stringResource(id = R.string.app_logo),
            modifier = modifier.align(Alignment.CenterHorizontally).requiredHeight(40.dp)
          )
        }

        Text(
          color = contentColor,
          fontSize = 16.sp,
          text = stringResource(id = R.string.app_version, versionCode, versionName),
          modifier = modifier.wrapContentWidth().padding(0.dp).testTag(LOGIN_FOOTER)
        )
      }
    }
  }
}

@Composable
fun ForgotPasswordDialog(
  forgotPassword: () -> Unit,
  onDismissDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  AlertDialog(
    onDismissRequest = onDismissDialog,
    title = {
      Text(
        text = stringResource(R.string.forgot_password_title),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      )
    },
    text = {
      Text(text = stringResource(R.string.call_supervisor, "012-3456-789"), fontSize = 16.sp)
    },
    buttons = {
      Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.End
      ) {
        Text(
          text = stringResource(R.string.cancel),
          modifier = modifier.padding(horizontal = 10.dp).clickable { onDismissDialog() }
        )
        Text(
          color = MaterialTheme.colors.primary,
          text = stringResource(R.string.dial_number),
          modifier =
            modifier.padding(horizontal = 10.dp).clickable {
              onDismissDialog()
              forgotPassword()
            }
        )
      }
    },
    modifier = Modifier.testTag(PASSWORD_FORGOT_DIALOG)
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun LoginScreenPreview() {
  LoginPage(
    applicationConfiguration =
      ApplicationConfiguration(appId = "appId", configType = "application"),
    username = "",
    onUsernameChanged = {},
    password = "",
    onPasswordChanged = {},
    forgotPassword = {},
    onLoginButtonClicked = {},
    appVersionPair = Pair(1, "0.0.1")
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun LoginScreenPreviewDarkMode() {
  LoginPage(
    applicationConfiguration =
      ApplicationConfiguration(appId = "appId", configType = "application"),
    username = "",
    onUsernameChanged = {},
    password = "",
    onPasswordChanged = {},
    forgotPassword = {},
    onLoginButtonClicked = {},
    appVersionPair = Pair(1, "0.0.1")
  )
}
