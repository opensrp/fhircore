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

package org.smartregister.fhircore.engine.ui.appsetting

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class AppSettingScreenKtTest : RobolectricTest() {

  private class Listeners {
    val onLoadConfigurations: (Boolean) -> Unit = spyk()

    val onAppIdChanged: (String) -> Unit = spyk()
  }

  private val appId = "appId"

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @get:Rule val composeRule = createComposeRule()

  private var listenersSpy = spyk<Listeners>()
  @Before
  fun setUp() {
    composeRule.setContent {
      AppSettingScreen(
        appId = appId,
        onAppIdChanged = listenersSpy.onAppIdChanged,
        onLoadConfigurations = listenersSpy.onLoadConfigurations
      )
    }
  }

  @Test
  fun testAppSettingScreenLayout() {
    composeRule.onNodeWithText(context.getString(R.string.fhir_core_app))
    composeRule.onNodeWithText(context.getString(R.string.application_id))
    composeRule.onNodeWithText(context.getString(R.string.enter_app_id))
    composeRule.onNodeWithText(context.getString(R.string.app_id_sample))
    composeRule.onNodeWithText(context.getString(R.string.remember_app))
    composeRule.onNodeWithText(context.getString(R.string.load_configurations))
  }

  @Test
  fun testLoadConfigurationButtonListenerAction() {
    composeRule.onNodeWithText(context.getString(R.string.load_configurations)).performClick()
    verify { listenersSpy.onLoadConfigurations }
  }

  @Test
  @Ignore("Fix this test; runs indefinitely")
  fun testUpdatingAppIdAction() {
    composeRule.onNodeWithTag(APP_ID_TEXT_INPUT_TAG).performTextInput("appId")
    verify { listenersSpy.onAppIdChanged }
  }
}
