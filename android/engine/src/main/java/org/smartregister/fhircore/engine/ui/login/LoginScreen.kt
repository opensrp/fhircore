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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.theme.LoginBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.LoginButtonColor
import org.smartregister.fhircore.engine.ui.theme.LoginFieldBackgroundColor

@Composable
fun LoginScreen(loginViewModel: LoginViewModel) {

  val viewConfiguration by loginViewModel.loginViewConfiguration.observeAsState(
    loginViewConfigurationOf()
  )
  val username by loginViewModel.username.observeAsState("")
  val password by loginViewModel.password.observeAsState("")
  val loginError by loginViewModel.loginError.observeAsState("")
  val showProgressBar by loginViewModel.showProgressBar.observeAsState(false)

  LoginPage(
    viewConfiguration = viewConfiguration,
    username = username,
    onUsernameChanged = { loginViewModel.onUsernameUpdated(it) },
    password = password,
    onPasswordChanged = { loginViewModel.onPasswordUpdated(it) },
    onLoginButtonClicked = { loginViewModel.attemptRemoteLogin() },
    loginError = loginError,
    showProgressBar = showProgressBar
  )
}

@Composable
fun LoginPage(
  viewConfiguration: LoginViewConfiguration,
  username: String,
  onUsernameChanged: (String) -> Unit,
  password: String,
  onPasswordChanged: (String) -> Unit,
  onLoginButtonClicked: () -> Unit,
  modifier: Modifier = Modifier,
  loginError: String = "",
  showProgressBar: Boolean = false
) {
  var showPassword by remember { mutableStateOf(false) }
  val backgroundColor = if (viewConfiguration.darkMode) LoginBackgroundColor else Color.White
  val contentColor = if (viewConfiguration.darkMode) Color.White else Color.Black
  val textFieldBackgroundColor =
    if (viewConfiguration.darkMode) LoginFieldBackgroundColor else Color.LightGray
  Surface(
    modifier = modifier.fillMaxSize(),
    color = backgroundColor,
    contentColor = contentColorFor(backgroundColor = contentColor)
  ) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
      Column(
        modifier = modifier.weight(1f).padding(4.dp),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          color =
            if (viewConfiguration.darkMode) Color.White else MaterialTheme.colors.primaryVariant,
          text = viewConfiguration.applicationName,
          fontWeight = FontWeight.Bold,
          fontSize = 32.sp,
          modifier =
            modifier.wrapContentWidth().padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = modifier.height(80.dp))
        TextField(
          colors =
            TextFieldDefaults.textFieldColors(
              backgroundColor = textFieldBackgroundColor,
              textColor = contentColor
            ),
          value = username,
          onValueChange = onUsernameChanged,
          label = {
            Text(
              color = contentColor,
              text = stringResource(R.string.username_input_hint),
              modifier = modifier.padding(vertical = 4.dp)
            )
          },
          modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
        TextField(
          value = password,
          colors =
            TextFieldDefaults.textFieldColors(
              backgroundColor = textFieldBackgroundColor,
              textColor = contentColor
            ),
          onValueChange = onPasswordChanged,
          label = {
            Text(
              color = contentColor,
              text = stringResource(R.string.password_input_hint),
              modifier = modifier.padding(vertical = 4.dp)
            )
          },
          visualTransformation =
            if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
          trailingIcon = {
            val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(imageVector = image, "", tint = contentColor)
            }
          }
        )
        Spacer(modifier = modifier.height(10.dp))
        if (loginError.isNotEmpty()) {
          Text(
            fontSize = 14.sp,
            color = MaterialTheme.colors.error,
            text = stringResource(id = R.string.login_error, loginError),
            modifier = modifier.wrapContentWidth().padding(0.dp).align(Alignment.Start)
          )
        }
        Spacer(modifier = modifier.height(40.dp))
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
          Button(
            enabled = !showProgressBar && username.isNotEmpty() && password.isNotEmpty(),
            colors =
              ButtonDefaults.buttonColors(
                backgroundColor = LoginButtonColor,
                disabledBackgroundColor =
                  if (viewConfiguration.darkMode) LoginFieldBackgroundColor else Color.LightGray
              ),
            onClick = onLoginButtonClicked,
            modifier = modifier.fillMaxWidth()
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
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
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
          text = stringResource(id = R.string.app_version, viewConfiguration.applicationVersion),
          modifier = modifier.wrapContentWidth().padding(0.dp)
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
  LoginPage(loginViewConfigurationOf(), "", {}, "", {}, {})
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreviewDarkMode() {
  LoginPage(loginViewConfigurationOf().apply { darkMode = true }, "", {}, "", {}, {})
}
