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

package org.smartregister.fhircore.engine.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem

class RegisterBottomSheetViewsKtTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  private val registerItems =
    listOf(
      RegisterItem(uniqueTag = "UniqueTag1", title = "Menu 1", isSelected = true),
      RegisterItem(uniqueTag = "UniqueTag2", title = "Menu 2", isSelected = false),
    )

  @Test
  fun testThatMenuItemsAreShowing() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent { RegisterBottomSheet(registers = registerItems, itemListener = {}) }
    composeRule.onNodeWithText("Menu 1").assertExists()
    composeRule.onNodeWithText("Menu 2").assertExists()

    // A tick icon showing for selected menu
    composeRule.onNodeWithContentDescription("Tick").assertExists()
  }

  @Test
  fun testThatMenuClickCallsTheListener() {
    var itemListenerCalled = false
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      RegisterBottomSheet(registers = registerItems, itemListener = { itemListenerCalled = true })
    }
    val menu2 = composeRule.onNodeWithText("Menu 2")
    menu2.assertExists()
    menu2.performClick()
    Assert.assertTrue(itemListenerCalled)
  }
}
