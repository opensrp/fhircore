/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import android.content.Intent
import androidx.compose.material.ExperimentalMaterialApi
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowIntent
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.login.LoginActivity
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.p2p.P2PLibrary

@HiltAndroidTest
class PinLoginActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @BindValue val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private val pinLoginActivityController = Robolectric.buildActivity(PinLoginActivity::class.java)
  private lateinit var pinLoginActivity: PinLoginActivity

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    pinLoginActivity = spyk(pinLoginActivityController.create().resume().get())
  }

  @After
  override fun tearDown() {
    pinLoginActivityController.destroy()
  }

  @Test
  fun testThaPinUiStateIsUpdatedWithActivityLaunch() {
    val pinUiState = pinLoginActivity.pinViewModel.pinUiState.value
    Assert.assertTrue(pinUiState.appName.isNotEmpty())
  }

  @Test
  fun testDialPadLaunched() {
    val phoneNumber = "1234567890"
    pinLoginActivity.pinViewModel.launchDialPad.value = phoneNumber
    val resultIntent = Shadows.shadowOf(pinLoginActivity).nextStartedActivity
    Assert.assertEquals(Intent.ACTION_DIAL, resultIntent.action)
    Assert.assertEquals(phoneNumber, resultIntent.data?.schemeSpecificPart.toString())
  }

  @Test
  fun testNavigateToSettingLaunchesAppSettingActivity() {
    // Simulate clicking the settings menu option from overflow dropdown menu
    pinLoginActivity.pinViewModel.onMenuItemClicked(true)
    val resultIntent = Shadows.shadowOf(pinLoginActivity).nextStartedActivity
    Assert.assertNotNull(resultIntent)
    val shadowIntent: ShadowIntent = Shadows.shadowOf(resultIntent)
    Assert.assertEquals(AppSettingActivity::class.java, shadowIntent.intentClass)
  }

  @Test
  fun testNavigateToLoginLaunchesLoginActivity() {
    // Simulate clicking the login menu option from overflow dropdown menu
    pinLoginActivity.pinViewModel.onMenuItemClicked(false)
    val resultIntent = Shadows.shadowOf(pinLoginActivity).nextStartedActivity
    Assert.assertNotNull(resultIntent)
    val shadowIntent: ShadowIntent = Shadows.shadowOf(resultIntent)
    Assert.assertEquals(LoginActivity::class.java, shadowIntent.intentClass)
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun testNavigateToHomeLaunchesAppMainActivity() = runBlocking {
    // Mock p2p Library then un mock it at the end of test
    mockkObject(P2PLibrary)
    every { P2PLibrary.init(any()) } returns mockk()

    // When new pin is setup the app navigates to home screen
    pinLoginActivity.pinViewModel.onSetPin("1234".toCharArray())
    pinLoginActivity.pinViewModel.navigateToHome.observeForever { isNavigating ->
      if (isNavigating == true) {
        val resultIntent = Shadows.shadowOf(pinLoginActivity).nextStartedActivity
        Assert.assertNotNull(resultIntent)
        val shadowIntent: ShadowIntent = Shadows.shadowOf(resultIntent)
        Assert.assertEquals(AppMainActivity::class.java, shadowIntent.intentClass)
      }
    }

    unmockkObject(P2PLibrary)
  }
}
