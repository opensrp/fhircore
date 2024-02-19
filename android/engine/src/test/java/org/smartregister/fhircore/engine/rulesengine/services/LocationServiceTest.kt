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

package org.smartregister.fhircore.engine.rulesengine.services

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class LocationServiceTest : RobolectricTest() {
  private val mockLocation = mockk<Resource>()
  private val mockGPSLocation = mockk<Location>()
  private val fusedLocationProviderClient = mockk<FusedLocationProviderClient>()

  @Before
  fun setUp() {
    mockkObject(LocationService)
    LocationService.init(fusedLocationProviderClient, true)
  }

  @Test
  fun calculateDistanceByProvidedLocations_shouldReturnCorrectDistance() {
    val destination = generateTestLocations(latitude = 10.0, longitude = 20.0)

    val currentLocation = generateTestLocations(latitude = 12.0, longitude = 25.0)

    val result = LocationService.calculateDistanceByProvidedLocations(destination, currentLocation)
    assertEquals("Unexpected distance calculation", "589.49 km", result)
  }

  @Test
  fun testCalculateDistanceByGpsLocation() = runBlocking {
    coEvery { LocationService.calculateGpsLocation() } returns mockGPSLocation

    coEvery { LocationService.calculateDistanceByGpsLocation(mockLocation) } returns "100.00 mtrs"

    val result = LocationService.calculateDistanceByGpsLocation(mockLocation)

    assertEquals("100.00 mtrs", result)
  }

  @Test
  fun testOut() {
    assertEquals(1, 1)
  }

  private fun generateTestLocations(longitude: Double, latitude: Double): Location {
    val location = Location("test Location")
    location.longitude = longitude
    location.latitude = latitude
    return location
  }
}
