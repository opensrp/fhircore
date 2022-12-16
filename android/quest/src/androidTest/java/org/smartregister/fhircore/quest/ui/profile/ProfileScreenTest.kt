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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.quest.Faker
import org.smartregister.fhircore.quest.HiltActivityForTest
import org.smartregister.fhircore.quest.waitUntilExists

@HiltAndroidTest
class ProfileScreenTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<HiltActivityForTest>()

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Before
  fun setUp() {
    hiltRule.inject()
    val snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>().asSharedFlow()
    runBlocking {
      val profileUiState =
        ProfileUiState(
          resourceData =
            ResourceData(
              Patient(),
            ),
          profileConfiguration =
            configurationRegistry.retrieveConfiguration(ConfigType.Profile, "householdProfile")
        )
      composeTestRule.setContent {
        ProfileScreen(
          navController = rememberNavController(),
          profileUiState = profileUiState,
          onEvent = spyk({}),
          snackStateFlow = snackBarStateFlow
        )
      }
    }
  }

  @Test
  fun testFloatingActionButtonIsDisplayed() {
    // We wait for the text be drawn before we do the assertion
    composeTestRule.waitUntilExists(hasText("ADD MEMBER"))
    composeTestRule.waitUntilExists(hasTestTag(FAB_BUTTON_TEST_TAG))
    composeTestRule
      .onNodeWithText("ADD MEMBER", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testTopBarRendersCorrectly() {
    // We wait for the text be drawn before we do the assertion
    composeTestRule
      .onNodeWithTag(PROFILE_TOP_BAR_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(PROFILE_TOP_BAR_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testThatOverflowMenuIsDisplayed() {
    // We wait for the menu icon to be drawn before clicking it
    composeTestRule.waitUntilExists(hasTestTag(DROPDOWN_MENU_TEST_TAG))
    composeTestRule.onNodeWithTag(DROPDOWN_MENU_TEST_TAG).performClick()
    composeTestRule
      .onNodeWithText("Family details", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Change family head", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Remove family", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }
}
