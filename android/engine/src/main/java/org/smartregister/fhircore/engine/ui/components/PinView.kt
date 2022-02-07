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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R

const val PIN_VIEW = "pin_view"
const val PIN_VIEW_CELL = "pin_view_cell"
const val PIN_VIEW_CELL_DOTTED = "pin_view_cell_dotted"
const val PIN_VIEW_CELL_TEXT = "pin_view_cell_text"
const val PIN_VIEW_ERROR = "pin_view_error"

@ExperimentalComposeUiApi
@Composable
fun PinView(
  pinInputLength: Int = 4,
  onPinChanged: (String) -> Unit = {},
  inputPin: String = "",
  isDotted: Boolean = false,
  showError: Boolean = false,
  modifier: Modifier = Modifier
) {
  val (editValue, setEditValue) = remember { mutableStateOf(inputPin) }
  val pinLength = remember { pinInputLength }
  val focusRequester = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current
  TextField(
    value = editValue,
    onValueChange = {
      if (it.length <= pinLength) {
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
    (0 until pinLength).map { index ->
      PinCell(
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
fun PinCell(
  modifier: Modifier = Modifier,
  value: String,
  isCursorVisible: Boolean = false,
  isDotted: Boolean = false,
  showError: Boolean = false,
) {
  val scope = rememberCoroutineScope()
  val (cursorSymbol, setCursorSymbol) = remember { mutableStateOf("") }
  var borderColor = colorResource(id = R.color.darkGrayText)
  var dottedBg = colorResource(id = R.color.darkGrayText)
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
  if (value.length == 4) {
    dottedBg = colorResource(id = R.color.colorSuccess)
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
      Card(
        modifier = Modifier.size(30.dp).align(Alignment.Center).testTag(PIN_VIEW_CELL_DOTTED),
        elevation = 1.dp,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(width = 1.dp, color = borderColor),
        backgroundColor = dottedBg
      ) {
        Text(
          text = if (isCursorVisible) cursorSymbol else "",
          fontSize = 18.sp,
          style = MaterialTheme.typography.body1,
          modifier = Modifier.wrapContentSize().align(Alignment.Center).testTag(PIN_VIEW_CELL_TEXT)
        )
      }
      //      }
    } else {
      Card(
        modifier = Modifier.fillMaxSize().align(Alignment.Center).testTag(PIN_VIEW_CELL),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 1.dp, color = borderColor),
        backgroundColor = colorResource(id = R.color.white)
      ) {
        Text(
          text = if (isCursorVisible) cursorSymbol else value,
          style = MaterialTheme.typography.body1,
          modifier = Modifier.wrapContentSize().align(Alignment.Center).testTag(PIN_VIEW_CELL_TEXT)
        )
      }
    }
  }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun PinViewPreview() {
  Surface(modifier = Modifier.padding(24.dp)) { PinView() }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun PinViewDottedPreview() {
  Surface(modifier = Modifier.padding(24.dp)) { PinView(isDotted = true) }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun PinViewErrorPreview() {
  Surface(modifier = Modifier.padding(24.dp)) { PinView(showError = true) }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun PinViewDottedErrorPreview() {
  Surface(modifier = Modifier.padding(24.dp)) { PinView(isDotted = true, showError = true) }
}
