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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.PinViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.pinViewConfigurationOf
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class PinViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  @Inject lateinit var accountAuthenticator: AccountAuthenticator
  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var pinViewModel: PinViewModel

  private val testPin = MutableLiveData("1234")
  val testPinViewConfiguration =
    PinViewConfiguration(
      appId = "ancApp",
      classification = "classification",
      applicationName = "Test App",
      appLogoIconResourceFile = "ic_launcher",
      enablePin = true,
      showLogo = true
    )

  @Before
  fun setUp() {
    hiltRule.inject()

    configurationRegistry.loadAppConfigurations("appId", accountAuthenticator) {}

    coEvery { sharedPreferencesHelper.read(any(), "") } returns "1234"
    coEvery { sharedPreferencesHelper.write(FORCE_LOGIN_VIA_USERNAME, true) } returns Unit
    coEvery { sharedPreferencesHelper.remove(any()) } returns Unit
    coEvery { secureSharedPreference.retrieveSessionUsername() } returns "demo"
    coEvery { secureSharedPreference.saveSessionPin("1234") } returns Unit
    coEvery { secureSharedPreference.retrieveSessionPin() } returns "1234"

    pinViewModel =
      PinViewModel(
        dispatcher = dispatcherProvider,
        sharedPreferences = sharedPreferencesHelper,
        secureSharedPreference = secureSharedPreference,
        configurationRegistry = configurationRegistry,
        app = application
      )
    pinViewModel.apply {
      savedPin = "1234"
      isSetupPage = true
      appName = "demo"
      appLogoResFile = "ic_launcher"
      onPinChanged("1234")
      pinViewConfiguration = testPinViewConfiguration
    }
  }

  @Test
  fun testPinViewConfiguration() {
    val expectedPinConfig =
      pinViewConfigurationOf(
        appId = "ancApp",
        classification = "classification",
        applicationName = "Test App",
        appLogoIconResourceFile = "ic_launcher",
        enablePin = true,
        showLogo = true
      )
    Assert.assertEquals(expectedPinConfig.appId, testPinViewConfiguration.appId)
    Assert.assertEquals(expectedPinConfig.classification, testPinViewConfiguration.classification)
    Assert.assertEquals(expectedPinConfig.applicationName, testPinViewConfiguration.applicationName)
    Assert.assertEquals(
      expectedPinConfig.appLogoIconResourceFile,
      testPinViewConfiguration.appLogoIconResourceFile
    )
    Assert.assertEquals(expectedPinConfig.enablePin, testPinViewConfiguration.enablePin)
    Assert.assertEquals(expectedPinConfig.showLogo, testPinViewConfiguration.showLogo)
  }

  @Test
  fun testOnPinChangeValidated() {
    pinViewModel.apply {
      savedPin = "1234"
      isSetupPage = false
    }
    pinViewModel.onPinChanged(testPin.value.toString())
    Assert.assertEquals(pinViewModel.savedPin, testPin.value.toString())
    Assert.assertEquals(pinViewModel.enableSetPin.value, true)
    Assert.assertEquals(pinViewModel.navigateToHome.value, true)
  }

  @Test
  fun testOnPinConfirmed() {
    pinViewModel.onPinConfirmed()
    Assert.assertEquals(
      pinViewModel.secureSharedPreference.retrieveSessionPin()!!,
      testPin.value.toString()
    )
    Assert.assertEquals(pinViewModel.showError.value, false)
  }

  @Test
  fun testOnPinConfirmedValidated() {
    pinViewModel.onPinConfirmed()
    Assert.assertEquals(
      pinViewModel.secureSharedPreference.retrieveSessionPin()!!,
      testPin.value.toString()
    )
    Assert.assertEquals(pinViewModel.showError.value, false)
    Assert.assertEquals(pinViewModel.navigateToHome.value, true)
  }

  @Test
  fun testOnAppBackCLicked() {
    pinViewModel.onAppBackClick()
    Assert.assertEquals(pinViewModel.onBackClick.value, true)
  }

  @Test
  fun testLoadData() {
    pinViewModel.loadData(isSetup = true)
    Assert.assertEquals(pinViewModel.isSetupPage, true)
    Assert.assertNotNull(pinViewModel.savedPin)
    Assert.assertNotNull(pinViewModel.enterUserLoginMessage)
  }

  @Test
  fun testLoadDataForLoginScreen() {
    pinViewModel.loadData(isSetup = false)
    Assert.assertEquals(pinViewModel.isSetupPage, false)
    Assert.assertNotNull(pinViewModel.savedPin)
    Assert.assertEquals("demo", pinViewModel.retrieveUsername())
  }

  @Test
  fun testOnPinChangeError() {
    pinViewModel.onPinChanged("3232")
    Assert.assertEquals(pinViewModel.showError.value, true)
  }

  @Test
  fun testOnPinChangeInvalid() {
    pinViewModel.onPinChanged("32")
    Assert.assertEquals(pinViewModel.showError.value, false)
    Assert.assertEquals(pinViewModel.enableSetPin.value, false)
  }

  @Ignore("reason : action dialer is disabled for now")
  @Test
  fun testOnForgotPin() {
    pinViewModel.forgotPin()
    Assert.assertEquals(pinViewModel.launchDialPad.value, "tel:XXXX")
  }

  @Test
  fun testOnMenuLoginClicked() {
    pinViewModel.onMenuLoginClicked()
    Assert.assertEquals(pinViewModel.navigateToLogin.value, true)
  }

  @Test
  fun testOnMenuSettingsClicked() {
    pinViewModel.onMenuSettingClicked()
    Assert.assertEquals(pinViewModel.navigateToSettings.value, true)
  }
}
