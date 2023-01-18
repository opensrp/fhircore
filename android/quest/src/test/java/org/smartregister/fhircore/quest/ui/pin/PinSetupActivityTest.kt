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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
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
import javax.inject.Inject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.login.LoginActivity

@HiltAndroidTest
class PinSetupActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @BindValue
  var configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val testPin = MutableLiveData("1234")

  private lateinit var pinViewModel: PinViewModel

  private lateinit var pinSetupActivity: PinSetupActivity

  private lateinit var pinSetupActivitySpy: PinSetupActivity

  @Before
  fun setUp() {
    hiltRule.inject()

    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    val controller = Robolectric.buildActivity(PinSetupActivity::class.java)
    pinSetupActivity = controller.create().resume().get()

    pinSetupActivitySpy = spyk(pinSetupActivity, recordPrivateCalls = true)
    every { pinSetupActivitySpy.finish() } returns Unit

    pinViewModel = mockk()
    every { pinViewModel.pinUiState } returns
      mutableStateOf(
        PinUiState(
          savedPin = "1234",
          enterUserLoginMessage = "demo",
        )
      )
    coEvery { pinViewModel.pin } returns testPin
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
  fun testNavigateToLoginShouldVerifyExpectedIntent() {
    pinSetupActivity.pinViewModel.onMenuLoginClicked(true)
    val expectedIntent = Intent(pinSetupActivity, LoginActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return pinSetupActivity
  }
}
