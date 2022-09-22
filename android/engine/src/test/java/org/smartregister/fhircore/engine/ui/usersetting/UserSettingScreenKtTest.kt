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

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import io.mockk.spyk
import io.mockk.verify
import java.util.Locale
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class UserSettingScreenKtTest : RobolectricTest() {

  private val mockUserSettingsEventListener: (UserSettingsEvent) -> Unit = spyk({})

  @get:Rule(order = 1) val composeRule = createEmptyComposeRule()

  private lateinit var scenario: ActivityScenario<ComponentActivity>

  @Before
  fun setUp() {
    scenario = ActivityScenario.launch(ComponentActivity::class.java)
  }

  @Test
  fun testUserProfileShouldDisplayCorrectContent() {
    initComposable()
    composeRule.onNodeWithText("Johndoe").assertExists()

    // TODO temporary disabled the sync functionality and will be enabled in future
    // composeRule.onNodeWithText("Sync").assertExists()

    composeRule.onNodeWithText("Log out").assertExists()
  }

  // TODO temporary disabled the sync functionality and will be enabled in future
  /*@Test
  fun testSyncRowClickShouldInitiateSync() {
    initComposable()
    composeRule.onNodeWithText("Sync").performClick()
    verify { mockUserSettingsEventListener(any()) }
  }*/

  @Test
  fun testLogoutRowClickShouldInitiateLogout() {
    initComposable()
    composeRule.onNodeWithText("Log out").performClick()
    verify { mockUserSettingsEventListener(any()) }
  }

  @Test
  fun testLanguageRowIsNotShownWhenAllowSwitchingLanguagesIsFalse() {
    initComposable(allowSwitchingLanguages = false)
    composeRule.onNodeWithText("Language").assertDoesNotExist()
  }

  @Test
  fun testLanguageRowIsShownWhenAllowSwitchingLanguagesIsTrue() {
    initComposable()
    composeRule.onNodeWithText("Language").assertExists()
  }

  @Ignore("Fix AppIdleException")
  @Test
  fun testLanguageRowIsShownWithDropMenuItemsWhenAllowSwitchingLanguagesIsTrueAndLanguagesReturned() {
    initComposable(allowMainClockAutoAdvance = true)
    composeRule.onNodeWithText("Language").performClick()
    composeRule.onNodeWithText("Swahili").assertExists()
    composeRule.onNodeWithText("English").assertExists()
  }

  private fun initComposable(
    allowSwitchingLanguages: Boolean = true,
    allowMainClockAutoAdvance: Boolean = false
  ) {
    scenario.onActivity { activity ->
      activity.setContent {
        UserSettingScreen(
          username = "Johndoe",
          allowSwitchingLanguages = allowSwitchingLanguages,
          selectedLanguage = Locale.ENGLISH.toLanguageTag(),
          languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
          onEvent = mockUserSettingsEventListener,
          isShowProgressBar = false,
          isShowDatabaseResetConfirmation = false,
        )
      }
    }
    composeRule.mainClock.autoAdvance = allowMainClockAutoAdvance
  }
  @Test
  fun testResetDatabaseRowIsRenderedOnProfileScreen() {
    initComposable()
    composeRule.onNodeWithText("Reset data").assertExists()
  }
}
