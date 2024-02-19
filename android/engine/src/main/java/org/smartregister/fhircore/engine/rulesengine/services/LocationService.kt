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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.util.location.LocationUtils.Companion.getAccurateLocation

object LocationService {
  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private var locationEnabled: Boolean = false
  private var retrievedGPSLocation: Location? = null

  fun init(fusedLocationProviderClient: FusedLocationProviderClient, isLocationEnabled: Boolean) {
    locationEnabled = isLocationEnabled
    fusedLocationClient = fusedLocationProviderClient
  }

  fun calculateDistanceByProvidedLocations(
    destination: Location,
    currentLocation: Location,
  ): String? {
    val distanceInMeters = currentLocation.distanceTo(destination)
    return formatDistance(distanceInMeters)
  }

  fun calculateDistanceByGpsLocation(location: Resource): String? {
    val currentLocation = generateLocation(location)
    CoroutineScope(Dispatchers.IO).launch {
      if (locationEnabled) {
        retrievedGPSLocation = getAccurateLocation(fusedLocationClient)
      }
    }
    val distanceInMeters = retrievedGPSLocation?.let { calculateDistance(currentLocation!!, it) }
    return distanceInMeters?.let { formatDistance(it) }
  }

  private fun generateLocation(location: Resource): Location? {
    return (location as? org.hl7.fhir.r4.model.Location)?.let {
      Location("CustomLocationProvider").apply {
        longitude = it.position.longitude.toDouble()
        latitude = it.position.latitude.toDouble()
      }
    }
  }

  private fun calculateDistance(locationA: Location, locationB: Location): Float {
    val resultArray = FloatArray(1)
    Location.distanceBetween(
      locationA.latitude,
      locationA.longitude,
      locationB.latitude,
      locationB.longitude,
      resultArray,
    )
    return resultArray[0]
  }

  private fun formatDistance(distanceInMeters: Float): String {
    return if (distanceInMeters < 1000) {
      String.format("%.2f mtrs", distanceInMeters)
    } else {
      val distanceInKilometers = distanceInMeters / 1000.0
      String.format("%.2f km", distanceInKilometers)
    }
  }

  @VisibleForTesting
  fun calculateGpsLocation(): Location {
    // For testing purposes, return a predefined static location
    return Location("StaticTestLocation").apply {
      latitude = 37.7749
      longitude = -122.4194
    } // Adjust the expected result based on your distance calculation
  }
}
