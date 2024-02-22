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

import android.Manifest
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.util.location.LocationUtils.getAccurateLocation
import org.smartregister.fhircore.engine.util.location.PermissionUtils

class LocationService(
  @ApplicationContext val context: Context,
) {
  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private var retrievedGPSLocation: Location? = null

  fun calculateDistanceByProvidedLocations(
    destination: Location,
    currentLocation: Location,
  ): String? {
    val distanceInMeters = currentLocation.distanceTo(destination)
    return formatDistance(distanceInMeters)
  }

  fun calculateDistanceByGpsLocation(location: Resource): String? {
    val currentLocation = generateLocation(location)
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    CoroutineScope(Dispatchers.IO).launch {
      if (hasLocationPermissions()) {
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
    return if (distanceInMeters < METERS_IN_A_KILOMETER) {
      String.format("%.2f mtrs", distanceInMeters)
    } else {
      val distanceInKilometers = distanceInMeters / METERS_IN_A_KILOMETER
      String.format("%.2f km", distanceInKilometers)
    }
  }

  @VisibleForTesting
  fun calculateGpsLocation(): Location {
    return Location("StaticTestLocation").apply {
      latitude = 37.7749
      longitude = -122.4194
    }
  }

  private fun hasLocationPermissions(): Boolean {
    return PermissionUtils.checkPermissions(
      context,
      listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
      ),
    )
  }

  companion object {
    fun create(context: Context): LocationService {
      return LocationService(context)
    }

    const val METERS_IN_A_KILOMETER = 1000
  }
}
