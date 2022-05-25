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

package org.smartregister.fhircore.quest.integration.quest

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.login.LoginActivity

@HiltAndroidTest
class LoginTest : BaseIntegrationTest() {

  @get:Rule val hiltAndroidRule = HiltAndroidRule(this)

  @get:Rule val composeTestRule = createEmptyComposeRule()

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private lateinit var scenario: ActivityScenario<LoginActivity>

  @Before
  fun setup() {
    hiltAndroidRule.inject()
    scenario = ActivityScenario.launch(LoginActivity::class.java)
    scenario.moveToState(Lifecycle.State.RESUMED)
  }

  @After
  fun tearDown() {
    scenario.close()
  }

  @Test
  fun successfulLogin() {
    runForDuration(30) {
      composeTestRule.onNodeWithText("Enter username").assertExists().performTextInput("demo")
      composeTestRule.onNodeWithText("Enter password").assertExists().performTextInput("Amani123")
      composeTestRule.onNodeWithText("LOGIN").performClick()
    }
  }
}
