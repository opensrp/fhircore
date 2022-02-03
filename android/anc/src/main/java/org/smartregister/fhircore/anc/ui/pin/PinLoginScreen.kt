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

package org.smartregister.fhircore.anc.ui.pin

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.ui.family.details.TOOLBAR_MENU
import org.smartregister.fhircore.anc.ui.family.details.TOOLBAR_MENU_BUTTON
import org.smartregister.fhircore.anc.ui.family.details.TOOLBAR_TITLE
import org.smartregister.fhircore.engine.ui.components.PinView
import org.smartregister.fhircore.engine.ui.login.APP_LOGO_TAG
import org.smartregister.fhircore.engine.ui.theme.LoginButtonColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val TOOLBAR_MENU_ICON = "toolbarIcon"
const val TOOLBAR_MENU_LOGIN = "toolbarMenuLogin"
const val FORGOT_PIN = "forgot_pin"

@Composable
fun PinLoginScreen(viewModel: PinViewModel) {

  val showError by viewModel.showError.observeAsState(initial = false)

  PinLoginPage(
    onPinChanged = viewModel::onPinChanged,
    showError = showError,
    onMenuLoginClicked = { viewModel.onMenuLoginClicked() },
    forgotPin = viewModel::forgotPin,
  )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PinLoginPage(
  modifier: Modifier = Modifier,
  onPinChanged: (String) -> Unit,
  showError: Boolean = false,
  onMenuLoginClicked: () -> Unit,
  forgotPin: () -> Unit
) {

  var showMenu by remember { mutableStateOf(false) }
  var showForgotPinDialog by remember { mutableStateOf(false) }

  Surface(color = colorResource(id = R.color.white_slightly_opaque)) {
    TopAppBar(
      title = { Text(text = "", Modifier.testTag(TOOLBAR_TITLE)) },
      navigationIcon = {
        IconButton(onClick = {}) {
          Icon(
            Icons.Filled.ArrowBack,
            contentDescription = "Back arrow",
            modifier = Modifier.size(0.dp).testTag(TOOLBAR_MENU_ICON)
          )
        }
      },
      actions = {
        IconButton(
          onClick = { showMenu = !showMenu },
          modifier = Modifier.testTag(TOOLBAR_MENU_BUTTON)
        ) { Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null) }
        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          Modifier.testTag(TOOLBAR_MENU)
        ) {
          DropdownMenuItem(
            onClick = {
              showMenu = false
              onMenuLoginClicked()
            },
            modifier = Modifier.testTag(TOOLBAR_MENU_LOGIN)
          ) { Text(text = stringResource(id = R.string.otp_menu_login)) }
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
        painter = painterResource(id = R.drawable.ic_liberia),
        contentDescription = stringResource(id = R.string.app_logo),
        modifier =
          modifier
            .padding(top = 16.dp)
            .align(Alignment.CenterHorizontally)
            .requiredHeight(120.dp)
            .requiredWidth(140.dp)
            .testTag(APP_LOGO_TAG),
      )
      Text(
        text = stringResource(R.string.app_name_ecbis),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        modifier = modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
      )

      Text(
        text = stringResource(R.string.enter_pin_w4vv01),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        modifier = modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
      )

      PinView(
        pinInputLength = 4,
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
          modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally).clickable {
            showForgotPinDialog = !showForgotPinDialog
          }
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
    modifier = Modifier.testTag(FORGOT_PIN),
    onDismissRequest = onDismissDialog,
    title = {
      Text(
        text = stringResource(org.smartregister.fhircore.engine.R.string.forgot_password_title),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      )
    },
    text = {
      Text(
        text =
          stringResource(
            org.smartregister.fhircore.engine.R.string.call_supervisor,
            "012-3456-789"
          ),
        fontSize = 16.sp
      )
    },
    buttons = {
      Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.End
      ) {
        Text(
          text = stringResource(org.smartregister.fhircore.engine.R.string.cancel),
          modifier = modifier.padding(horizontal = 10.dp).clickable { onDismissDialog() }
        )
        Text(
          color = MaterialTheme.colors.primary,
          text = stringResource(org.smartregister.fhircore.engine.R.string.dial_number),
          modifier =
            modifier.padding(horizontal = 10.dp).clickable {
              onDismissDialog()
              forgotPin()
            }
        )
      }
    }
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PinLoginPreview() {
  PinLoginPage(onPinChanged = {}, showError = false, onMenuLoginClicked = {}, forgotPin = {})
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PinLoginErrorPreview() {
  PinLoginPage(onPinChanged = {}, showError = true, onMenuLoginClicked = {}, forgotPin = {})
}
