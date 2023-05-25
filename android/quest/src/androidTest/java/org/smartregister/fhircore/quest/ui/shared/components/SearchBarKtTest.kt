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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class SearchBarKtTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSearchRendersHintCorrectly() {
    composeTestRule.setContent { AppTheme { SearchHint(Modifier.background(color = Color.White)) } }
    composeTestRule.onNodeWithText("Search name or ID").assertIsDisplayed()
  }

  @Test
  fun testSearchBarHasTrailingIconWhenHasTextInput() {
    composeTestRule.setContent {
      AppTheme {
        val searchTextState = remember { mutableStateOf(TextFieldValue(text = "A text yoo")) }
        SearchBar(onTextChanged = {}, onBackPress = {}, searchTextState = searchTextState)
      }
    }
    composeTestRule
      .onNodeWithTag(SEARCH_BAR_TRAILING_ICON_TEST_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
  }

  @Test
  fun testSearchBarHasNoTrailingIconWhenHasNoTextInput() {
    composeTestRule.setContent {
      AppTheme {
        val searchTextState = remember { mutableStateOf(TextFieldValue(text = "")) }
        SearchBar(onTextChanged = {}, onBackPress = {}, searchTextState = searchTextState)
      }
    }
    composeTestRule
      .onNodeWithTag(SEARCH_BAR_TRAILING_ICON_TEST_TAG, useUnmergedTree = true)
      .assertDoesNotExist()
  }

  @Test
  fun testSearchBarOnClickTrailingIconShouldClearTextInput() {
    composeTestRule.setContent {
      AppTheme {
        val searchTextState = remember { mutableStateOf(TextFieldValue(text = "A text yoo")) }
        SearchBar(onTextChanged = {}, onBackPress = {}, searchTextState = searchTextState)
      }
    }
    composeTestRule
      .onNodeWithTag(SEARCH_BAR_TRAILING_TEXT_FIELD_TEST_TAG, useUnmergedTree = true)
      .assertTextEquals("A text yoo")
    composeTestRule
      .onNodeWithTag(SEARCH_BAR_TRAILING_ICON_BUTTON_TEST_TAG, useUnmergedTree = true)
      .performClick()
    composeTestRule
      .onNodeWithTag(SEARCH_BAR_TRAILING_TEXT_FIELD_TEST_TAG, useUnmergedTree = true)
      .assertTextEquals("")
  }
}
