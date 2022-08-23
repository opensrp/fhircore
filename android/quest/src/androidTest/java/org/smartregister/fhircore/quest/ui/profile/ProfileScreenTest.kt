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

package org.smartregister.fhircore.quest.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration

@HiltAndroidTest
class ProfileScreenTest {
  @get:Rule(order = 1) val composeTestRule = createComposeRule()
  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  private val profileViewModel = mockk<ProfileViewModel>()
  private lateinit var navController: NavHostController
  private lateinit var profileUiState: ProfileUiState
  private val appDebug = "app/debug"

  @Before
  fun setUp() {
    hiltRule.inject()
    navController = mockk()
    runBlocking {
      configurationRegistry.loadConfigurations(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        appId = appDebug
      ) {}
      val profileConfiguration =
        configurationRegistry.retrieveConfiguration<ProfileConfiguration>(
          ConfigType.Profile,
          "householdProfile"
        )
      profileUiState = ProfileUiState(profileConfiguration = profileConfiguration)
    }

    every { profileViewModel.onEvent(any()) } returns
      composeTestRule.setContent {
        ProfileScreen(
          navController = navController,
          profileUiState = profileUiState,
          onEvent = mockk()
        )
      }
  }

  @Test
  fun testFloatingActionButtonIsDisplayed() {
    composeTestRule.onNodeWithText("ADD MEMBER").assertExists().assertIsDisplayed()
  }

  @Test
  fun testOverFlowMenuItemsListedInTheHouseHoldProfileConfigAreDisplayed() {
    composeTestRule.onNodeWithTag(ICON_BUTTON_TEST_TAG).assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag(ICON_BUTTON_TEST_TAG).performClick()
    composeTestRule.onNodeWithText("Family details").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Change family head").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Family activity").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Family details").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Remove family").assertExists().assertIsDisplayed()
  }

  @Test
  fun testThatFabButtonIsDisplayed() {
    composeTestRule.onNodeWithTag(FAB_BUTTON_TEST_TAG).assertExists().assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(
        profileUiState.profileConfiguration?.fabActions?.first()?.display?.uppercase().toString()
      )
      .assertExists()
      .assertIsDisplayed()
  }
}
