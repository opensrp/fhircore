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

package org.smartregister.fhircore.quest.ui.main.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import io.mockk.spyk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.navigation.NavigationBottomSheetRegisterConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.appMainUiStateOf

class AppDrawerTest {
  private val mockAppMainEventListener: (AppMainEvent) -> Unit = spyk({})
  @get:Rule val composeTestRule = createComposeRule()

  private val navigationConfiguration =
    NavigationConfiguration(
      appId = "appId",
      configType = ConfigType.Navigation.name,
      staticMenu = listOf(),
      clientRegisters =
        listOf(
          NavigationMenuConfig(id = "id3", visible = true, display = "Register 1"),
          NavigationMenuConfig(id = "id4", visible = false, display = "Register 2")
        ),
      bottomSheetRegisters =
        NavigationBottomSheetRegisterConfig(
          visible = true,
          display = "My Register",
          registers =
            listOf(NavigationMenuConfig(id = "id2", visible = true, display = "Title My Register"))
        ),
      menuActionButton =
        NavigationMenuConfig(id = "id1", visible = true, display = "Register Household")
    )

  @Test
  fun testNavDrawerRendersTopSectionCorrectly() {
    setContent("")
    composeTestRule
      .onNodeWithTag(NAV_TOP_SECTION_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("MOH VTS").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("1(0.0.1)").assertExists().assertIsDisplayed()
  }

  @Test
  fun testNavDrawerRendersMenuActionButtonCorrectly() {
    setContent("")
    composeTestRule
      .onNodeWithTag(MENU_BUTTON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(MENU_BUTTON_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(MENU_BUTTON_TEXT_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(
        navigationConfiguration.menuActionButton?.display?.uppercase() ?: "Register new client"
      )
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testAppDrawerRendersSideMenuItemsCorrectly() {
    setContent("")
    composeTestRule
      .onAllNodesWithTag(SIDE_MENU_ITEM_MAIN_ROW_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(3)
    composeTestRule
      .onAllNodesWithTag(SIDE_MENU_ITEM_INNER_ROW_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(3)
    composeTestRule
      .onAllNodesWithTag(SIDE_MENU_ITEM_TEXT_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(5)
  }

  @Test
  fun testAppDrawerRendersNavBottomSectionCorrectly() {
    setContent("")
    composeTestRule
      .onNodeWithText("Sync", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(NAV_BOTTOM_SECTION_SIDE_MENU_ITEM_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(NAV_BOTTOM_SECTION_MAIN_BOX_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testAppDrawerRendersOtherPatientsItemCorrectly() {
    setContent("")
    composeTestRule
      .onNodeWithText("Other patients", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("05:30 PM, Mar 3", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testAppDrawerRendersGivenItemNameCorrectly() {
    setContent("My Register")
    composeTestRule
      .onNodeWithText("My Register", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testAppDrawerRendersClientRegisterMenusCorrectly() {
    setContent("")
    composeTestRule.onNodeWithTag(NAV_CLIENT_REGISTER_MENUS_LIST).assertExists().assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Register 1", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Register 2", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testThatSideMenuClickCallsTheListener() {
    setContent("")
    val sideMenuItem = composeTestRule.onAllNodesWithTag(SIDE_MENU_ITEM_MAIN_ROW_TEST_TAG)
    sideMenuItem[0].performClick()
    verify { mockAppMainEventListener(any()) }
  }
  private fun setContent(name: String) {
    composeTestRule.setContent {
      AppDrawer(
        appUiState =
          appMainUiStateOf(
            appTitle = "MOH VTS",
            username = "Demo",
            lastSyncTime = "05:30 PM, Mar 3",
            currentLanguage = "English",
            languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
            navigationConfiguration =
              navigationConfiguration.copy(
                bottomSheetRegisters =
                  navigationConfiguration.bottomSheetRegisters?.copy(display = name)
              )
          ),
        navController = rememberNavController(),
        openDrawer = {},
        onSideMenuClick = mockAppMainEventListener,
        appVersionPair = Pair(1, "0.0.1")
      )
    }
  }
}
