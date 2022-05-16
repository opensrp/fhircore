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

package org.smartregister.fhircore.engine.ui.userprofile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@Ignore("Fix failing tests")
class UserProfileScreenKtTest : RobolectricTest() {

  private val userProfileViewModel = mockk<UserProfileViewModel>()

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Before
  fun setUp() {
    every { userProfileViewModel.retrieveUsername() } returns "johndoe"
    every { userProfileViewModel.allowSwitchingLanguages() } returns false
  }

  @Test
  fun testUserProfileShouldDisplayCorrectContent() {
    composeRule.setContent { UserProfileScreen(userProfileViewModel = userProfileViewModel) }

    composeRule.onNodeWithText("Johndoe").assertExists()
    composeRule.onNodeWithText("Sync").assertExists()
    composeRule.onNodeWithText("Log out").assertExists()
  }

  @Test
  fun testSyncRowClickShouldInitiateSync() {
    composeRule.setContent { UserProfileScreen(userProfileViewModel = userProfileViewModel) }
    every { userProfileViewModel.runSync() } returns Unit

    composeRule.onNodeWithText("Sync").performClick()

    verify { userProfileViewModel.runSync() }
  }

  @Test
  fun testLanguageRowIsNotShownWhenAllowSwitchingLanguagesIsFalse() {
    composeRule.setContent { UserProfileScreen(userProfileViewModel = userProfileViewModel) }

    composeRule.onNodeWithText("Language").assertDoesNotExist()
  }

  @Test
  fun testLanguageRowIsShownWhenAllowSwitchingLanguagesIsTrue() {
    every { userProfileViewModel.allowSwitchingLanguages() } returns true
    every { userProfileViewModel.loadSelectedLanguage() } returns "Some lang"
    composeRule.setContent { UserProfileScreen(userProfileViewModel = userProfileViewModel) }

    composeRule.onNodeWithText("Language").assertExists()
  }

  @Test
  fun testLanguageRowIsShownWithDropMenuItemsWhenAllowSwitchingLanguagesIsTrueAndLanguagesReturned() {
    val languages = listOf(Language("es", "Spanish"), Language("en", "English"))
    every { userProfileViewModel.languages } returns languages
    every { userProfileViewModel.allowSwitchingLanguages() } returns true
    every { userProfileViewModel.loadSelectedLanguage() } returns "Some lang"
    composeRule.setContent { UserProfileScreen(userProfileViewModel = userProfileViewModel) }

    composeRule.onNodeWithText("Language").performClick()
    composeRule.onNodeWithText("Spanish").assertExists()
    composeRule.onNodeWithText("English").assertExists()
  }
}
