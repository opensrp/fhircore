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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig

class ExtendedFabTest {
  @get:Rule val composeRule = createComposeRule()
  @Before
  fun init() {
    composeRule.setContent {
      ExtendedFab(
        fabActions = listOf(NavigationMenuConfig(id = "test", display = "Fab Button")),
        onViewComponentEvent = {}
      )
    }
  }

  @Test
  fun testFloatingButtonIsDisplayed() {
    composeRule
      .onNodeWithTag(FAB_BUTTON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun extendedFabButtonRendersRowCorrectly() {
    composeRule
      .onNodeWithTag(FAB_BUTTON_ROW_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }
  @Test
  fun extendedFabButtonRendersRowTextCorrectly() {
    composeRule
      .onNodeWithTag(FAB_BUTTON_ROW_TEXT_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithText("FAB BUTTON").assertExists().assertIsDisplayed()
  }
  @Test
  fun extendedFabButtonRendersRowIconCorrectly() {
    composeRule
      .onNodeWithTag(FAB_BUTTON_ROW_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }
}
