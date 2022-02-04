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
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.PIN_KEY
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class PinViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var pinViewModel: PinViewModel

  private val testPin = MutableLiveData("1234")

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { sharedPreferencesHelper.read(any(), "") } returns "1234"
    coEvery { sharedPreferencesHelper.write(any(), "true") } returns Unit
    coEvery { sharedPreferencesHelper.remove(any()) } returns Unit

    pinViewModel =
      PinViewModel(
        dispatcher = dispatcherProvider,
        sharedPreferences = sharedPreferencesHelper,
        app = application
      )
    pinViewModel.apply {
      savedPin = "1234"
      isSetupPage = true
    }
  }

  @Test
  fun testOnPinChangeValidated() {
    pinViewModel.apply {
      savedPin = "1234"
      isSetupPage = false
    }
    pinViewModel.onPinChanged(testPin.value.toString())
    Assert.assertEquals(
      pinViewModel.sharedPreferences.read(PIN_KEY, "").toString(),
      testPin.value.toString()
    )
    Assert.assertEquals(pinViewModel.enableSetPin.value, true)
    Assert.assertEquals(pinViewModel.navigateToHome.value, true)
  }

  @Test
  fun testLoadDataForSetupScreen() {
    pinViewModel.loadData(isSetup = true)
    Assert.assertEquals(pinViewModel.isSetupPage, true)
    Assert.assertNotNull(pinViewModel.savedPin)
  }

  @Test
  fun testLoadDataForLoginScreen() {
    pinViewModel.loadData(isSetup = false)
    Assert.assertEquals(pinViewModel.isSetupPage, false)
    Assert.assertNotNull(pinViewModel.savedPin)
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

  @Test
  fun testOnForgotPin() {
    pinViewModel.forgotPin()
    Assert.assertEquals(pinViewModel.launchDialPad.value, "tel:0123456789")
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
