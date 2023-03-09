/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

internal class PinKtTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testPinCellsEqualsToPinInput() {
    composeRule.setContent {
      PinInput(
        pinLength = 4,
        inputMode = true,
        onPinSet = {},
        onPinVerified = {},
        onShowPinError = {}
      )
    }
    composeRule.onNodeWithTag(PIN_TEXT_FIELD_TEST_TAG).performTextInput("1234")

    composeRule.onAllNodesWithTag(PIN_CELL_TEST_TAG).assertCountEquals(4)
    composeRule.onAllNodesWithTag(PIN_CELL_TEXT_TEST_TAG, true)[0].assertTextEquals("1")
    composeRule.onAllNodesWithTag(PIN_CELL_TEXT_TEST_TAG, true)[1].assertTextEquals("2")
    composeRule.onAllNodesWithTag(PIN_CELL_TEXT_TEST_TAG, true)[2].assertTextEquals("3")
    composeRule.onAllNodesWithTag(PIN_CELL_TEXT_TEST_TAG, true)[3].assertTextEquals("4")
  }
}
