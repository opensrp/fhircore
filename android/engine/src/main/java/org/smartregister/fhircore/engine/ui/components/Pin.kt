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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PinInput(
  modifier: Modifier = Modifier,
  pinLength: Int,
  inputMode: Boolean = true,
  onPinChanged: (String) -> Unit
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }
  var enteredPin by remember { mutableStateOf("") }
  var currentCellIndex by remember { mutableStateOf(0) }

  // Launch keyboard and request focus on the hidden input field
  LaunchedEffect(Unit) {
    keyboard?.show()
    focusRequester.requestFocus()
  }

  BasicTextField(
    value = enteredPin,
    onValueChange = {
      if (it.length == pinLength) {
        onPinChanged(enteredPin)
      }
      enteredPin = it
      if (it.length < pinLength) {
        keyboard?.show()
      } else {
        keyboard?.hide()
      }
      currentCellIndex = enteredPin.length
    },
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
    singleLine = true,
    modifier = modifier.focusRequester(focusRequester).size(0.dp).wrapContentSize(),
  )

  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    for ((index, _) in (0 until pinLength).withIndex()) {
      PinCell(
        inputMode = inputMode,
        borderColor = if (index == currentCellIndex) InfoColor else Color.LightGray,
        number = enteredPin.getOrNull(index)?.toString() ?: "",
        backgroundColor =
          when {
            inputMode -> Color.White
            enteredPin.getOrNull(index) == null -> Color.LightGray
            enteredPin.length == pinLength -> SuccessColor
            else -> InfoColor
          }
      )
    }
  }
}

@Composable
fun PinCell(
  modifier: Modifier = Modifier,
  borderColor: Color = Color.LightGray,
  backgroundColor: Color = Color.LightGray,
  inputMode: Boolean = true,
  number: String
) {
  Box(
    modifier =
      modifier
        .padding(8.dp)
        .clip(if (inputMode) RoundedCornerShape(size = 4.dp) else CircleShape)
        .size(if (inputMode) 60.dp else 20.dp)
        .background(backgroundColor)
        .border(width = if (inputMode) 1.5.dp else 0.dp, color = borderColor),
    contentAlignment = Alignment.Center
  ) { if (inputMode) Text(text = number, textAlign = TextAlign.Center) }
}

@Composable
@Preview(showBackground = false)
private fun PinViewWithActiveInputModePreview() {
  PinInput(pinLength = 4, inputMode = true) {}
}

@Composable
@Preview(showBackground = false)
private fun PinViewWithInActiveInputModePreview() {
  PinInput(pinLength = 4, inputMode = false) {}
}
