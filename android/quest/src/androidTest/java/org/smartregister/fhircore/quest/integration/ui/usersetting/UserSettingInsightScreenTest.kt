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

package org.smartregister.fhircore.quest.integration.ui.usersetting

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.quest.ui.usersetting.UserInfoView
import org.smartregister.fhircore.quest.ui.usersetting.UserSettingInsightScreen

class UserSettingInsightScreenTest {

  @get:Rule(order = 1) val composeRule = createComposeRule()
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
 fun testUserInfoViewAndAllItsViewIsShown(){
   composeRule.setContent {
     UserInfoView(title = "User Info", name = "John Doe", team = "user_team", locality = "locality")

     composeRule.onNodeWithText("User Info").assertExists()
     composeRule.onNodeWithText("John Doe").assertExists()
     composeRule.onNodeWithText("user_team").assertExists()
     composeRule.onNodeWithText("locality").assertExists()
     composeRule.onNodeWithText("User").assertExists()
     composeRule.onNodeWithText("Team").assertExists()
     composeRule.onNodeWithText("Locality").assertExists()
   }
 }


  private fun initComposable(
    unsyncedResourcesFlow: MutableSharedFlow<List<Pair<String, Int>>> = MutableSharedFlow(),
    ) {
    scenario.onActivity { activity->
      activity.setContent {
        UserSettingInsightScreen(
          fullName = "full_name",
          team = "team",
          locality = "locality" ,
          userName = "username",
          organization = "organization",
          careTeam = "care_team",
          location = "location",
          appVersionCode = "v.123",
          appVersion = "7",
          buildDate = "29 jan 2023" ,
          unsyncedResourcesFlow = unsyncedResourcesFlow,
          navController = rememberNavController()
        ) {

        }
      }
    }
  }
}
