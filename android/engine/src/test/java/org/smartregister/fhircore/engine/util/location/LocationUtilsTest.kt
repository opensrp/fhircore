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

package org.smartregister.fhircore.engine.util.location

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest

@HiltAndroidTest
class LocationUtilsTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
  private val activityController: ActivityController<HiltActivityForTest> =
    Robolectric.buildActivity(
      HiltActivityForTest::class.java,
      Intent().apply { putExtra(HiltActivityForTest.THEME_EXTRAS_BUNDLE_KEY, R.style.AppTheme) },
    )
  private lateinit var context: Context

  @Before
  fun setUp() {
    hiltRule.inject()
    context = activityController.create().resume().get()
  }

  @Test
  fun `test isLocationEnabled when GPS provider is enabled`() {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)

    val result = LocationUtils.isLocationEnabled(context)

    assert(result)
  }

  @Test
  fun `test isLocationEnabled when Network provider is enabled`() {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

    val result = LocationUtils.isLocationEnabled(context)

    assert(result)
  }

  @Test
  @Ignore(
    "kotlin.UninitializedPropertyAccessException: lateinit property fusedLocationProviderClient has not been initialized",
  )
  fun `test getAccurateLocation`() = runTest {
    val location =
      Location("Test location").apply {
        latitude = 36.0
        longitude = 1.0
      }

    fusedLocationProviderClient.setMockLocation(location)

    val result = LocationUtils.getAccurateLocation(fusedLocationProviderClient)
    assertNotNull(result)

    assertEquals(location.latitude, result.latitude, 0.0)
    assertEquals(location.longitude, result.longitude, 0.0)
  }

  @Test
  @Ignore(
    "kotlin.UninitializedPropertyAccessException: lateinit property fusedLocationProviderClient has not been initialized",
  )
  fun `test getApproximateLocation`() = runTest {
    val location =
      Location("").apply {
        latitude = 36.0
        longitude = 1.0
      }
    fusedLocationProviderClient.setMockLocation(location)

    val result = LocationUtils.getApproximateLocation(fusedLocationProviderClient)
    assertEquals(location.latitude, result.latitude, 0.0)
    assertEquals(location.longitude, result.longitude, 0.0)
  }

  @Test
  @Ignore(
    "kotlin.UninitializedPropertyAccessException: lateinit property fusedLocationProviderClient has not been initialized",
  )
  fun `test getAccurateLocation with cancellation`() = runTest {
    val job = launch {
      delay(500)
      coroutineContext.cancel()
    }

    val result = runCatching { LocationUtils.getAccurateLocation(fusedLocationProviderClient) }

    assertEquals(true, result.isFailure)
    assertEquals(true, result.exceptionOrNull() is CancellationException)

    job.join()
  }
}
