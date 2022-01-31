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

package org.smartregister.fhircore.anc.ui.otp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.components.PIN_VIEW

@ExperimentalCoroutinesApi
class OtpSetupScreenTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  private val listenerObjectSpy =
    spyk(
      object {
        // Imitate click action by doing nothing
        fun onPinChanged() {}
        fun onPinConfirmed() {}
        fun onMenuSettingsClicked() {}
      }
    )

  @Test
  fun testOtpLoginScreenPage() {
    composeRule.setContent {
      OtpSetupPage(
        onPinChanged = { listenerObjectSpy.onPinChanged() },
        onPinConfirmed = { listenerObjectSpy.onPinConfirmed() },
        onMenuSettingClicked = { listenerObjectSpy.onMenuSettingsClicked() },
        setPinEnabled = false,
        inputPin = "0000"
      )
    }
    composeRule.onNodeWithTag(TOOLBAR_MENU_ICON).assertExists()
    composeRule.onNodeWithTag(TOOLBAR_MENU_SETTINGS).assertExists()
    composeRule.onNodeWithTag(SET_PIN_CONFIRM_BUTTON).assertExists()
    composeRule.onNodeWithTag(PIN_VIEW).assertExists()
    composeRule.onNodeWithTag(SET_PIN_CONFIRM_BUTTON).performClick()
    verify { listenerObjectSpy.onPinConfirmed() }
  }
}
