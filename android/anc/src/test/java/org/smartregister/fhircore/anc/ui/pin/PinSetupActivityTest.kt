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
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class PinSetupActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val testPin = MutableLiveData("1234")

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()

  private lateinit var pinViewModel: PinViewModel

  private lateinit var pinSetupActivity: PinSetupActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    coEvery { sharedPreferencesHelper.read(any(), "") } returns "1234"
    coEvery { sharedPreferencesHelper.write(any(), "true") } returns Unit
    coEvery { sharedPreferencesHelper.remove(any()) } returns Unit
    pinViewModel = mockk()
    coEvery { pinViewModel.savedPin } returns "1234"
    coEvery { sharedPreferencesHelper.write(any(), "false") } returns Unit
    coEvery { pinViewModel.pin } returns testPin
    pinSetupActivity =
      Robolectric.buildActivity(PinSetupActivity::class.java).create().resume().get()
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
  fun testNavigateToSettingShouldVerifyExpectedIntent() {
    pinSetupActivity.pinViewModel.onMenuSettingClicked()
    val expectedIntent = Intent(pinSetupActivity, AppSettingActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    pinSetupActivity.pinViewModel.onPinConfirmed()
    Assert.assertEquals("1234", testPin.value.toString())
    Assert.assertEquals(false, pinSetupActivity.pinViewModel.showError.value)
  }

  @Test
  fun testHandleBackClickedShouldCallFinishMethod() {
    pinSetupActivity.pinViewModel.onAppBackClick()
    Assert.assertTrue(pinSetupActivity.isFinishing)
  }

  override fun getActivity(): Activity {
    return pinSetupActivity
  }
}
