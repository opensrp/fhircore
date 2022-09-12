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

package org.smartregister.fhircore.engine.ui.usersetting

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class UserSettingScreenKtTest : RobolectricTest() {

  private val mockUserSettingsEventListener: (UserSettingsEvent) -> Unit = spyk({})

  private val userSettingViewModel = mockk<UserSettingViewModel>()

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Before
  fun setUp() {
    every { userSettingViewModel.retrieveUsername() } returns "johndoe"
    every { userSettingViewModel.allowSwitchingLanguages() } returns false
  }

  @Test
  fun testUserProfileShouldDisplayCorrectContent() {
    composeRule.setContent {
      UserSettingScreen(
        userSettingViewModel = userSettingViewModel,
        onClick = mockUserSettingsEventListener
      )
    }

    composeRule.onNodeWithText("Johndoe").assertExists()
    composeRule.onNodeWithText("Sync").assertExists()
    composeRule.onNodeWithText("Log out").assertExists()
  }

  @Test
  fun testSyncRowClickShouldInitiateSync() {
    composeRule.setContent {
      UserSettingScreen(
        userSettingViewModel = userSettingViewModel,
        onClick = mockUserSettingsEventListener
      )
    }
    composeRule.onNodeWithText("Sync").performClick()
    verify { mockUserSettingsEventListener(any()) }
  }

  @Test
  fun testLogoutRowClickShouldInitiateSync() {
    composeRule.setContent {
      UserSettingScreen(
        userSettingViewModel = userSettingViewModel,
        onClick = mockUserSettingsEventListener
      )
    }
    composeRule.onNodeWithTag(USER_SETTING_ROW_TEST_TAG).performClick()
    verify { mockUserSettingsEventListener(any()) }
  }

  @Test
  fun testLanguageRowIsNotShownWhenAllowSwitchingLanguagesIsFalse() {
    composeRule.setContent {
      UserSettingScreen(
        userSettingViewModel = userSettingViewModel,
        onClick = mockUserSettingsEventListener
      )
    }

    composeRule.onNodeWithText("Language").assertDoesNotExist()
  }

  @Test
  fun testLanguageRowIsShownWhenAllowSwitchingLanguagesIsTrue() {
    every { userSettingViewModel.allowSwitchingLanguages() } returns true
    every { userSettingViewModel.loadSelectedLanguage() } returns "Some lang"
    composeRule.setContent {
      UserSettingScreen(
        userSettingViewModel = userSettingViewModel,
        onClick = mockUserSettingsEventListener
      )
    }

    composeRule.onNodeWithText("Language").assertExists()
  }

  @Test
  fun testLanguageRowIsShownWithDropMenuItemsWhenAllowSwitchingLanguagesIsTrueAndLanguagesReturned() {
    val languages = listOf(Language("es", "Spanish"), Language("en", "English"))
    every { userSettingViewModel.languages } returns languages
    every { userSettingViewModel.allowSwitchingLanguages() } returns true
    every { userSettingViewModel.loadSelectedLanguage() } returns "Some lang"
    composeRule.setContent {
      UserSettingScreen(
        userSettingViewModel = userSettingViewModel,
        onClick = mockUserSettingsEventListener
      )
    }

    composeRule.onNodeWithText("Language").performClick()
    composeRule.onNodeWithText("Spanish").assertExists()
    composeRule.onNodeWithText("English").assertExists()
  }
}
