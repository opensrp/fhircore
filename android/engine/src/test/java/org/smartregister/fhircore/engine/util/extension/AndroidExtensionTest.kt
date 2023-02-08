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

package org.smartregister.fhircore.engine.util.extension

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.LightColors
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor

class AndroidExtensionTest : RobolectricTest() {
  private lateinit var context: Application

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun `Activity#refresh() should call startActivity and finish()`() {
    val activity = spyk(AppCompatActivity())
    val intentCapture = slot<Intent>()

    every { activity.packageName } returns "package-name"
    every { activity.startActivity(any()) } just runs

    activity.refresh()

    verify { activity.startActivity(capture(intentCapture)) }
    verify { activity.finish() }

    assertEquals(activity.javaClass.name, intentCapture.captured.component?.className)
    assertEquals(activity.packageName, intentCapture.captured.component?.packageName)

    // Fixes a compose and activity test failure
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun testParseColorReturnsDefaultColorWhenStringIsEmpty() {
    assertEquals(Color.Unspecified, "".parseColor())
  }

  @Test
  fun testParseColorReturnsCorrectColorWhenHexColorIsParsed() {
    assertEquals(Color.White, "#ffffff".parseColor())
  }

  @Test
  fun testParseColorReturnsCorrectColorForThemeColorStrings() {
    assertEquals(LightColors.primary, PRIMARY_COLOR.parseColor())
    assertEquals(LightColors.primaryVariant, PRIMARY_VARIANT_COLOR.parseColor())
    assertEquals(LightColors.error, ERROR_COLOR.parseColor())
    assertEquals(DangerColor, DANGER_COLOR.parseColor())
    assertEquals(WarningColor, WARNING_COLOR.parseColor())
    assertEquals(InfoColor, INFO_COLOR.parseColor())
    assertEquals(SuccessColor, SUCCESS_COLOR.parseColor())
    assertEquals(DefaultColor, DEFAULT_COLOR.parseColor())
  }

  @Test
  fun testIsDeviceOfflineShouldReturnTrueWhenOnline() {
    val activity = mockk<HiltActivityForTest>()
    val connectivityManager = mockk<ConnectivityManager>()

    val network = mockk<Network>()
    every { connectivityManager.activeNetwork } returns network

    val networkCapabilities = spyk(NetworkCapabilities())
    every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
    every { networkCapabilities.capabilities } returns
      intArrayOf(NetworkCapabilities.TRANSPORT_WIFI)

    every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
    every { activity.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

    val isDeviceOnline = activity.isDeviceOnline()
    Assert.assertTrue(isDeviceOnline)
  }

  @Test
  fun testIsDeviceOfflineShouldReturnFalseWhenOffline() {
    val activity = mockk<HiltActivityForTest>()
    val connectivityManager = mockk<ConnectivityManager>()

    val network = mockk<Network>()
    every { connectivityManager.activeNetwork } returns network

    val networkCapabilities = spyk(NetworkCapabilities())
    every { networkCapabilities.capabilities } returns intArrayOf()

    every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
    every { activity.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

    val isDeviceOnline = activity.isDeviceOnline()
    Assert.assertFalse(isDeviceOnline)
  }
}
