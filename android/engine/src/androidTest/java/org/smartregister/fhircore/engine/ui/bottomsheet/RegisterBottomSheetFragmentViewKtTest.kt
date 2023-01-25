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

package org.smartregister.fhircore.engine.ui.bottomsheet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig

class RegisterBottomSheetFragmentViewKtTest {

  private val mockListener: (NavigationMenuConfig) -> Unit = {}

  @get:Rule val composeRule = createComposeRule()

  private val navigationMenuConfigs =
    listOf(
      NavigationMenuConfig(id = "UniqueTag1", display = "Menu 1"),
      NavigationMenuConfig(id = "UniqueTag2", display = "Menu 2")
    )

  @Before
  fun setUp() {
    composeRule.setContent {
      RegisterBottomSheetView(
        menuClickListener = mockListener,
        navigationMenuConfigs = navigationMenuConfigs,
        onDismiss = {}
      )
    }
  }

  @Test
  fun testThatMenuItemsAreShowing() {
    composeRule.onNodeWithTag(REGISTER_BOTTOM_SHEET_LIST, useUnmergedTree = true).assertExists()
    composeRule.onNodeWithText("Menu 1", useUnmergedTree = true).assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Menu 2", useUnmergedTree = true).assertExists().assertIsDisplayed()
  }
}
