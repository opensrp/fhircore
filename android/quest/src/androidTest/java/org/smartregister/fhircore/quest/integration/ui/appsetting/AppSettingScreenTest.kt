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

package org.smartregister.fhircore.quest.integration.ui.appsetting

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingScreen

class AppSettingScreenTest {

  @get:Rule val composeRule = createComposeRule()
  private val appId = "appId"
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private var listenersSpy =
    object {
      val onLoadConfigurations: (Context) -> Unit = {}

      val onAppIdChanged: (String) -> Unit = {}
    }

  @Test
  fun testAppSettingScreenLayout() {
    composeRule.setContent {
      AppSettingScreen(
        appId = appId,
        onAppIdChanged = listenersSpy.onAppIdChanged,
        fetchConfiguration = listenersSpy.onLoadConfigurations,
        error = "",
      )
    }

    composeRule.onNodeWithText(context.getString(R.string.fhir_core_app)).assertExists()
    composeRule.onNodeWithText(context.getString(R.string.application_id)).assertExists()
    composeRule.onNodeWithText(context.getString(R.string.load_configurations)).assertExists()
  }

  @Test
  fun testLoadConfigurationButtonListenerAction() {
    composeRule.setContent {
      AppSettingScreen(
        appId = appId,
        onAppIdChanged = listenersSpy.onAppIdChanged,
        fetchConfiguration = listenersSpy.onLoadConfigurations,
        error = "",
      )
    }

    composeRule
      .onNodeWithText(context.getString(R.string.load_configurations))
      .assertHasClickAction()
  }

  @Test
  fun testErrorString() {
    val error = "theError"
    composeRule.setContent {
      AppSettingScreen(
        appId = appId,
        onAppIdChanged = listenersSpy.onAppIdChanged,
        fetchConfiguration = listenersSpy.onLoadConfigurations,
        error = error,
      )
    }

    composeRule.onNodeWithText(error).assertExists()
  }
}
