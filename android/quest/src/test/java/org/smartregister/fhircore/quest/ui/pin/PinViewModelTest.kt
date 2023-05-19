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

package org.smartregister.fhircore.quest.ui.pin

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.passwordHashString
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class PinViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  private val sharedPreferenceHelper: SharedPreferencesHelper = mockk(relaxUnitFun = true)
  private var secureSharedPreference: SecureSharedPreference = mockk(relaxUnitFun = true)
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var pinViewModel: PinViewModel
  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    pinViewModel =
      PinViewModel(
        secureSharedPreference = secureSharedPreference,
        sharedPreferences = sharedPreferenceHelper,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = dispatcherProvider
      )
  }

  @Test
  fun testSetPinUiState() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"
    pinViewModel.setPinUiState(true, context)
    val pinUiState = pinViewModel.pinUiState.value
    Assert.assertEquals(context.getString(R.string.set_pin_message), pinUiState.message)
    Assert.assertEquals(true, pinUiState.setupPin)
    Assert.assertTrue(pinUiState.setupPin)
  }

  @Test
  fun testSetPinUiStateDisplaysConfiguredPinLoginMessage() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    every { secureSharedPreference.retrieveSessionPin() } returns null
    every { secureSharedPreference.retrieveSessionUsername() } returns null
    configurationRegistry.configsJsonMap[ConfigType.Application.name] =
      "{\"appId\":\"app\",\"configType\":\"application\",\"loginConfig\":{\"showLogo\":true,\"enablePin\":true,\"pinLoginMessage\":\"Test Message\"}}"
    pinViewModel.setPinUiState(true, context)
    val pinUiState = pinViewModel.pinUiState.value
    Assert.assertEquals("Test Message", pinUiState.message)
  }

  @Test
  fun testSetPinUiStateDisplaysDefaultMessageWhenPinLoginMessageIsNotDefined() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    configurationRegistry.configsJsonMap[ConfigType.Application.name] =
      "{\"appId\":\"app\",\"configType\":\"application\",\"loginConfig\":{\"showLogo\":true,\"enablePin\":true}}"
    every { secureSharedPreference.retrieveSessionPin() } returns null
    every { secureSharedPreference.retrieveSessionUsername() } returns null
    pinViewModel.setPinUiState(true, context)
    val pinUiState = pinViewModel.pinUiState.value
    Assert.assertEquals("CHA will use this PIN to login", pinUiState.message)
  }

  @Test
  fun testOnPinVerified() {
    pinViewModel.onPinVerified(true)

    Assert.assertEquals(true, pinViewModel.navigateToHome.value)
  }

  @Test
  fun testOnSetPin() {
    pinViewModel.onSetPin("1990".toCharArray())

    val newPinSlot = slot<CharArray>()
    verify { secureSharedPreference.saveSessionPin(capture(newPinSlot)) }

    Assert.assertEquals("1990", newPinSlot.captured.concatToString())
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

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun testPinLogin() {

    mockkStatic(::passwordHashString)

    coEvery { passwordHashString(any(), any()) } returns "currentStoredPinHash"
    coEvery { secureSharedPreference.retrieveSessionPin() } returns "currentStoredPinHash"
    coEvery { secureSharedPreference.retrievePinSalt() } returns
      Base64.getEncoder().encodeToString("currentStoredSalt".toByteArray())

    val loginPin = charArrayOf('1', '2', '1', '3', '1', '4')

    var pinIsValid = false
    val callback = { valid: Boolean -> pinIsValid = valid }
    runTest { pinViewModel.pinLogin(loginPin, callback) }
    // Verify the credentials are fetched from the secure shared prefs helper
    verify { secureSharedPreference.retrieveSessionPin() }
    verify { secureSharedPreference.retrievePinSalt() }

    // Verify callback is invoked with result after PIN validation
    Assert.assertTrue(pinIsValid)

    // Verify pin char array is overwritten in memory for valid pin
    Assert.assertEquals("******", loginPin.concatToString())

    // Verify the progressBar flag is set to hidden
    Assert.assertFalse(pinViewModel.pinUiState.value.showProgressBar)

    unmockkStatic(::passwordHashString)
  }
}
