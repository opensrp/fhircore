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

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.OTP_PIN

@HiltAndroidTest
class OtpLoginActivityTest : ActivityRobolectricTest() {

  private lateinit var otpLoginActivity: OtpLoginActivity

  @get:Rule var hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val testPin = MutableLiveData("1234")

  @BindValue
  val loginViewModel =
    OtpViewModel(DefaultDispatcherProvider(), mockk(), ApplicationProvider.getApplicationContext())

  @Before
  fun setUp() {
    hiltRule.inject()
    otpLoginActivity =
      spyk(Robolectric.buildActivity(OtpLoginActivity::class.java).create().resume().get())
    coEvery { otpLoginActivity.otpViewModel.savedOtp } returns "1234"
    coEvery { otpLoginActivity.otpViewModel.pin } returns testPin
  }

  @Test
  fun testNavigateToLoginShouldVerifyExpectedIntent() {
    otpLoginActivity.otpViewModel.onMenuLoginClicked()
    val expectedIntent = Intent(otpLoginActivity, LoginActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    otpLoginActivity.otpViewModel.onPinChanged("1234")
    Assert.assertEquals(
      otpLoginActivity.otpViewModel.sharedPreferences.read(OTP_PIN, "").toString(),
      testPin.value.toString()
    )
    val expectedIntent = Intent(otpLoginActivity, FamilyRegisterActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnPinChangedShowsError() {
    otpLoginActivity.otpViewModel.onPinChanged("0909")
    Assert.assertEquals(otpLoginActivity.otpViewModel.showError.value, true)
  }

  override fun getActivity(): Activity {
    return otpLoginActivity
  }
}
