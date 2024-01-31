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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package org.smartregister.fhircore.quest.ui.login

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
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
  val dataMigrationInProgress by loginViewModel.dataMigrationInProgress.observeAsState(false)
  val context = LocalContext.current

  LoginPage(
    applicationConfiguration = applicationConfiguration,
    username = username,
    onUsernameChanged = { loginViewModel.onUsernameUpdated(it) },
    password = password,
    onPasswordChanged = { loginViewModel.onPasswordUpdated(it) },
    forgotPassword = { loginViewModel.forgotPassword() },
    onLoginButtonClicked = { loginViewModel.login(context) },
    loginErrorState = loginErrorState,
    showProgressBar = showProgressBar,
    appVersionPair = appVersionPair,
    dataMigrationInProgress = dataMigrationInProgress,
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
  appVersionPair: Pair<Int, String>? = null,
  dataMigrationInProgress: Boolean,
) {
  var showPassword by remember { mutableStateOf(false) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val (versionCode, versionName) = remember { appVersionPair ?: context.appVersion() }
  val coroutineScope = rememberCoroutineScope()
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  val focusManager = LocalFocusManager.current
  val (usernameFocusRequester, passwordFocusRequester) = FocusRequester.createRefs()

  LaunchedEffect(Unit) {
    delay(300)
    focusManager.moveFocus(FocusDirection.Next)
  }

  Surface(
    modifier =
      modifier
        .fillMaxSize()
        .scrollable(orientation = Orientation.Vertical, state = rememberScrollState()),
    color = Color.White,
    contentColor = contentColorFor(backgroundColor = Color.DarkGray),
  ) {
    // TODO display percentage of data migration progress
    if (dataMigrationInProgress) {
      LoaderDialog(
        dialogMessage = stringResource(id = R.string.migrating_data),
        showPercentageProgress = false,
      )
    }
    if (showForgotPasswordDialog) {
      ForgotPasswordDialog(
        forgotPassword = forgotPassword,
        onDismissDialog = { showForgotPasswordDialog = false },
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
              .testTag(APP_NAME_TEXT_TAG),
        )
        Spacer(modifier = modifier.height(40.dp))
        Text(text = stringResource(R.string.username), modifier = modifier.padding(vertical = 4.dp))
        OutlinedTextField(
          value = username,
          onValueChange = onUsernameChanged,
          maxLines = 1,
          singleLine = true,
          placeholder = {
            Text(color = Color.LightGray, text = stringResource(R.string.username_sample))
          },
          modifier =
            modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .background(color = Color.Unspecified)
              .testTag(USERNAME_FIELD_TAG)
              .focusRequester(usernameFocusRequester)
              .focusProperties { next = passwordFocusRequester },
          keyboardActions =
            KeyboardActions(onDone = { focusManager.moveFocus(FocusDirection.Next) }),
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = modifier.fillMaxWidth()) {
          Text(
            text = stringResource(R.string.password),
            modifier = modifier.wrapContentWidth().padding(vertical = 4.dp),
          )
          Text(
            text = stringResource(R.string.forgot_password),
            color = MaterialTheme.colors.primary,
            style = TextStyle(textDecoration = TextDecoration.Underline),
            modifier =
              modifier.wrapContentWidth().padding(vertical = 8.dp).clickable {
                showForgotPasswordDialog = !showForgotPasswordDialog
              },
          )
        }
        OutlinedTextField(
          value = password,
          onValueChange = onPasswordChanged,
          maxLines = 1,
          singleLine = true,
          placeholder = { Text(color = Color.LightGray, text = "********") },
          visualTransformation =
            if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          modifier =
            modifier
              .fillMaxWidth()
              .onFocusEvent { event ->
                if (event.isFocused) {
                  coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
              }
              .padding(vertical = 4.dp)
              .background(color = Color.Unspecified)
              .testTag(PASSWORD_FIELD_TAG)
              .focusRequester(passwordFocusRequester),
          trailingIcon = {
            val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(imageVector = image, "", tint = Color.DarkGray)
            }
          },
          keyboardActions =
            KeyboardActions(
              onDone = {
                focusManager.clearFocus()
                onLoginButtonClicked()
              },
            ),
        )
        Text(
          fontSize = 14.sp,
          color = MaterialTheme.colors.error,
          text =
            when (loginErrorState) {
              LoginErrorState.UNKNOWN_HOST ->
                stringResource(
                  id = R.string.login_error,
                  stringResource(R.string.login_call_fail_error_message),
                )
              LoginErrorState.INVALID_CREDENTIALS ->
                stringResource(
                  id = R.string.login_error,
                  stringResource(R.string.invalid_login_credentials),
                )
              null -> ""
              LoginErrorState.MULTI_USER_LOGIN_ATTEMPT ->
                stringResource(
                  id = R.string.login_error,
                  stringResource(R.string.multi_user_login_attempt),
                )
              LoginErrorState.ERROR_FETCHING_USER ->
                stringResource(
                  id = R.string.login_error,
                  stringResource(
                    org.smartregister.fhircore.quest.R.string.error_fetching_user_details,
                  ),
                )
              LoginErrorState.INVALID_OFFLINE_STATE ->
                stringResource(
                  id = R.string.login_error,
                  stringResource(R.string.invalid_offline_login_state),
                )
            },
          modifier =
            modifier
              .wrapContentWidth()
              .padding(vertical = 10.dp)
              .align(Alignment.Start)
              .testTag(LOGIN_ERROR_TEXT_TAG),
        )
        Spacer(modifier = modifier.height(0.dp))
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
          Button(
            enabled = !showProgressBar && username.isNotEmpty() && password.isNotEmpty(),
            colors =
              ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                disabledContentColor =
                  if (applicationConfiguration.useDarkTheme) {
                    LoginFieldBackgroundColor
                  } else {
                    Color.Gray
                  },
                contentColor = Color.White,
              ),
            onClick = onLoginButtonClicked,
            modifier =
              modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .testTag(LOGIN_BUTTON_TAG),
            elevation = null,
          ) {
            Text(
              text = if (!showProgressBar) stringResource(id = R.string.login_text) else "",
              modifier = modifier.padding(8.dp),
            )
          }
          if (showProgressBar) {
            CircularProgressIndicator(
              modifier = modifier.align(Alignment.Center).size(18.dp),
              strokeWidth = 1.6.dp,
              color = Color.White,
            )
          }
        }
      }
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        Column {
          Text(
            text = stringResource(id = R.string.powered_by),
            modifier = modifier.wrapContentWidth().padding(vertical = 8.dp).align(Alignment.Start),
            fontWeight = FontWeight.Light,
          )
          Image(
            painter = painterResource(id = R.drawable.ic_opensrplogo),
            contentDescription = stringResource(id = R.string.app_logo),
            modifier = modifier.align(Alignment.CenterHorizontally).requiredHeight(32.dp),
          )
        }

        Text(
          fontSize = 16.sp,
          text = stringResource(id = R.string.app_version, versionCode, versionName),
          modifier = modifier.wrapContentWidth().padding(bottom = 8.dp).testTag(LOGIN_FOOTER),
          fontWeight = FontWeight.Light,
        )
      }
    }
  }
}

