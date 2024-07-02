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

package org.smartregister.fhircore.quest.integration.ui.shared.components

import SubsequentSyncDetailsBar
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.navigation.NavigationBottomSheetRegisterConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.quest.ui.main.SyncStatus
import org.smartregister.fhircore.quest.ui.main.appMainUiStateOf

@RunWith(AndroidJUnit4::class)
class SubsequentSyncDetailsBarTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val navigationConfiguration =
    NavigationConfiguration(
      appId = "appId",
      configType = ConfigType.Navigation.name,
      staticMenu = listOf(),
      clientRegisters =
        listOf(
          NavigationMenuConfig(id = "id3", visible = true, display = "Register 1"),
          NavigationMenuConfig(id = "id4", visible = false, display = "Register 2"),
        ),
      bottomSheetRegisters =
        NavigationBottomSheetRegisterConfig(
          visible = true,
          display = "My Register",
          registers =
            listOf(NavigationMenuConfig(id = "id2", visible = true, display = "Title My Register")),
        ),
      menuActionButton =
        NavigationMenuConfig(id = "id1", visible = true, display = "Register Household"),
    )

  val appUiState =
    appMainUiStateOf(
      appTitle = "MOH VTS",
      username = "Demo",
      lastSyncTime = "05:30 PM, Mar 3",
      currentLanguage = "English",
      progressPercentage = 50,
      syncStatus = SyncStatus.INPROGRESS,
      isSyncUpload = true,
      languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
      navigationConfiguration =
        navigationConfiguration.copy(
          bottomSheetRegisters =
            navigationConfiguration.bottomSheetRegisters?.copy(display = "Random name"),
        ),
    )

  @Test
  fun testProgressBarDisplaysCorrectProgress() {
    composeTestRule.setContent {
      SubsequentSyncDetailsBar(
        appUiState = appUiState,
        hideExtraInformation = true,
        onCancelButtonClick = {},
      )
    }

    // Check the progress percentage text
    composeTestRule.onNodeWithText("50% Syncing...").assertIsDisplayed()
  }

  @Test
  fun testCancelButtonClick() {
    var isCancelClicked = false

    composeTestRule.setContent {
      SubsequentSyncDetailsBar(
        appUiState = appUiState,
        hideExtraInformation = true,
        onCancelButtonClick = { isCancelClicked = true },
      )
    }

    // Perform a click on the cancel button
    composeTestRule.onNodeWithText("CANCEL").performClick()

    // Verify that the click event was triggered
    assert(isCancelClicked)
  }

  @Test
  fun testHideExtraInformation() {
    composeTestRule.setContent {
      SubsequentSyncDetailsBar(
        appUiState = appUiState,
        hideExtraInformation = false,
        onCancelButtonClick = {},
      )
    }

    // Verify that extra information text is not displayed
    composeTestRule.onNodeWithText("20% Syncing...").assertDoesNotExist()

    composeTestRule.onNodeWithText("Calculating mins remaining...").assertDoesNotExist()

    // Verify that the cancel button is not displayed
    composeTestRule.onNodeWithText("CANCEL").assertDoesNotExist()
  }
}
