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
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.OTP_PIN

@HiltAndroidTest
class OtpSetupActivityTest : ActivityRobolectricTest() {

  private lateinit var otpSetupActivity: OtpSetupActivity

  @get:Rule var hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val testPin = MutableLiveData("1234")

  @BindValue
  val loginViewModel =
    OtpViewModel(DefaultDispatcherProvider(), mockk(), ApplicationProvider.getApplicationContext())

  @Before
  fun setUp() {
    hiltRule.inject()
    otpSetupActivity =
      spyk(Robolectric.buildActivity(OtpSetupActivity::class.java).create().resume().get())
    coEvery { otpSetupActivity.otpViewModel.savedOtp } returns "1234"
    coEvery { otpSetupActivity.otpViewModel.pin } returns testPin
  }

  @Test
  fun testNavigateToSettingShouldVerifyExpectedIntent() {
    otpSetupActivity.otpViewModel.onMenuSettingClicked()
    val expectedIntent = Intent(otpSetupActivity, AppSettingActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    otpSetupActivity.otpViewModel.onPinConfirmed()
    Assert.assertEquals(
      otpSetupActivity.otpViewModel.sharedPreferences.read(OTP_PIN, "").toString(),
      testPin.value.toString()
    )
    val expectedIntent = Intent(otpSetupActivity, FamilyRegisterActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return otpSetupActivity
  }
}
