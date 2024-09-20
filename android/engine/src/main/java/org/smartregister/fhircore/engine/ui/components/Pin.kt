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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.clearPasswordInMemory
import org.smartregister.fhircore.engine.util.safePlus
import org.smartregister.fhircore.engine.util.safeRemoveLast

const val PIN_CELL_TEST_TAG = "pinCell"
const val PIN_CELL_TEXT_TEST_TAG = "pinCellText"
const val PIN_TEXT_FIELD_TEST_TAG = "pinTextField"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PinInput(
  modifier: Modifier = Modifier,
  pinLength: Int,
  inputMode: Boolean = true,
  onPinSet: (CharArray) -> Unit,
  onShowPinError: (Boolean) -> Unit,
  onPinEntered: (CharArray, (Boolean) -> Unit) -> Unit,
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val (focusRequester) = FocusRequester.createRefs()
  var enteredPin by remember { mutableStateOf(charArrayOf()) }
  var nextCellIndex by remember { mutableIntStateOf(0) }
  var isValidPin by remember { mutableStateOf<Boolean?>(null) }
  val isFocused by remember { mutableStateOf(false) }

  // Launch keyboard and request focus
  SideEffect {
    if (!isFocused) {
      focusRequester.requestFocus()
      keyboard?.show()
    }
  }

  // Hidden input field
  BasicTextField(
    value = enteredPin.joinToString(""),
    onValueChange = {
      when {
        it.length == pinLength -> {
          enteredPin = enteredPin.safePlus(it.last())
          nextCellIndex = enteredPin.size
          keyboard?.hide()

          if (inputMode) {
            onPinSet(enteredPin)
          } else {
            onPinEntered(enteredPin) { isValid ->
              isValidPin = isValid
              if (!isValid) {
                keyboard?.show()
                onShowPinError(true)
              }
            }
          }
        }
        it.length < pinLength -> {
          isValidPin = null
          keyboard?.show()
          enteredPin =
            if (it.length < enteredPin.size) {
              enteredPin.safeRemoveLast()
            } else {
              enteredPin.safePlus(it.last())
            }
          nextCellIndex = enteredPin.size
          onPinSet(enteredPin)
          onShowPinError(false)
        }
        else -> return@BasicTextField
      }
    },
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
    singleLine = true,
    modifier = modifier.focusRequester(focusRequester).size(0.dp).testTag(PIN_TEXT_FIELD_TEST_TAG),
    maxLines = 1,
  )

  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    for (index in 0 until pinLength) {
      val backgroundColor =
        when {
          inputMode -> Color.White
          enteredPin.getOrNull(index) == null -> Color.LightGray
          enteredPin.size == pinLength && isValidPin == true -> SuccessColor
          enteredPin.size == pinLength && isValidPin == false -> DangerColor
          else -> InfoColor
        }
      PinCell(
        inputMode = inputMode,
        borderColor =
          when {
            inputMode -> if (index == nextCellIndex) InfoColor else Color.LightGray
            else -> backgroundColor
          },
        number = enteredPin.getOrNull(index)?.toString() ?: "",
        backgroundColor = backgroundColor,
        onPinCellClick = {
          focusRequester.requestFocus()
          keyboard?.show()
          clearPasswordInMemory(enteredPin)
          onShowPinError(false)
        },
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
  number: String,
  onPinCellClick: () -> Unit,
) {
  Box(
    modifier =
      modifier
        .testTag(PIN_CELL_TEST_TAG)
        .padding(4.dp)
        .size(
          width = if (inputMode) 48.dp else 18.dp,
          height = if (inputMode) 56.dp else 18.dp,
        )
        .clip(if (inputMode) RoundedCornerShape(size = 6.dp) else CircleShape)
        .background(backgroundColor)
        .border(
          width = if (inputMode) 1.dp else 0.dp,
          color = borderColor,
          shape = RoundedCornerShape(8.dp),
        )
        .clickable { onPinCellClick() },
    contentAlignment = Alignment.Center,
  ) {
    if (inputMode) {
      Text(
        modifier = modifier.testTag(PIN_CELL_TEXT_TEST_TAG),
        text = number,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun PinViewWithActiveInputModePreview() {
  PinInput(
    pinLength = 4,
    inputMode = true,
    onPinSet = {},
    onShowPinError = {},
    onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun PinViewWithInActiveInputModePreview() {
  PinInput(
    pinLength = 4,
    inputMode = false,
    onPinSet = {},
    onShowPinError = {},
    onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
  )
}
