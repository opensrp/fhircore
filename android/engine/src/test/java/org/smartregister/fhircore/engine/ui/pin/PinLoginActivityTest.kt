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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class PinLoginActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val testPin = MutableLiveData("1234")

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  private lateinit var pinViewModel: PinViewModel
  private lateinit var pinLoginActivity: PinLoginActivity
  private lateinit var pinLoginActivitySpy: PinLoginActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    coEvery { sharedPreferencesHelper.read(any(), "") } returns "1234"
    coEvery { sharedPreferencesHelper.read(any(), false) } returns false
    coEvery { sharedPreferencesHelper.write(any(), true) } returns Unit
    coEvery { sharedPreferencesHelper.write(any(), false) } returns Unit
    coEvery { sharedPreferencesHelper.remove(any()) } returns Unit
    coEvery { secureSharedPreference.retrieveSessionUsername() } returns "demo"
    coEvery { secureSharedPreference.saveSessionPin("1234") } returns Unit
    coEvery { secureSharedPreference.retrieveSessionPin() } returns "1234"

    pinViewModel = mockk()
    coEvery { pinViewModel.savedPin } returns "1234"
    coEvery { pinViewModel.enterUserLoginMessage } returns "demo"
    coEvery { pinViewModel.pin } returns testPin
    every { pinViewModel.appName } returns "Anc"

    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    pinLoginActivity =
      Robolectric.buildActivity(PinLoginActivity::class.java).create().resume().get()

    pinLoginActivitySpy = spyk(pinLoginActivity, recordPrivateCalls = true)
    every { pinLoginActivitySpy.finish() } returns Unit
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(getActivity())
  }

  @Test
  fun testNavigateToLoginShouldVerifyExpectedIntent() {
    pinLoginActivity.pinViewModel.onMenuLoginClicked(FORCE_LOGIN_VIA_USERNAME)
    val expectedIntent = Intent(pinLoginActivity, LoginActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Ignore("reason : action dialer is disabled for now")
  @Test
  fun testNavigateToCallDialerShouldVerifyExpectedIntent() {
    pinLoginActivity.pinViewModel.forgotPin()
    val expectedIntent = Intent(Intent.ACTION_DIAL)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Ignore("temp ignore in PR")
  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    pinLoginActivity.pinViewModel.onPinChanged("1234")
    Assert.assertEquals(
      pinLoginActivity.pinViewModel.secureSharedPreference.retrieveSessionPin()!!,
      testPin.value.toString()
    )
  }

  override fun getActivity(): Activity {
    return pinLoginActivity
  }
}
