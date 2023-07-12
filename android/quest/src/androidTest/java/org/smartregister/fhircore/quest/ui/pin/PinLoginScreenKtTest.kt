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

package org.smartregister.fhircore.quest.ui.pin

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
            message = "CHA will use this PIN to login",
            appName = "MOH eCBIS",
            setupPin = true,
            pinLength = 4,
            showLogo = true,
          ),
        onShowPinError = {},
        onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
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
            message = "Enter PIN for ecbis",
            appName = "MOH eCBIS",
            setupPin = false,
            pinLength = 4,
            showLogo = true,
          ),
        onShowPinError = {},
        onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
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

  @Test
  fun testThatErrorMessageIsDisplayedOnPinLogin() {
    val errorMessage = "Incorrect PIN. Please try again."
    composeRule.setContent {
      PinLoginPage(
        onSetPin = {},
        showError = true,
        onMenuLoginClicked = {},
        forgotPin = {},
        pinUiState =
          PinUiState(
            message = errorMessage,
            appName = "MOH eCBIS",
            setupPin = false,
            pinLength = 4,
            showLogo = true,
            showProgressBar = true,
          ),
        onShowPinError = {},
        onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
      )
    }
    composeRule.onNodeWithText(errorMessage, ignoreCase = true).assertExists().assertIsDisplayed()
  }

  @Test
  fun testThatPinSetupPageShowsCircularProgressIndicator() {
    composeRule.setContent {
      PinLoginPage(
        onSetPin = {},
        showError = false,
        onMenuLoginClicked = {},
        forgotPin = {},
        pinUiState =
          PinUiState(
            message = "Provider will use this PIN to login",
            appName = "Quest",
            setupPin = true,
            pinLength = 4,
            showLogo = false,
            showProgressBar = true,
          ),
        onShowPinError = {},
        onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
      )
    }

    composeRule.onAllNodesWithText("Set Pin", ignoreCase = true).assertCountEquals(1)
    composeRule.onAllNodesWithTag(PIN_CELL_TEST_TAG).assertCountEquals(4)
    composeRule.onNodeWithTag(CIRCULAR_PROGRESS_INDICATOR).assertExists().assertIsDisplayed()
  }

  @Test
  fun testThatPinLoginPageWithShowLogoFalseHidesLogoImage() {
    val pinStateMessage = "Provider will use this PIN to login"
    composeRule.setContent {
      PinLoginPage(
        onSetPin = {},
        showError = false,
        onMenuLoginClicked = {},
        forgotPin = {},
        pinUiState =
          PinUiState(
            message = pinStateMessage,
            appName = "Quest APP",
            setupPin = false,
            pinLength = 1,
            showLogo = false,
            showProgressBar = true,
          ),
        onShowPinError = {},
        onPinEntered = { _: CharArray, _: (Boolean) -> Unit -> },
      )
    }

    composeRule.onAllNodesWithText("Quest APP", ignoreCase = true).assertCountEquals(1)
    composeRule.onNodeWithText(pinStateMessage).assertExists().assertIsDisplayed()
    composeRule.onNodeWithTag(PIN_LOGO_IMAGE).assertDoesNotExist()
  }
}
