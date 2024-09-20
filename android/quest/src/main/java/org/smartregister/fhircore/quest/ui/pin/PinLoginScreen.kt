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

package org.smartregister.fhircore.quest.ui.pin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.components.PinInput
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val CIRCULAR_PROGRESS_INDICATOR = "progress_indicator"
const val PIN_LOGO_IMAGE = "pin_logo_image"

@Composable
fun PinLoginScreen(viewModel: PinViewModel) {
  val showError by viewModel.showError.observeAsState(initial = false)
  val showProgressBar by viewModel.showProgressBar.observeAsState(initial = false)
  val pinUiState = viewModel.pinUiState.value

  PinLoginPage(
    showProgressBar = showProgressBar,
    showError = showError,
    pinUiState = pinUiState,
    onMenuLoginClicked = viewModel::onMenuItemClicked,
    forgotPin = viewModel::forgotPin,
    onSetPin = viewModel::onSetPin,
    onShowPinError = viewModel::onShowPinError,
    onPinEntered = viewModel::pinLogin,
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinLoginPage(
  modifier: Modifier = Modifier,
  showProgressBar: Boolean,
  showError: Boolean,
  pinUiState: PinUiState,
  onSetPin: (CharArray) -> Unit,
  onMenuLoginClicked: (Boolean) -> Unit,
  onShowPinError: (Boolean) -> Unit,
  forgotPin: () -> Unit,
  onPinEntered: (CharArray, (Boolean) -> Unit) -> Unit,
) {
  var showMenu by remember { mutableStateOf(false) }
  var showForgotPinDialog by remember { mutableStateOf(false) }
  var newPin by remember { mutableStateOf(charArrayOf()) }
  val bringIntoViewRequester = remember { BringIntoViewRequester() }

  LaunchedEffect(Unit) { bringIntoViewRequester.bringIntoView() }

  Scaffold(
    topBar = {
      // Only show toolbar when entering pin
      if (!pinUiState.setupPin) {
        PinTopBar(
          showMenu = showMenu,
          onShowMenu = { showMenu = it },
          onMenuLoginClicked = onMenuLoginClicked,
        )
      }
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      if (showForgotPinDialog) {
        ForgotPinDialog(forgotPin = forgotPin, onDismissDialog = { showForgotPinDialog = false })
      }
      Column {
        Spacer(modifier = modifier.fillMaxHeight(0.22f))
        Column(modifier = modifier.fillMaxWidth()) {
          if (pinUiState.setupPin) {
            PinLogoSection(
              showLogo = pinUiState.showLogo,
              title = stringResource(id = R.string.set_pin),
            )
          } else {
            PinLogoSection(showLogo = pinUiState.showLogo, title = pinUiState.appName)
          }
          Text(
            text = pinUiState.message,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            modifier =
              modifier.padding(bottom = 12.dp, top = 20.dp).align(Alignment.CenterHorizontally),
          )

          PinInput(
            inputMode = pinUiState.setupPin,
            pinLength = pinUiState.pinLength,
            onPinSet = { enteredPin -> newPin = enteredPin },
            onShowPinError = onShowPinError,
            onPinEntered = onPinEntered,
          )

          // Only show error message and forgot password when not setting the pin
          if (!pinUiState.setupPin) {
            if (showProgressBar) CircularProgressBar()

            if (showError) {
              Text(
                text = stringResource(R.string.incorrect_pin_please_retry),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = DangerColor,
                modifier = modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally),
              )
            } else {
              Text(
                text = stringResource(R.string.forgot_pin),
                color = MaterialTheme.colors.primary.copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier =
                  modifier
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable { showForgotPinDialog = !showForgotPinDialog }
                    .bringIntoViewRequester(bringIntoViewRequester),
              )
            }
          } else {
            // Enable button when a new PIN of required length is entered
            Button(
              onClick = { onSetPin(newPin) },
              enabled = newPin.size == pinUiState.pinLength && !showProgressBar,
              modifier =
                modifier
                  .bringIntoViewRequester(bringIntoViewRequester)
                  .padding(top = 32.dp, end = 16.dp, start = 16.dp)
                  .fillMaxWidth(),
              colors =
                ButtonDefaults.buttonColors(
                  disabledContentColor = Color.Gray,
                  contentColor = Color.White,
                ),
              elevation = null,
            ) {
              Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                if (showProgressBar) {
                  CircularProgressIndicator(
                    modifier = modifier.size(18.dp).testTag(CIRCULAR_PROGRESS_INDICATOR),
                    strokeWidth = 1.6.dp,
                    color = Color.White,
                  )
                } else {
                  Text(text = stringResource(id = R.string.set_pin).uppercase())
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PinLogoSection(modifier: Modifier = Modifier, showLogo: Boolean, title: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
    if (showLogo) {
      Image(
        painter = painterResource(id = R.drawable.ic_app_logo),
        contentDescription = stringResource(id = R.string.app_logo),
        modifier =
          modifier
            .align(Alignment.CenterHorizontally)
            .requiredHeight(120.dp)
            .requiredWidth(140.dp)
            .testTag(PIN_LOGO_IMAGE),
      )
    }
    Text(
      text = title,
      textAlign = TextAlign.Center,
      fontWeight = FontWeight.Bold,
      fontSize = 22.sp,
      modifier = modifier.padding(8.dp),
    )
  }
}

@Composable
private fun PinTopBar(
  showMenu: Boolean,
  onShowMenu: (Boolean) -> Unit,
  onMenuLoginClicked: (Boolean) -> Unit,
) {
  TopAppBar(
    title = { Text(text = "") },
    actions = {
      IconButton(onClick = { onShowMenu(true) }) {
        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
      }
      DropdownMenu(expanded = showMenu, onDismissRequest = { onShowMenu(false) }) {
        DropdownMenuItem(onClick = { onMenuLoginClicked(false) }) {
          Text(text = stringResource(id = R.string.pin_menu_login))
        }

        DropdownMenuItem(onClick = { onMenuLoginClicked(true) }) {
          Text(text = stringResource(id = R.string.settings))
        }
      }
    },
  )
}

@Composable
fun ForgotPinDialog(
  forgotPin: () -> Unit,
  onDismissDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismissDialog,
    title = {
      Text(
        text = stringResource(R.string.forgot_pin),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
      )
    },
    text = { Text(text = stringResource(R.string.please_contact_supervisor), fontSize = 16.sp) },
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
              forgotPin()
            },
        )
      }
    },
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun PinSetupPreview() {
  PinLoginPage(
    showProgressBar = false,
    showError = false,
    pinUiState =
      PinUiState(
        message = "CHA will use this PIN to login",
        appName = "MOH eCBIS",
        setupPin = true,
        pinLength = 4,
        showLogo = true,
      ),
    onSetPin = {},
    onMenuLoginClicked = {},
    onShowPinError = {},
    forgotPin = {},
    onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun PinSetupPreviewWithProgress() {
  PinLoginPage(
    showProgressBar = true,
    showError = false,
    pinUiState =
      PinUiState(
        message = "CHA will use this PIN to login",
        appName = "MOH eCBIS",
        setupPin = true,
        pinLength = 4,
        showLogo = true,
      ),
    onSetPin = {},
    onMenuLoginClicked = {},
    onShowPinError = {},
    forgotPin = {},
    onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun PinLoginPreview() {
  PinLoginPage(
    showProgressBar = false,
    showError = false,
    pinUiState =
      PinUiState(
        message = "Enter PIN for ecbis",
        appName = "MOH eCBIS",
        setupPin = false,
        pinLength = 4,
        showLogo = true,
      ),
    onSetPin = {},
    onMenuLoginClicked = {},
    onShowPinError = {},
    forgotPin = {},
    onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
  )
}
