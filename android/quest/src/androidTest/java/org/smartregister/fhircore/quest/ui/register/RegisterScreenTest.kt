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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.quest.HiltActivityForTest

@HiltAndroidTest
class RegisterScreenTest {

  private val mockListener: () -> Unit = spyk({})

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @get:Rule val composeTestRule = createAndroidComposeRule<HiltActivityForTest>()

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun registerScreenRendersNoRegistersViewCorrectly() {
    composeTestRule.setContent {
      NoRegistersView(
        noResults =
          NoResultsConfig(
            title = "Title",
            message = "This is message",
            actionButton = NavigationMenuConfig(display = "Button Text", id = "1")
          ),
        context = LocalContext.current,
        onClick = mockListener
      )
    }
    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_TITLE_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("Title").assertExists().assertIsDisplayed()

    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_MESSAGE_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("This is message").assertExists().assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_BUTTON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()

    val changeRow =
      composeTestRule.onNodeWithTag(NO_REGISTER_VIEW_BUTTON_TEST_TAG, useUnmergedTree = true)
    changeRow.assertExists()
    changeRow.performClick()
    verify { mockListener() }

    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_BUTTON_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_BUTTON_TEXT_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }
}
