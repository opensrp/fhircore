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
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubsequentSyncDetailsBarTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testInitialState() {
    composeTestRule.setContent { SubsequentSyncDetailsBar() {} }

    composeTestRule.onNodeWithText("0% Syncing...").assertIsDisplayed()
    composeTestRule.onNodeWithText("Calculating minutes remaining...").assertIsDisplayed()
    composeTestRule.onNodeWithText("CANCEL").assertIsDisplayed()
  }

  @Test
  fun testProgressDisplay() {
    val progressFlow = flowOf(50)

    composeTestRule.setContent {
      SubsequentSyncDetailsBar(percentageProgressFlow = progressFlow) {}
    }
    composeTestRule.onNodeWithText("50% Syncing...").assertIsDisplayed()
  }

  @Test
  fun testCancelButtonClick() {
    var cancelClicked = false

    composeTestRule.setContent {
      SubsequentSyncDetailsBar(onCancelButtonClick = { cancelClicked = true })
    }

    composeTestRule.onNodeWithText("CANCEL").performClick()
    assert(cancelClicked)
  }

  @Test
  fun testHideExtraInformation() {
    composeTestRule.setContent { SubsequentSyncDetailsBar(hideExtraInformation = false) {} }
    composeTestRule.onNodeWithText("Sync in progress").assertDoesNotExist()
    composeTestRule.onNodeWithText("minutes remaining").assertDoesNotExist()
    composeTestRule.onNodeWithText("Cancel Sync").assertDoesNotExist()
  }
}
