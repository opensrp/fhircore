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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R

const val PIN_VIEW = "pin_view"

@ExperimentalComposeUiApi
@Composable
fun PinView(
  otpInputLength: Int = 4,
  onPinChanged: (String) -> Unit = {},
  inputPin: String = "",
  isDotted: Boolean = false,
  showError: Boolean = false
) {
  val (editValue, setEditValue) = remember { mutableStateOf(inputPin) }
  val otpLength = remember { otpInputLength }
  val focusRequester = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current
  TextField(
    value = editValue,
    onValueChange = {
      if (it.length <= otpLength) {
        setEditValue(it)
        onPinChanged(it)
      }
      if (it.length < 4) {
        keyboard?.show()
      } else {
        keyboard?.hide()
      }
    },
    modifier = Modifier.size(0.dp).focusRequester(focusRequester),
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
  )
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .testTag(PIN_VIEW)
        .background(color = colorResource(id = R.color.cardview_light_background)),
    horizontalArrangement = Arrangement.Center
  ) {
    (0 until otpLength).map { index ->
      OtpCell(
        modifier =
          Modifier.size(40.dp).clickable {
            focusRequester.requestFocus()
            keyboard?.show()
          },
        value = editValue.getOrNull(index)?.toString() ?: "",
        isCursorVisible = editValue.length == index,
        isDotted = isDotted,
        showError = showError
      )
      Spacer(modifier = Modifier.size(8.dp))
    }
  }
}

@Composable
fun OtpCell(
  modifier: Modifier,
  value: String,
  isCursorVisible: Boolean = false,
  isDotted: Boolean = false,
  showError: Boolean = false,
) {
  val scope = rememberCoroutineScope()
  val (cursorSymbol, setCursorSymbol) = remember { mutableStateOf("") }
  var borderColor = colorResource(id = R.color.darkGrayText)
  var dottedBg = colorResource(id = R.color.darkGrayText)
  if (value.length == 4) {
    dottedBg = colorResource(id = R.color.colorSuccess)
  }
  if (showError) {
    borderColor = colorResource(id = R.color.colorError)
    dottedBg = colorResource(id = R.color.colorErrorDull)
  } else if (isCursorVisible) {
    borderColor = colorResource(id = R.color.colorPrimaryLight)
    dottedBg = colorResource(id = R.color.white)
  } else if (value.isEmpty()) {
    borderColor = colorResource(id = R.color.light_gray)
    dottedBg = colorResource(id = R.color.light_gray)
  }
  LaunchedEffect(key1 = cursorSymbol, isCursorVisible) {
    if (isCursorVisible) {
      scope.launch {
        delay(350)
        setCursorSymbol(if (cursorSymbol.isEmpty()) "|" else "")
      }
    }
  }

  Box(modifier = modifier) {
    if (isDotted) {
      //      if (isCursorVisible) {
      //        Text(
      //          text = if (value.isEmpty()) cursorSymbol else value,
      //          fontSize = 18.sp,
      //          style = MaterialTheme.typography.body1,
      //          modifier = Modifier.wrapContentSize().align(Alignment.Center)
      //        )
      //      } else {
      Card(
        modifier = Modifier.size(30.dp).align(Alignment.Center),
        elevation = 1.dp,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(width = 1.dp, color = borderColor),
        backgroundColor = dottedBg
      ) {
        Text(
          text = if (isCursorVisible) cursorSymbol else "",
          fontSize = 18.sp,
          style = MaterialTheme.typography.body1,
          modifier = Modifier.wrapContentSize().align(Alignment.Center)
        )
      }
      //      }
    } else {
      Card(
        modifier = Modifier.fillMaxSize().align(Alignment.Center),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 1.dp, color = borderColor),
        backgroundColor = colorResource(id = R.color.white)
      ) {
        Text(
          text = if (isCursorVisible) cursorSymbol else value,
          style = MaterialTheme.typography.body1,
          modifier = Modifier.wrapContentSize().align(Alignment.Center)
        )
      }
    }
  }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun OtpViewPreview() {
  Surface(modifier = Modifier.padding(24.dp)) { PinView() }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun OtpViewDottedPreview() {
  Surface(modifier = Modifier.padding(24.dp)) { PinView(isDotted = true) }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun OtpViewErrorPreview() {
  Surface(modifier = Modifier.padding(24.dp)) { PinView(showError = true) }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun OtpViewDottedErrorPreview() {
  Surface(modifier = Modifier.padding(24.dp)) { PinView(isDotted = true, showError = true) }
}

@Composable
fun OTPTextFields(modifier: Modifier = Modifier, length: Int, onFilled: (code: String) -> Unit) {
  var code: List<Char> by remember { mutableStateOf(listOf()) }
  val focusRequesters: List<FocusRequester> = remember {
    val temp = mutableListOf<FocusRequester>()
    repeat(length) { temp.add(FocusRequester()) }
    temp
  }

  Row(modifier = Modifier.height(50.dp)) {
    (0 until length).forEach { index ->
      OutlinedTextField(
        modifier =
          Modifier.width(50.dp).height(50.dp).focusOrder(focusRequester = focusRequesters[index]) {
            focusRequesters[index + 1].requestFocus()
          },
        textStyle =
          MaterialTheme.typography.body2.copy(textAlign = TextAlign.Center, color = Color.Black),
        singleLine = true,
        value = code.getOrNull(index = index)?.takeIf { it.isDigit() }?.toString() ?: "",
        onValueChange = { value: String ->
          if (focusRequesters[index].freeFocus()) {
            val temp = code.toMutableList()
            if (value == "") {
              if (temp.size > index) {
                temp.removeAt(index = index)
                code = temp
                focusRequesters.getOrNull(index - 1)?.requestFocus()
              }
            } else {
              if (code.size > index) {
                temp[index] = value.getOrNull(0) ?: ' '
              } else {
                temp.add(value.getOrNull(0) ?: ' ')
                code = temp
                focusRequesters.getOrNull(index + 1)?.requestFocus()
                  ?: onFilled(code.joinToString(separator = ""))
              }
            }
          }
        },
        keyboardOptions =
          KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
          ),
        visualTransformation = PasswordVisualTransformation()
      )

      Spacer(modifier = Modifier.width(15.dp))
    }
  }
}
