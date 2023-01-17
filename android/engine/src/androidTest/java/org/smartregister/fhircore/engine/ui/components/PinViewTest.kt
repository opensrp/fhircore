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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class PinViewTest {

  @get:Rule val composeRule = createComposeRule()

  @ExperimentalComposeUiApi
  @Test
  fun testPinView() {
    composeRule.setContent { PinView(inputPin = "1234", showError = true) }
    composeRule.onNodeWithTag(PIN_VIEW).assertExists()
    composeRule.onNodeWithTag(PIN_VIEW_INPUT_TEXT_FIELD).assertExists()
    composeRule.onNodeWithText("1234").assertExists()
  }

  @ExperimentalComposeUiApi
  @Test
  fun testPinCell() {
    composeRule.setContent {
      PinCell(
        indexValue = "3",
        fullEditValue = "123",
        isCursorVisible = false,
        isDotted = false,
        showError = true
      )
    }
    composeRule.onNodeWithTag(PIN_VIEW_CELL).assertExists()
    composeRule.onNodeWithTag(PIN_VIEW_CELL_TEXT).assertExists().assertTextEquals("3")
  }

  @ExperimentalComposeUiApi
  @Test
  fun testPinCellDotted() {
    composeRule.setContent { PinCell(isDotted = true, indexValue = "3", fullEditValue = "123") }
    composeRule.onNodeWithTag(PIN_VIEW_CELL_DOTTED).assertExists()
    composeRule.onNodeWithTag(PIN_VIEW_CELL_TEXT).assertExists()
  }

  @ExperimentalComposeUiApi
  @Test
  fun testPinCellViewError() {
    composeRule.setContent {
      PinCell(isDotted = false, indexValue = "4", fullEditValue = "1234", showError = true)
    }
    composeRule.onNodeWithTag(PIN_VIEW_CELL).assertExists()
    composeRule.onNodeWithTag(PIN_VIEW_CELL_TEXT).assertExists().assertTextEquals("4")
  }

  @ExperimentalComposeUiApi
  @Test
  fun testPinCellViewDottedError() {
    composeRule.setContent {
      PinCell(isDotted = true, indexValue = "3", fullEditValue = "123", showError = true)
    }
    composeRule.onNodeWithTag(PIN_VIEW_CELL_DOTTED).assertExists()
    composeRule.onNodeWithTag(PIN_VIEW_CELL_TEXT).assertExists().assertTextEquals("")
  }
}
