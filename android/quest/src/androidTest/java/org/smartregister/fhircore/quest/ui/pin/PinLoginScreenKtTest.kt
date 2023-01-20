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

package org.smartregister.fhircore.quest.ui.pin

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.components.PIN_CELL_TEST_TAG

class PinLoginScreenKtTest {
  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Test
  fun testThatPinSetupPageIsLaunched() {
    composeRule.setContent {
      PinLoginPage(
        onSetPin = {},
        showError = false,
        onMenuLoginClicked = {},
        forgotPin = {},
        pinUiState =
          PinUiState(
            currentUserPin = "",
            message = "CHA will use this PIN to login",
            appName = "MOH eCBIS",
            setupPin = true,
            pinLength = 4,
            showLogo = true
          ),
        onPinVerified = {}
      )
    }

    // Both title and button named the same
    composeRule.onAllNodesWithText("Set Pin", ignoreCase = true).assertCountEquals(2)
    composeRule.onAllNodesWithTag(PIN_CELL_TEST_TAG).assertCountEquals(4)
    composeRule
      .onNodeWithText("CHA will use this PIN to login", ignoreCase = true, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testThatEnterPinPageIsLaunched() {
    composeRule.setContent {
      PinLoginPage(
        onSetPin = {},
        showError = false,
        onMenuLoginClicked = {},
        forgotPin = {},
        pinUiState =
          PinUiState(
            currentUserPin = "1234",
            message = "Enter PIN for ecbis",
            appName = "MOH eCBIS",
            setupPin = false,
            pinLength = 4,
            showLogo = true
          ),
        onPinVerified = {}
      )
    }
    composeRule.onNodeWithText("MOH eCBIS", ignoreCase = true).assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Enter PIN for ecbis", ignoreCase = true)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onAllNodesWithTag(PIN_CELL_TEST_TAG).assertCountEquals(4)
    val forgotPinNode = composeRule.onNodeWithText("Forgot PIN?", ignoreCase = true)
    forgotPinNode.assertExists().assertIsDisplayed().assertHasClickAction()

    // Clicking forgot pin should launch dialog
    forgotPinNode.performClick()
    composeRule.onNodeWithText("CANCEL").assertIsDisplayed().assertHasClickAction()
    composeRule.onNodeWithText("DIAL NUMBER").assertIsDisplayed().assertHasClickAction()
  }
}
