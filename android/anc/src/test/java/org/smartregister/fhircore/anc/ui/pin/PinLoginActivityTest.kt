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
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.util.PIN_KEY
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class PinLoginActivityTest : ActivityRobolectricTest() {

  private lateinit var pinLoginActivity: PinLoginActivity

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val testPin = MutableLiveData("1234")

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()

  private lateinit var pinViewModel: PinViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    coEvery { sharedPreferencesHelper.read(any(), "") } returns "1234"
    coEvery { sharedPreferencesHelper.write(any(), "true") } returns Unit
    pinViewModel = mockk()
    coEvery { pinViewModel.savedPin } returns "1234"
    coEvery { pinViewModel.pin } returns testPin
    pinLoginActivity =
      Robolectric.buildActivity(PinLoginActivity::class.java).create().resume().get()
  }

  @Test
  fun testNavigateToLoginShouldVerifyExpectedIntent() {
    pinLoginActivity.pinViewModel.onMenuLoginClicked()
    val expectedIntent = Intent(pinLoginActivity, LoginActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    pinLoginActivity.pinViewModel.onPinChanged("1234")
    Assert.assertEquals(
      pinLoginActivity.pinViewModel.sharedPreferences.read(PIN_KEY, "").toString(),
      testPin.value.toString()
    )
    val expectedIntent = Intent(pinLoginActivity, FamilyRegisterActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnPinChangedShowsError() {
    pinLoginActivity.pinViewModel.onPinChanged("0909")
    Assert.assertEquals(pinLoginActivity.pinViewModel.showError.value, true)
  }

  override fun getActivity(): Activity {
    return pinLoginActivity
  }
}
