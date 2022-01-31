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

package org.smartregister.fhircore.anc.ui.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.ui.family.details.TOOLBAR_MENU
import org.smartregister.fhircore.anc.ui.family.details.TOOLBAR_MENU_BUTTON
import org.smartregister.fhircore.anc.ui.family.details.TOOLBAR_TITLE
import org.smartregister.fhircore.engine.ui.components.PinView
import org.smartregister.fhircore.engine.ui.login.APP_LOGO_TAG
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val TOOLBAR_MENU_SETTINGS = "toolbarMenuSettings"
const val SET_PIN_CONFIRM_BUTTON = "SET_PIN_CONFIRM_BUTTON"

@Composable
fun OtpSetupScreen(viewModel: OtpViewModel) {

  val inputPin by viewModel.pin.observeAsState(initial = "")
  val enableSetPin by viewModel.enableSetPin.observeAsState(initial = false)

  OtpSetupPage(
    onPinChanged = viewModel::onPinChanged,
    inputPin = inputPin,
    setPinEnabled = enableSetPin ?: false,
    onPinConfirmed = viewModel::onPinConfirmed,
    onMenuSettingClicked = { viewModel.onMenuSettingClicked() },
  )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpSetupPage(
  modifier: Modifier = Modifier,
  onPinChanged: (String) -> Unit,
  inputPin: String,
  setPinEnabled: Boolean = false,
  onPinConfirmed: () -> Unit,
  onMenuSettingClicked: () -> Unit,
) {

  var showMenu by remember { mutableStateOf(false) }

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
              onMenuSettingClicked()
            },
            modifier = Modifier.testTag(TOOLBAR_MENU_SETTINGS)
          ) { Text(text = stringResource(id = R.string.settings)) }
        }
      }
    )

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
        text = stringResource(R.string.set_pin),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        modifier = modifier.padding(top = 20.dp).align(Alignment.CenterHorizontally)
      )

      Text(
        text = stringResource(R.string.set_pin_message),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        modifier =
          modifier.padding(horizontal = 16.dp, vertical = 16.dp).align(Alignment.CenterHorizontally)
      )

      PinView(otpInputLength = 4, onPinChanged = onPinChanged, inputPin = inputPin)

      Button(
        enabled = setPinEnabled,
        onClick = onPinConfirmed,
        modifier = Modifier.fillMaxWidth().padding(top = 30.dp).testTag(SET_PIN_CONFIRM_BUTTON)
      ) {
        Text(
          color = Color.White,
          text = stringResource(id = R.string.set_pin),
          modifier = Modifier.padding(8.dp)
        )
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun OtpSetupPreview() {
  OtpSetupPage(
    onPinChanged = {},
    onPinConfirmed = {},
    inputPin = "",
    setPinEnabled = false,
    onMenuSettingClicked = {}
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun OtpSetupFilledPreview() {
  OtpSetupPage(
    onPinChanged = {},
    onPinConfirmed = {},
    inputPin = "1234",
    setPinEnabled = true,
    onMenuSettingClicked = {}
  )
}
