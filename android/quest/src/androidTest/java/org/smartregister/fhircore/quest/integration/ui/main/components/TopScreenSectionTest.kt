/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.integration.ui.main.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import io.mockk.mockk
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.ui.main.components.LEADING_ICON_TEST_TAG
import org.smartregister.fhircore.quest.ui.main.components.OUTLINED_BOX_TEST_TAG
import org.smartregister.fhircore.quest.ui.main.components.TITLE_ROW_TEST_TAG
import org.smartregister.fhircore.quest.ui.main.components.TOP_ROW_ICON_TEST_TAG
import org.smartregister.fhircore.quest.ui.main.components.TOP_ROW_TEXT_TEST_TAG
import org.smartregister.fhircore.quest.ui.main.components.TRAILING_ICON_BUTTON_TEST_TAG
import org.smartregister.fhircore.quest.ui.main.components.TRAILING_ICON_TEST_TAG
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection

class TopScreenSectionTest {
  private val listener: (String) -> Unit = {}

  @get:Rule val composeTestRule = createComposeRule()
  private val navController: NavController = mockk(relaxUnitFun = true)

  @Test
  fun testTopScreenSectionRendersTitleRowCorrectly() {
    composeTestRule.setContent {
      TopScreenSection(
        title = "All Clients",
        searchText = "search text",
        onSearchTextChanged = listener,
        navController = navController,
        isSearchBarVisible = true,
        onClick = {},
      )
    }

    composeTestRule
      .onNodeWithTag(TITLE_ROW_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(TOP_ROW_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(TOP_ROW_TEXT_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("All Clients", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testTopScreenSectionRendersSearchRowCorrectly() {
    composeTestRule.setContent {
      TopScreenSection(
        title = "All Clients",
        searchText = "search text",
        onSearchTextChanged = listener,
        navController = navController,
        isSearchBarVisible = true,
        onClick = {},
      )
    }

    composeTestRule
      .onNodeWithTag(OUTLINED_BOX_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(TRAILING_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(LEADING_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("search text", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testThatTrailingIconClickCallsTheListener() {
    var clicked = false

    composeTestRule.setContent {
      TopScreenSection(
        title = "All Clients",
        searchText = "search text",
        onSearchTextChanged = { clicked = true },
        navController = navController,
        isSearchBarVisible = true,
        onClick = {},
      )
    }

    val trailingIcon = composeTestRule.onNodeWithTag(TRAILING_ICON_BUTTON_TEST_TAG)
    trailingIcon.assertExists()
    trailingIcon.performClick()
    Assert.assertTrue(clicked)
  }
}
