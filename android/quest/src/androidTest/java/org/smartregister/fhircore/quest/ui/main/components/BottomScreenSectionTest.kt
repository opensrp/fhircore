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

package org.smartregister.fhircore.quest.ui.main.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen

class BottomScreenSectionTest {

  private val navigationScreens =
    listOf(MainNavigationScreen.Home, MainNavigationScreen.Reports, MainNavigationScreen.Settings)
  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    composeTestRule.setContent {
      BottomScreenSection(
        navController = rememberNavController(),
        mainNavigationScreens = navigationScreens
      )
    }
  }

  @Test
  fun testBottomSectionRendersCorrectly() {
    composeTestRule
      .onNodeWithTag(BOTTOM_NAV_CONTAINER_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testBottomSectionRendersItemsCorrectly() {
    composeTestRule
      .onAllNodesWithTag(BOTTOM_NAV_ITEM_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(navigationScreens.size)
    composeTestRule
      .onAllNodesWithTag(BOTTOM_NAV_ITEM_ICON_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(navigationScreens.size)
  }

  @Test
  fun testBottomSectionRendersTitlesCorrectly() {
    composeTestRule.onNodeWithText("Clients").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Reports").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Settings").assertExists().assertIsDisplayed()
  }
}