@Composable
fun ForgotPasswordDialog(
  forgotPassword: () -> Unit,
  onDismissDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismissDialog,
    title = {
      Text(
        text = stringResource(R.string.forgot_password_title),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )
    },
    text = {
      Text(text = stringResource(R.string.call_supervisor, "012-3456-789"), fontSize = 16.sp)
    },
    buttons = {
      Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        Text(
          text = stringResource(R.string.cancel),
          modifier = modifier.padding(horizontal = 10.dp).clickable { onDismissDialog() },
        )
        Text(
          color = MaterialTheme.colors.primary,
          text = stringResource(R.string.dial_number),
          modifier =
            modifier.padding(horizontal = 10.dp).clickable {
              onDismissDialog()
              forgotPassword()
            },
        )
      }
    },
    modifier = Modifier.testTag(PASSWORD_FORGOT_DIALOG),
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun LoginScreenPreview() {
  LoginPage(
    applicationConfiguration =
      ApplicationConfiguration(
        appId = "appId",
        configType = "application",
        appTitle = "FHIRCore App",
      ),
    username = "",
    onUsernameChanged = {},
    password = "",
    onPasswordChanged = {},
    forgotPassword = {},
    onLoginButtonClicked = {},
    appVersionPair = Pair(1, "0.0.1"),
    dataMigrationInProgress = true,
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun LoginScreenPreviewDarkMode() {
  LoginPage(
    applicationConfiguration =
      ApplicationConfiguration(
        appId = "appId",
        configType = "application",
        appTitle = "FHIRCore App",
      ),
    username = "",
    onUsernameChanged = {},
    password = "",
    onPasswordChanged = {},
    forgotPassword = {},
    onLoginButtonClicked = {},
    appVersionPair = Pair(1, "0.0.1"),
    dataMigrationInProgress = false,
  )
}
