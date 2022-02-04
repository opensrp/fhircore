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

package org.smartregister.fhircore.anc.ui.pin

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.details.TOOLBAR_MENU
import org.smartregister.fhircore.anc.ui.family.details.TOOLBAR_MENU_BUTTON
import org.smartregister.fhircore.engine.ui.components.PIN_VIEW

@ExperimentalCoroutinesApi
class PinSetupScreenTest : RobolectricTest() {

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

  private val application = ApplicationProvider.getApplicationContext<Application>()

  @Test
  fun testPinSetupScreenPage() {
    composeRule.setContent {
      PinSetupPage(
        onPinChanged = { listenerObjectSpy.onPinChanged() },
        onPinConfirmed = { listenerObjectSpy.onPinConfirmed() },
        onMenuSettingClicked = { listenerObjectSpy.onMenuSettingsClicked() },
        setPinEnabled = false,
        inputPin = "0000"
      )
    }

    composeRule.onNodeWithTag(PIN_VIEW).assertExists()

    composeRule.onNodeWithTag(SET_PIN_CONFIRM_BUTTON).assertExists()
    composeRule.onNodeWithTag(SET_PIN_CONFIRM_BUTTON).assertHasClickAction()

    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).assertHasClickAction().performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertIsDisplayed()
    composeRule
      .onNodeWithTag(TOOLBAR_MENU)
      .onChildAt(0)
      .assertTextEquals(application.getString(R.string.settings))
      .assertHasClickAction()
      .performClick()

    verify { listenerObjectSpy.onMenuSettingsClicked() }
  }
}
