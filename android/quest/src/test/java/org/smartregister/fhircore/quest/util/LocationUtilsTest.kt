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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class LocationUtilsTest {
  private lateinit var context: Context
  private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

  @Before
  fun setUp() {
    context = mockk(relaxed = true)
    fusedLocationProviderClient = mockk(relaxed = true)
    mockkObject(LocationUtils)
  }

  @Test
  fun `test isLocationEnabled when GPS provider is enabled`() {
    val locationManager = mockk<LocationManager>()
    every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
    every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
    every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns false

    val result = LocationUtils.isLocationEnabled(context)

    assert(result)
  }

  @Test
  fun `test isLocationEnabled when Network provider is enabled`() {
    val locationManager = mockk<LocationManager>()
    every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
    every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false
    every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true

    val result = LocationUtils.isLocationEnabled(context)

    assert(result)
  }

  @Test
  fun `test getAccurateLocation`() = runBlocking {
    val location = mockk<Location>()

    coEvery { LocationUtils.getAccurateLocation(fusedLocationProviderClient) } returns location

    val result = LocationUtils.getAccurateLocation(fusedLocationProviderClient)

    assertEquals(location, result)
  }

  @Test
  fun `test getApproximateLocation`() = runBlocking {
    val location = mockk<Location>()

    coEvery { LocationUtils.getApproximateLocation(fusedLocationProviderClient) } returns location

    val result = LocationUtils.getApproximateLocation(fusedLocationProviderClient)

    assertEquals(location, result)
  }
}
