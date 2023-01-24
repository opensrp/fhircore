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

package org.smartregister.fhircore.engine.ui.pin

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.components.PIN_INPUT_MAX_THRESHOLD
import org.smartregister.fhircore.engine.ui.components.PinView
import org.smartregister.fhircore.engine.ui.login.APP_LOGO_TAG
import org.smartregister.fhircore.engine.ui.theme.LoginButtonColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val PIN_TOOLBAR_MENU = "toolbarMenuTag"
const val PIN_TOOLBAR_MENU_BUTTON = "toolbarMenuButtonTag"
const val PIN_TOOLBAR_TITLE = "toolbarTitle"
const val PIN_TOOLBAR_MENU_ICON = "toolbarIcon"
const val PIN_TOOLBAR_MENU_LOGIN = "toolbarMenuLogin"
const val PIN_FORGOT_PIN = "forgotPin"
const val PIN_FORGOT_DIALOG = "forgotPinDialog"
const val PIN_TOOLBAR_MENU_SETTINGS = "toolbarMenuSettings"
const val PIN_SET_PIN_CONFIRM_BUTTON = "setPinConfirmButton"

@Composable
fun PinLoginScreen(viewModel: PinViewModel) {

  val showError by viewModel.showError.observeAsState(initial = false)
  val pinUiState by remember { mutableStateOf(viewModel.pinUiState.value) }

  PinLoginPage(
    onPinChanged = viewModel::onPinChanged,
    showError = showError,
    enterUserPinMessage = pinUiState.enterUserLoginMessage,
    onMenuLoginClicked = { viewModel.onMenuLoginClicked(false) },
    forgotPin = viewModel::forgotPin,
    appName = pinUiState.appName,
  )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PinLoginPage(
  modifier: Modifier = Modifier,
  onPinChanged: (String) -> Unit,
  showError: Boolean = false,
  onMenuLoginClicked: (Boolean) -> Unit,
  enterUserPinMessage: String = "",
  forgotPin: () -> Unit,
  appName: String = "",
) {

  var showMenu by remember { mutableStateOf(false) }
  var showForgotPinDialog by remember { mutableStateOf(false) }

  Surface(color = colorResource(id = R.color.white_slightly_opaque)) {
    TopAppBar(
      title = { Text(text = "", Modifier.testTag(PIN_TOOLBAR_TITLE)) },
      navigationIcon = {
        IconButton(onClick = {}) {
          Icon(
            Icons.Filled.ArrowBack,
            contentDescription = "Back arrow",
            modifier = Modifier.size(0.dp).testTag(PIN_TOOLBAR_MENU_ICON)
          )
        }
      },
      actions = {
        IconButton(
          onClick = { showMenu = !showMenu },
          modifier = Modifier.testTag(PIN_TOOLBAR_MENU_BUTTON)
        ) { Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null) }
        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          Modifier.testTag(PIN_TOOLBAR_MENU)
        ) {
          DropdownMenuItem(
            onClick = {
              showMenu = false
              onMenuLoginClicked(false)
            },
            modifier = Modifier.testTag(PIN_TOOLBAR_MENU_LOGIN)
          ) { Text(text = stringResource(id = R.string.pin_menu_login)) }
        }
      }
    )

    if (showForgotPinDialog) {
      ForgotPinDialog(forgotPin = forgotPin, onDismissDialog = { showForgotPinDialog = false })
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 70.dp)
          .wrapContentWidth(Alignment.CenterHorizontally)
    ) {
      Image(
        painter = painterResource(id = R.drawable.ic_app_logo),
        contentDescription = stringResource(id = R.string.app_logo),
        modifier =
          modifier
            .padding(top = 16.dp)
            .align(Alignment.CenterHorizontally)
            .requiredHeight(120.dp)
            .requiredWidth(140.dp)
            .testTag(APP_LOGO_TAG)
      )
      Text(
        text = appName,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        modifier = modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
      )

      Text(
        text = enterUserPinMessage,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        modifier = modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
      )

      PinView(
        pinInputLength = PIN_INPUT_MAX_THRESHOLD,
        isDotted = true,
        onPinChanged = onPinChanged,
        showError = showError
      )

      if (showError)
        Text(
          text = stringResource(R.string.incorrect_pin_please_retry),
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          color = colorResource(id = R.color.colorError),
          modifier = modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
        )

      Text(
        text = stringResource(R.string.forgot_pin),
        color = LoginButtonColor,
        fontSize = 16.sp,
        style = TextStyle(textDecoration = TextDecoration.Underline, color = LoginDarkColor),
        modifier =
          modifier
            .padding(top = 24.dp)
            .align(Alignment.CenterHorizontally)
            .testTag(PIN_FORGOT_PIN)
            .clickable { showForgotPinDialog = !showForgotPinDialog }
      )
    }
  }
}

@Composable
fun ForgotPinDialog(
  forgotPin: () -> Unit,
  onDismissDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  AlertDialog(
    onDismissRequest = onDismissDialog,
    title = {
      Text(
        text = stringResource(R.string.forgot_pin),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      )
    },
    text = { Text(text = stringResource(R.string.please_contact_supervisor), fontSize = 16.sp) },
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
              forgotPin()
            }
        )
      }
    },
    modifier = Modifier.testTag(PIN_FORGOT_DIALOG)
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun PinLoginPreview() {
  PinLoginPage(
    onPinChanged = {},
    showError = false,
    onMenuLoginClicked = {},
    forgotPin = {},
    appName = "anc"
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun PinLoginErrorPreview() {
  PinLoginPage(
    onPinChanged = {},
    showError = true,
    onMenuLoginClicked = {},
    forgotPin = {},
    appName = "ecbis"
  )
}
