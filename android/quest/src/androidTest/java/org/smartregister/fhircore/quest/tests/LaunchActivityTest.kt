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

package org.smartregister.fhircore.quest.tests

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity

class LaunchActivityTest {
  @get:Rule val composeTestRule = createEmptyComposeRule()
  // @get:Rule val composeTestRule2 = createAndroidCocmposeRule(LoginActivity::class.java)
  private lateinit var scenario: ActivityScenario<AppSettingActivity>

  @Before
  fun setup() {
    scenario = ActivityScenario.launch(AppSettingActivity::class.java)
    scenario.moveToState(Lifecycle.State.RESUMED)
    Thread.sleep(5000)
    composeTestRule.onNodeWithText("Enter Application ID").performTextInput("quest")
    composeTestRule.onNodeWithText("LOAD CONFIGURATIONS").performClick()
    Thread.sleep(15000)
    composeTestRule.waitForIdle()
  }

  @Test
  fun successfulLogin() {
    Thread.sleep(5000)
    composeTestRule.onNodeWithText("Enter username").performTextInput("ecbis")
    composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
    composeTestRule.onNodeWithText("LOGIN").performClick()
    Thread.sleep(5000)
  }
}
