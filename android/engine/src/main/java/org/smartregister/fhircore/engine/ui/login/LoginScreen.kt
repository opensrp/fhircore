package org.smartregister.fhircore.engine.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
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

  Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    Spacer(modifier = modifier.height(60.dp))
    Image(
      painter = painterResource(id = R.drawable.ic_default_logo),
      contentDescription = stringResource(id = R.string.app_logo),
      modifier = modifier.align(Alignment.CenterHorizontally).height(120.dp).width(120.dp)
    )
    Text(
      color = MaterialTheme.colors.primary,
      text = viewConfiguration.applicationName,
      fontWeight = FontWeight.Bold,
      fontSize = 32.sp,
      modifier =
        modifier.wrapContentWidth().padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
    )

    Text(
      fontSize = 16.sp,
      text = stringResource(id = R.string.app_version, viewConfiguration.applicationVersion),
      modifier = modifier.wrapContentWidth().padding(0.dp).align(Alignment.CenterHorizontally)
    )
    Spacer(modifier = modifier.height(40.dp))

    TextField(
      value = username,
      onValueChange = onUsernameChanged,
      label = {
        Text(
          text = stringResource(R.string.username_input_hint),
          modifier = modifier.padding(vertical = 4.dp)
        )
      },
      modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
    TextField(
      value = password,
      onValueChange = onPasswordChanged,
      label = {
        Text(
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
        IconButton(onClick = { showPassword = !showPassword }) { Icon(imageVector = image, "") }
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
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
        onClick = onLoginButtonClicked,
        modifier = modifier.fillMaxWidth()
      ) { Text(text = stringResource(id = R.string.login_text), modifier = modifier.padding(8.dp)) }
      if (showProgressBar) {
        CircularProgressBar(modifier = modifier.matchParentSize().padding(4.dp))
      }
    }

    Spacer(modifier = modifier.height(40.dp))
    Text(
      text = stringResource(id = R.string.powered_by),
      modifier =
        modifier.wrapContentWidth().padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
    )

    Image(
      painter = painterResource(id = R.drawable.ic_opensrp_logo),
      contentDescription = stringResource(id = R.string.app_logo),
      modifier = modifier.align(Alignment.CenterHorizontally).requiredHeight(40.dp)
    )
    Spacer(modifier = modifier.height(60.dp))
  }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
  LoginPage(loginViewConfigurationOf(), "", {}, "", {}, {})
}
