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

package org.smartregister.fhircore.quest.ui.usersetting

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.Language

class UserSettingScreenKtTest {

  @get:Rule(order = 1) val composeRule = createEmptyComposeRule()
  private lateinit var scenario: ActivityScenario<ComponentActivity>
  private lateinit var activity: ComponentActivity

  @Before
  fun setUp() {
    scenario = ActivityScenario.launch(ComponentActivity::class.java)
  }

  @After
  fun tearDown() {
    scenario.close()
  }

  @Test
  fun testUserProfileShouldDisplayCorrectContent() {
    initComposable()
    composeRule.onNodeWithText("Johndoe").assertExists()
    composeRule.onNodeWithText(activity.getString(R.string.resetting_app)).assertDoesNotExist()
    composeRule
      .onNodeWithText(activity.getString(R.string.clear_database_message))
      .assertDoesNotExist()

    composeRule.onNodeWithText("Sync").assertExists()

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
  fun testLanguageRowIsNotShownWhenAllowSwitchingLanguagesIsFalse() {
    initComposable(allowSwitchingLanguages = false)
    composeRule.onNodeWithText("Language").assertDoesNotExist()
  }

  @Test
  fun testLanguageRowIsShownWhenAllowSwitchingLanguagesIsTrue() {
    initComposable()
    composeRule.onNodeWithText("Language").assertExists()
  }

  @Test
  fun testResetDatabaseRowIsRenderedOnProfileScreen() {
    initComposable(isDebugVariant = true)
    composeRule.onNodeWithText("Reset data").assertExists()
  }

  @Test
  fun testWhenShowProgressBarTrueRendersLoaderView() {
    initComposable(isShowProgressBar = true)
    composeRule.onNodeWithText(activity.getString(R.string.resetting_app)).assertExists()
  }

  @Test
  fun testWhenShowProgressBarFalseHidesLoaderView() {
    initComposable(isShowProgressBar = false)
    composeRule.onNodeWithText(activity.getString(R.string.resetting_app)).assertDoesNotExist()
  }

  @Test
  fun testWhenShowDatabaseResetConfirmationFalseHidesConfirmationDialog() {

    initComposable(isShowDatabaseResetConfirmation = false)
    composeRule
      .onNodeWithText(activity.getString(R.string.clear_database_message))
      .assertDoesNotExist()
  }

  @Test
  fun testWhenShowP2POption() {
    initComposable(isP2PAvailable = true)
    composeRule.onNodeWithText(activity.getString(R.string.transfer_data)).assertExists()
  }

  @Test
  fun testWhenShowDatabaseResetConfirmationTrueRendersConfirmationDialog() {

    initComposable(isShowDatabaseResetConfirmation = true)
    composeRule.onNodeWithText(activity.getString(R.string.clear_database_message)).assertExists()
  }

  @Test
  fun testLanguageRowIsShownWithDropMenuItemsWhenAllowSwitchingLanguagesIsTrueAndLanguagesReturned() {
    initComposable(allowMainClockAutoAdvance = true)
    composeRule.onNodeWithText("Language").performClick()
    composeRule.onNodeWithText("Swahili").assertExists()
    composeRule.onNodeWithText("English").assertExists()
  }

  private fun initComposable(
    allowSwitchingLanguages: Boolean = true,
    allowMainClockAutoAdvance: Boolean = false,
    isShowProgressBar: Boolean = false,
    isShowDatabaseResetConfirmation: Boolean = false,
    isDebugVariant: Boolean = false,
    isP2PAvailable: Boolean = false
  ) {
    scenario.onActivity { activity ->
      activity.setContent {
        UserSettingScreen(
          username = "Johndoe",
          allowSwitchingLanguages = allowSwitchingLanguages,
          selectedLanguage = Locale.ENGLISH.toLanguageTag(),
          languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
          showDatabaseResetConfirmation = isShowDatabaseResetConfirmation,
          progressBarState = Pair(isShowProgressBar, R.string.resetting_app),
          isDebugVariant = isDebugVariant,
          onEvent = {},
          mainNavController = rememberNavController(),
          allowP2PSync = isP2PAvailable,
          lastSyncTime = "05:30 PM, Mar 3"
        )
      }

      this.activity = activity
    }
    composeRule.mainClock.autoAdvance = allowMainClockAutoAdvance
  }
}
