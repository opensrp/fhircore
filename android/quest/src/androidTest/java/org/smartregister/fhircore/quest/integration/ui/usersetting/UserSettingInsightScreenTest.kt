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

package org.smartregister.fhircore.quest.integration.ui.usersetting

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.DEFAULT_FORMAT_SDF_DD_MM_YYYY
import org.smartregister.fhircore.quest.ui.usersetting.INSIGHT_UNSYNCED_DATA
import org.smartregister.fhircore.quest.ui.usersetting.UserSettingInsightScreen

class UserSettingInsightScreenTest {

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
  fun testUserInfoViewAndAllItsViewIsShown() {
    initComposable()
    composeRule.onNodeWithText("User info").assertExists()
    composeRule.onNodeWithText("full_name").assertExists()
    composeRule.onNodeWithText("user_team").assertExists()
    composeRule.onNodeWithText("locality").assertExists()
    composeRule.onNodeWithText("User").assertExists()
    composeRule.onNodeWithText("Team").assertExists()
    composeRule.onNodeWithText("Locality").assertExists()
  }

  @Test
  fun testAssignmentInfoViewAndAllItsItemIsShown() {
    initComposable()
    composeRule.onNodeWithText("Assignment info").assertExists()
    composeRule.onNodeWithText("user_name").assertExists()
    composeRule.onNodeWithText("care_team").assertExists()
    composeRule.onNodeWithText("location").assertExists()
    composeRule.onNodeWithText("Location").assertExists()
    composeRule.onNodeWithText("Care Team").assertExists()
    composeRule.onNodeWithText("Username").assertExists()
    composeRule.onNodeWithText("Team(Organization)").assertExists()
  }

  @Test
  fun testAppInfoViewAndAllItsItemIsShown() {
    initComposable()
    composeRule.onNodeWithText("App info").assertExists()
    composeRule.onNodeWithText("3").assertExists()
    composeRule.onNodeWithText("v.123").assertExists()
    composeRule.onNodeWithText("29 jan 2023").assertExists()
    composeRule.onNodeWithText("App version").assertExists()
    composeRule.onNodeWithText("App version code").assertExists()
    composeRule.onNodeWithText("Build date").assertExists()
  }

  @Test
  fun testUnsyncedViewIsShowIfUnsyncedResourceIsNotEmpty() {
    val unsyncedResources = listOf("Patient" to 10)
    initComposable(unsyncedResourcesFlow = MutableStateFlow(unsyncedResources))
    composeRule.onNodeWithTag(INSIGHT_UNSYNCED_DATA).assertExists()
  }

  @Test
  fun testUnsyncViewIsNotShowIfUnsyncedResourceIsEmpty() {
    val unsyncedResources = emptyList<Pair<String, Int>>()
    initComposable(unsyncedResourcesFlow = MutableStateFlow(unsyncedResources))
    composeRule.onNodeWithTag(INSIGHT_UNSYNCED_DATA).assertDoesNotExist()
  }

  private fun initComposable(
    unsyncedResourcesFlow: MutableSharedFlow<List<Pair<String, Int>>> = MutableSharedFlow(),
  ) {
    scenario.onActivity { activity ->
      activity.setContent {
        UserSettingInsightScreen(
          fullName = "full_name",
          team = "user_team",
          locality = "locality",
          userName = "user_name",
          organization = "organization",
          careTeam = "care_team",
          location = "location",
          appVersionCode = "v.123",
          appVersion = "3",
          buildDate = "29 jan 2023",
          unsyncedResourcesFlow = unsyncedResourcesFlow,
          navController = rememberNavController(),
          onRefreshRequest = {},
          dateFormat = DEFAULT_FORMAT_SDF_DD_MM_YYYY,
        )
      }
      this.activity = activity
    }
  }
}
