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

package org.smartregister.fhircore.quest.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class LocationUtilsTest : RobolectricTest() {
  private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
  private val activityController: ActivityController<HiltActivityForTest> =
    Robolectric.buildActivity(HiltActivityForTest::class.java)
  private lateinit var context: HiltActivityForTest

  @Before
  fun setUp() {
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
  fun `test getAccurateLocation`() = runBlocking {
    val location =
      Location("").apply {
        latitude = 36.0
        longitude = 1.0
      }
    fusedLocationProviderClient.setMockLocation(location)

    val result = LocationUtils.getAccurateLocation(fusedLocationProviderClient, coroutineContext)

    assertEquals(location.latitude, result!!.latitude, 0.0)
    assertEquals(location.longitude, result.longitude, 0.0)
  }

  @Test
  fun `test getApproximateLocation`() = runBlocking {
    val location =
      Location("").apply {
        latitude = 36.0
        longitude = 1.0
      }
    fusedLocationProviderClient.setMockLocation(location)

    val result = LocationUtils.getApproximateLocation(fusedLocationProviderClient, coroutineContext)
    assertEquals(location.latitude, result!!.latitude, 0.0)
    assertEquals(location.longitude, result.longitude, 0.0)
  }

  @Test
  fun `test getAccurateLocation with cancellation`() = runBlocking {
    val job = launch {
      delay(500)
      coroutineContext.cancel()
    }

    val result = runCatching {
      LocationUtils.getAccurateLocation(fusedLocationProviderClient, coroutineContext)
    }

    assertEquals(true, result.isFailure)
    assertEquals(true, result.exceptionOrNull() is CancellationException)

    job.join()
  }
}
