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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.robolectric.AccountManagerShadow
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.OTP_PIN
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
@Config(shadows = [AccountManagerShadow::class])
internal class OtpViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private lateinit var otpViewModel: OtpViewModel

  private val testPin = MutableLiveData("1234")

  @Before
  fun setUp() {
    hiltRule.inject()
    // Spy needed to control interaction with the real injected dependency

    otpViewModel =
      OtpViewModel(
        dispatcher = dispatcherProvider,
        sharedPreferences = sharedPreferencesHelper,
        app = ApplicationProvider.getApplicationContext()
      )

    coEvery { otpViewModel.savedOtp } returns "1234"
    coEvery { otpViewModel.pin } returns testPin
  }

  @Test
  fun testOnPinChangeValidated() {
    otpViewModel.run { loadData() }
    otpViewModel.onPinChanged(testPin.value.toString())
    Assert.assertEquals(
      otpViewModel.sharedPreferences.read(OTP_PIN, "").toString(),
      testPin.value.toString()
    )
  }

  @Test
  fun testOnPinChangeError() {
    otpViewModel.run { loadData() }
    otpViewModel.onPinChanged("3232")
    Assert.assertEquals(otpViewModel.showError.value, true)
  }

  @Test
  fun testOnMenuLoginClicked() {
    otpViewModel.onMenuLoginClicked()
    Assert.assertEquals(otpViewModel.navigateToLogin.value, true)
  }

  @Test
  fun testOnMenuSettingsClicked() {
    otpViewModel.onMenuSettingClicked()
    Assert.assertEquals(otpViewModel.showError.value, true)
  }

  @Test
  fun testOnPinConfirmed() {
    otpViewModel.onPinConfirmed()
    Assert.assertEquals(otpViewModel.navigateToHome.value, true)
  }
}
