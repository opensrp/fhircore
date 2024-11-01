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

package org.smartregister.fhircore.quest.integration.ui.profile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest
import org.smartregister.fhircore.quest.integration.Faker
import org.smartregister.fhircore.quest.ui.profile.DROPDOWN_MENU_TEST_TAG
import org.smartregister.fhircore.quest.ui.profile.FAB_BUTTON_TEST_TAG
import org.smartregister.fhircore.quest.ui.profile.PROFILE_TOP_BAR_ICON_TEST_TAG
import org.smartregister.fhircore.quest.ui.profile.PROFILE_TOP_BAR_TEST_TAG
import org.smartregister.fhircore.quest.ui.profile.ProfileScreen
import org.smartregister.fhircore.quest.ui.profile.ProfileUiState

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
              baseResourceId = "patientId",
              baseResourceType = ResourceType.Patient,
              computedValuesMap = emptyMap(),
            ),
          profileConfiguration =
            configurationRegistry.retrieveConfiguration(ConfigType.Profile, "householdProfile"),
        )
      composeTestRule.setContent {
        ProfileScreen(
          navController = rememberNavController(),
          profileUiState = profileUiState,
          snackStateFlow = snackBarStateFlow,
          onEvent = {},
          decodeImage = null,
        )
      }
    }
  }

  @Test
  fun testFloatingActionButtonIsDisplayed() {
    // We wait for the text be drawn before we do the assertion
    composeTestRule.waitUntil(5_000) { true }
    composeTestRule
      .onAllNodesWithTag(FAB_BUTTON_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(3)
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
    composeTestRule.waitUntil(5000) { true }
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
