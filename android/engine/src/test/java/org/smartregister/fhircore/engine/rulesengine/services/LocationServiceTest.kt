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

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.google.gson.GsonBuilder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class LocationServiceTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private lateinit var context: Context
  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private lateinit var locationService: LocationService

  @Before
  fun setup() {
    hiltRule.inject()
    context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
    sharedPreferencesHelper =
      SharedPreferencesHelper(
        context,
        GsonBuilder().registerTypeAdapter(Instant::class.java, InstantTypeAdapter()).create(),
      )
    locationService = LocationService.create(context, sharedPreferencesHelper)
  }

  @Test
  fun calculateDistanceByProvidedLocations_shouldReturnCorrectDistance() {
    val destination = generateTestLocations(latitude = 10.0, longitude = 20.0)

    val currentLocation = generateTestLocations(latitude = 12.0, longitude = 25.0)

    val result = locationService.calculateDistanceByProvidedLocations(destination, currentLocation)
    assertEquals("Unexpected distance calculation", "589.49 km", result)
  }

  private fun generateTestLocations(longitude: Double, latitude: Double): Location {
    val location = Location("test Location")
    location.longitude = longitude
    location.latitude = latitude
    return location
  }

  @Test
  fun calculateDistancesByGpsLocation_shouldReturnCorrectDistance() {
    val locationTest = LocationCoordinates(37.7749, -122.4194, 0.0, Instant.now())
    locationService.writeLocation(locationTest)
    assertNotNull(
      sharedPreferencesHelper.read<LocationCoordinates>(
        key = SharedPreferenceKey.GEO_LOCATION.name,
      ),
    )
    val currentLocation =
      org.hl7.fhir.r4.model.Location().apply {
        name = "ServicePoint"
        position.latitude = BigDecimal(37.7749)
        position.longitude = BigDecimal(-122.4194)
      }
    val result = locationService.calculateDistancesByGpsLocation(currentLocation)

    assertEquals("0.00 mtrs", "0.00 mtrs", result)
  }
}
