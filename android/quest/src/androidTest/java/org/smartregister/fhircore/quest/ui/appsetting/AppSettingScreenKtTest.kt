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

package org.smartregister.fhircore.quest.ui.appsetting

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R

class AppSettingScreenKtTest {

  @get:Rule val composeRule = createComposeRule()
  private val appId = "appId"
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private var listenersSpy =
    object {
      val onLoadConfigurations: (Boolean) -> Unit = {}

      val onAppIdChanged: (String) -> Unit = {}
    }

  @Before
  fun setUp() {
    composeRule.setContent {
      org.smartregister.fhircore.quest.ui.appsetting.AppSettingScreen(
        appId = appId,
        onAppIdChanged = listenersSpy.onAppIdChanged,
        fetchConfiguration = listenersSpy.onLoadConfigurations,
        error = error
      )
    }
  }

  @Test
  fun testAppSettingScreenLayout() {
    composeRule.onNodeWithText(context.getString(R.string.fhir_core_app)).assertExists()
    composeRule.onNodeWithText(context.getString(R.string.application_id)).assertExists()
    composeRule.onNodeWithText(context.getString(R.string.load_configurations)).assertExists()
  }

  @Test
  fun testLoadConfigurationButtonListenerAction() {
    composeRule
      .onNodeWithText(context.getString(R.string.load_configurations))
      .assertHasClickAction()
  }
}
