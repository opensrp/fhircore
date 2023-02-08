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

package org.smartregister.fhircore.quest.ui.pin

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class PinViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  private val sharedPreferenceHelper: SharedPreferencesHelper = mockk(relaxUnitFun = true)
  private var secureSharedPreference: SecureSharedPreference = mockk(relaxUnitFun = true)
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var pinViewModel: PinViewModel

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    pinViewModel =
      PinViewModel(
        secureSharedPreference = secureSharedPreference,
        sharedPreferences = sharedPreferenceHelper,
        configurationRegistry = configurationRegistry,
      )
  }

  @Test
  fun testSetPinUiState() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    every { secureSharedPreference.retrieveSessionPin() } returns "1245"
    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"
    pinViewModel.setPinUiState(true, context)
    val pinUiState = pinViewModel.pinUiState.value
    Assert.assertEquals("1245", pinUiState.currentUserPin)
    Assert.assertTrue(pinUiState.setupPin)
  }

  @Test
  fun testOnPinVerified() {
    pinViewModel.onPinVerified(true)

    Assert.assertEquals(true, pinViewModel.navigateToHome.value)

    pinViewModel.onPinVerified(false)
    Assert.assertEquals(true, pinViewModel.showError.value)
  }

  @Test
  fun testOnSetPin() {
    pinViewModel.onSetPin("1990")

    val newPinSlot = slot<String>()
    verify { secureSharedPreference.saveSessionPin(capture(newPinSlot)) }

    Assert.assertEquals("1990", newPinSlot.captured)
    Assert.assertEquals(true, pinViewModel.navigateToHome.value)
  }

  @Test
  fun testOnMenuItemClickedWithLaunchAppSettingScreenSetTrue() {
    pinViewModel.onMenuItemClicked(true)

    // Session token pin and credentials reset
    verifyOrder {
      secureSharedPreference.deleteSessionPin()
      secureSharedPreference.deleteCredentials()
    }
    verify { sharedPreferenceHelper.remove(SharedPreferenceKey.APP_ID.name) }
    Assert.assertEquals(true, pinViewModel.navigateToSettings.value)
  }

  @Test
  fun testOnMenuItemClickedWithLaunchAppSettingScreenSetFalse() {
    pinViewModel.onMenuItemClicked(false)

    // Session token pin and credentials reset
    verify { secureSharedPreference.deleteSessionPin() }
    Assert.assertEquals(true, pinViewModel.navigateToLogin.value)
  }

  @Test
  fun testForgotPin() {
    pinViewModel.forgotPin()
    Assert.assertEquals("tel:####", pinViewModel.launchDialPad.value)
  }

  @Test
  fun testOnShowPinError() {
    pinViewModel.onShowPinError(false)
    Assert.assertEquals(false, pinViewModel.showError.value)
  }
}
