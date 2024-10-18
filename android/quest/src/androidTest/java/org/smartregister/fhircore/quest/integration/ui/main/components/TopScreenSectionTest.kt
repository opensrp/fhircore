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

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.testing.TestNavHostController
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
import org.smartregister.fhircore.quest.ui.main.components.TRAILING_QR_SCAN_ICON_BUTTON_TEST_TAG
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery

class TopScreenSectionTest {
  private val listener: (SearchQuery, Boolean) -> Unit = { _, _ -> }

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testTopScreenSectionRendersTitleRowCorrectly() {
    composeTestRule.setContent {
      TopScreenSection(
        title = "All Clients",
        searchQuery = SearchQuery("search text"),
        onSearchTextChanged = listener,
        navController = TestNavHostController(LocalContext.current),
        isSearchBarVisible = true,
        onClick = {},
        decodeImage = null,
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
        searchQuery = SearchQuery("search text"),
        onSearchTextChanged = listener,
        navController = TestNavHostController(LocalContext.current),
        isSearchBarVisible = true,
        onClick = {},
        decodeImage = null,
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
        searchQuery = SearchQuery("search text"),
        onSearchTextChanged = { _, _ -> clicked = true },
        navController = TestNavHostController(LocalContext.current),
        isSearchBarVisible = true,
        onClick = {},
        decodeImage = null,
      )
    }

    val trailingIcon = composeTestRule.onNodeWithTag(TRAILING_ICON_BUTTON_TEST_TAG)
    trailingIcon.assertExists()
    trailingIcon.performClick()
    Assert.assertTrue(clicked)
  }

  @Test
  fun thatTopScreenSectionHideQrCodeIconWhenShowSearchByQrCodeIsTrueAndSearchQueryIsNotBlank() {
    composeTestRule.setContent {
      TopScreenSection(
        title = "All Clients",
        searchQuery = SearchQuery("search text"),
        showSearchByQrCode = true,
        navController = TestNavHostController(LocalContext.current),
        isSearchBarVisible = true,
        onClick = {},
        decodeImage = null,
      )
    }
    composeTestRule.onNodeWithTag(TRAILING_QR_SCAN_ICON_BUTTON_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun thatTopScreenSectionShowsQrCodeIconWhenShowSearchByQrCodeIsTrueAndSearchQueryIsBlank() {
    composeTestRule.setContent {
      TopScreenSection(
        title = "All Clients",
        searchQuery = SearchQuery(""),
        showSearchByQrCode = true,
        navController = TestNavHostController(LocalContext.current),
        isSearchBarVisible = true,
        onClick = {},
        decodeImage = null,
      )
    }
    composeTestRule.onNodeWithTag(TRAILING_QR_SCAN_ICON_BUTTON_TEST_TAG).assertIsDisplayed()
  }
}
