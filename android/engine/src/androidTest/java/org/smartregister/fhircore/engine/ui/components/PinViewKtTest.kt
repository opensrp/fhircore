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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class PinViewKtTest {

  @get:Rule val composeTestRule = createComposeRule()

  @OptIn(ExperimentalComposeUiApi::class)
  @Before
  fun init() {
    composeTestRule.setContent { AppTheme { PinView(inputPin = "1934") } }
  }

  @Test
  fun testPinViewInputsAndRendersPinDigitsCorrectly() {

    composeTestRule.onNodeWithText("1").assertIsDisplayed()
    composeTestRule.onNodeWithText("9").assertIsDisplayed()
    composeTestRule.onNodeWithText("3").assertIsDisplayed()
    composeTestRule.onNodeWithText("4").assertIsDisplayed()
  }
}
