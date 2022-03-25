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

package org.smartregister.fhircore.anc.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity

class LaunchActivityTest {
  @get:Rule val composeTestAppScreen = createAndroidComposeRule(AppSettingActivity::class.java)
  @get:Rule val composeTestRule = createAndroidComposeRule(LoginActivity::class.java)
  @Test
  fun applicationId() {
    composeTestAppScreen.onNodeWithText("Enter Application ID").performTextInput("demo")
    composeTestAppScreen.onNodeWithText("LOAD CONFIGURATIONS").performClick()
  }
  @Test
  fun successfulLogin() {
    composeTestRule.onNodeWithText("Enter username").performTextInput("demo")
    composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
    composeTestRule.onNodeWithText("LOGIN").performClick()
  }
}
