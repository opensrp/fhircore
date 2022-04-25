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

package org.smartregister.fhircore.engine.ui.pin

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.components.PIN_VIEW
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP

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
        fun onMenuLoginClicked(value: String) {}
      }
    )

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var pinViewModel: PinViewModel

  @Before
  fun setUp() {
    pinViewModel =
      mockk {
        every { appName } returns "anc"
        every { appLogoResFile } returns "ic_liberia"
        every { pin } returns MutableLiveData("1234")
        every { enableSetPin } returns MutableLiveData(false)
      }
  }

  @Test
  fun testPinSetupScreen() {
    composeRule.setContent { PinSetupScreen(viewModel = pinViewModel) }
    composeRule.onNodeWithTag(PIN_VIEW).assertExists()
    composeRule.onNodeWithTag(PIN_SET_PIN_CONFIRM_BUTTON).assertExists()
  }

  @Test
  fun testPinSetupPage() {
    composeRule.setContent {
      PinSetupPage(
        onPinChanged = { listenerObjectSpy.onPinChanged() },
        onPinConfirmed = { listenerObjectSpy.onPinConfirmed() },
        onMenuSettingClicked = { listenerObjectSpy.onMenuSettingsClicked() },
        onMenuLoginClicked = {
          listenerObjectSpy.onMenuLoginClicked(FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP)
        },
        setPinEnabled = false,
        inputPin = ""
      )
    }

    composeRule.onNodeWithTag(PIN_VIEW).assertExists()

    composeRule.onNodeWithTag(PIN_SET_PIN_CONFIRM_BUTTON).assertExists()
    composeRule.onNodeWithTag(PIN_SET_PIN_CONFIRM_BUTTON).assertHasClickAction()

    composeRule.onNodeWithTag(PIN_TOOLBAR_TITLE).assertExists()
    composeRule.onNodeWithTag(PIN_TOOLBAR_MENU_BUTTON).assertHasClickAction().performClick()
    composeRule.onNodeWithTag(PIN_TOOLBAR_MENU).assertIsDisplayed()
    composeRule
      .onNodeWithTag(PIN_TOOLBAR_MENU)
      .onChildAt(0)
      .assertTextEquals(application.getString(R.string.settings))
      .assertHasClickAction()
      .performClick()

    verify { listenerObjectSpy.onMenuSettingsClicked() }
  }

  @Test
  fun testPinSetupPageLogin() {
    composeRule.setContent {
      PinSetupPage(
        onPinChanged = { listenerObjectSpy.onPinChanged() },
        onPinConfirmed = { listenerObjectSpy.onPinConfirmed() },
        onMenuSettingClicked = { listenerObjectSpy.onMenuSettingsClicked() },
        onMenuLoginClicked = {
          listenerObjectSpy.onMenuLoginClicked(FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP)
        },
        setPinEnabled = false,
        inputPin = ""
      )
    }

    composeRule.onNodeWithTag(PIN_VIEW).assertExists()

    composeRule.onNodeWithTag(PIN_SET_PIN_CONFIRM_BUTTON).assertExists()
    composeRule.onNodeWithTag(PIN_SET_PIN_CONFIRM_BUTTON).assertHasClickAction()

    composeRule.onNodeWithTag(PIN_TOOLBAR_TITLE).assertExists()
    composeRule.onNodeWithTag(PIN_TOOLBAR_MENU_BUTTON).assertHasClickAction().performClick()
    composeRule.onNodeWithTag(PIN_TOOLBAR_MENU).assertIsDisplayed()

    composeRule
      .onNodeWithTag(PIN_TOOLBAR_MENU)
      .onChildAt(1)
      .assertTextEquals(application.getString(R.string.pin_menu_login))
      .assertHasClickAction()
      .performClick()

    verify { listenerObjectSpy.onMenuLoginClicked(FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP) }
  }

  @Test
  fun testPinSetupPageSetPinButtonEnabled() {
    composeRule.setContent {
      PinSetupPage(
        onPinChanged = { listenerObjectSpy.onPinChanged() },
        onPinConfirmed = { listenerObjectSpy.onPinConfirmed() },
        onMenuSettingClicked = { listenerObjectSpy.onMenuSettingsClicked() },
        onMenuLoginClicked = {
          listenerObjectSpy.onMenuLoginClicked(FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP)
        },
        setPinEnabled = true,
        inputPin = "0000"
      )
    }

    composeRule.onNodeWithTag(PIN_VIEW).assertExists()
  }
}
