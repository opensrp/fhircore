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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.smartregister.fhircore.engine.ui.components.PinView
import org.smartregister.fhircore.engine.ui.login.APP_LOGO_TAG
import org.smartregister.fhircore.engine.ui.theme.LoginButtonColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
fun OtpLoginScreen(viewModel: OtpViewModel) {
  val inputPin by remember { mutableStateOf(viewModel.pin.value) }
  val enableSetPin by remember { mutableStateOf(viewModel.enableSetPin.value) }

  OtpLoginPage(
    onPinChanged = viewModel::onPinChanged,
    inputPin = inputPin ?: "",
    setPinEnabled = enableSetPin ?: false,
    onPinConfirmed = viewModel::onPinConfirmed
  )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpLoginPage(
  modifier: Modifier = Modifier,
  onPinChanged: (String) -> Unit,
  inputPin: String,
  setPinEnabled: Boolean = false,
  onPinConfirmed: () -> Unit
) {
  Surface(color = colorResource(id = R.color.white_slightly_opaque)) {
    Column(
      modifier =
        Modifier.fillMaxSize().padding(all = 16.dp).wrapContentWidth(Alignment.CenterHorizontally)
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
        modifier =
          modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
      )

      Text(
        text = stringResource(R.string.enter_pin_w4vv01),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        modifier =
          modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
      )

      PinView(otpInputLength = 4, isDotted = true, onPinChanged = onPinChanged)

      Text(
        text = stringResource(R.string.forgot_pin),
        color = LoginButtonColor,
        fontSize = 16.sp,
        style = TextStyle(textDecoration = TextDecoration.Underline, color = LoginDarkColor),
        modifier = modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally)
//        modifier.wrapContentWidth().padding(vertical = 8.dp).clickable {
//          showForgotPasswordDialog = !showForgotPasswordDialog
//        }
      )

//      Button(
//        enabled = setPinEnabled,
//        onClick = onPinConfirmed,
//        modifier = Modifier.fillMaxWidth().padding(top = 40.dp).testTag(SET_PIN_CONFIRM_BUTTON)
//      ) {
//        Text(
//          color = Color.White,
//          text = stringResource(id = R.string.set_pin),
//          modifier = Modifier.padding(8.dp)
//        )
//      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun OtpLoginPreview() {
  OtpLoginPage(onPinChanged = {}, onPinConfirmed = {}, inputPin = "", setPinEnabled = false)
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun OtpLoginFilledPreview() {
  OtpLoginPage(onPinChanged = {}, onPinConfirmed = {}, inputPin = "1234", setPinEnabled = true)
}
