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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class PinViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()

  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  @BindValue val configurationRegistry = Faker.buildTestConfigurationRegistry()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var pinViewModel: PinViewModel

  private val testPin = MutableLiveData("1234")

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { sharedPreferencesHelper.read(any(), "") } returns "1234"
    coEvery { sharedPreferencesHelper.write(any(), true) } just runs
    coEvery { sharedPreferencesHelper.remove(any()) } returns Unit
    coEvery { secureSharedPreference.retrieveSessionUsername() } returns "demo"
    coEvery { secureSharedPreference.saveSessionPin("1234") } returns Unit
    coEvery { secureSharedPreference.retrieveSessionPin() } returns "1234"
    coEvery {
      secureSharedPreference.saveCredentials(
        AuthCredentials("username", "password", "sessionToken", "refreshToken")
      )
    } returns Unit

    pinViewModel =
      spyk(
        PinViewModel(
          sharedPreferences = sharedPreferencesHelper,
          secureSharedPreference = secureSharedPreference,
          configurationRegistry = configurationRegistry,
        )
      )

    every { pinViewModel.pinUiState } returns
      mutableStateOf(PinUiState(currentUserPin = "1234", setupPin = true, appName = "demo"))
    every { pinViewModel.applicationConfiguration } returns
      ApplicationConfiguration(appId = "appId", appTitle = "demo")
    every { pinViewModel.showError } returns MutableLiveData(false)
    every { pinViewModel.navigateToHome } returns MutableLiveData(true)
  }

  @Test
  fun testOnPinChangeValidated() {
    pinViewModel.pinUiState.value = PinUiState(currentUserPin = "1234", setupPin = false)

    pinViewModel.onSetPin(testPin.value.toString())
    Assert.assertEquals(pinViewModel.pinUiState.value.currentUserPin, testPin.value.toString())
    Assert.assertEquals(pinViewModel.navigateToHome.value, true)
  }

  @Test
  fun testOnPinConfirmed() {
    pinViewModel.onPinVerified()
    Assert.assertEquals(
      pinViewModel.secureSharedPreference.retrieveSessionPin()!!,
      testPin.value.toString()
    )
    Assert.assertEquals(false, pinViewModel.showError.value)
  }

  @Test
  fun testOnPinConfirmedValidated() {
    pinViewModel.onPinVerified()
    Assert.assertEquals(
      pinViewModel.secureSharedPreference.retrieveSessionPin()!!,
      testPin.value.toString()
    )
    Assert.assertEquals(pinViewModel.showError.value, false)
    Assert.assertEquals(true, pinViewModel.navigateToHome.value)
  }

  @Test
  fun testLoadData() {
    pinViewModel.setPinUiState(setupPin = true, context = application)
    val pinUiState = pinViewModel.pinUiState.value
    Assert.assertEquals(pinUiState.setupPin, true)
    Assert.assertNotNull(pinUiState.currentUserPin)
    Assert.assertNotNull(pinUiState.message)
  }

  @Test
  fun testLoadDataForLoginScreen() {
    pinViewModel.setPinUiState(setupPin = false, context = application)
    val pinUiState = pinViewModel.pinUiState.value
    Assert.assertEquals(pinUiState.setupPin, true)
    Assert.assertNotNull(pinUiState.currentUserPin)
  }

  @Test
  fun testOnPinChangeError() {
    pinViewModel.onSetPin("3232")
    Assert.assertEquals(pinViewModel.showError.value, true)
  }

  @Ignore("reason : action dialer is disabled for now")
  @Test
  fun testOnForgotPin() {
    pinViewModel.forgotPin()
    Assert.assertEquals(pinViewModel.launchDialPad.value, "tel:XXXX")
  }

  @Test
  fun testPinLoginOnMenuLoginClicked() {
    every { secureSharedPreference.deleteSessionTokens() } just runs
    every { secureSharedPreference.deleteSessionPin() } just runs
    every { secureSharedPreference.deleteCredentials() } just runs
    pinViewModel.onMenuItemClicked(false)
    Assert.assertEquals(pinViewModel.navigateToLogin.value, true)
  }
  @Test
  fun testPinSetupOnMenuLoginClicked() {
    every { secureSharedPreference.deleteSessionTokens() } just runs
    every { secureSharedPreference.deleteSessionPin() } just runs
    every { secureSharedPreference.deleteCredentials() } just runs
    pinViewModel.onMenuItemClicked(true)
    Assert.assertEquals(pinViewModel.navigateToLogin.value, true)
  }
}
