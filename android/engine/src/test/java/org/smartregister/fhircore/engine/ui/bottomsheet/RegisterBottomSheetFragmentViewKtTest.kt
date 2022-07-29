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

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class RegisterBottomSheetFragmentViewKtTest : RobolectricTest() {

  private val mockListener: (NavigationMenuConfig) -> Unit = spyk({})

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

  @After
  fun tearDown() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun testThatMenuItemsAreShowing() {
    composeRule.onNodeWithTag(REGISTER_BOTTOM_SHEET_LIST, useUnmergedTree = true).assertExists()
    composeRule.onNodeWithText("Menu 1", useUnmergedTree = true).assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Menu 2", useUnmergedTree = true).assertExists().assertIsDisplayed()
  }

  @Test
  fun testThatMenuClickCallsTheListener() {
    val menu2 = composeRule.onNodeWithText("Menu 2", useUnmergedTree = true)
    menu2.assertExists()
    menu2.performClick()
    verify { mockListener(any()) }
  }
}
