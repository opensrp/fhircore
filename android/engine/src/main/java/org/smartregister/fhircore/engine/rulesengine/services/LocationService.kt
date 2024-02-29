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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.location.LocationUtils.getAccurateLocation
import org.smartregister.fhircore.engine.util.location.LocationUtils.getApproximateLocation
import org.smartregister.fhircore.engine.util.location.PermissionUtils.hasCoarseLocationPermissions
import org.smartregister.fhircore.engine.util.location.PermissionUtils.hasFineLocationPermissions
import javax.inject.Inject

class LocationService(
  @ApplicationContext val context: Context,
  val sharedPreferences: SharedPreferencesHelper,
) {
  lateinit var fusedLocationProviderClient: FusedLocationProviderClient
  fun calculateDistanceByProvidedLocations(
    destination: Location,
    currentLocation: Location,
  ): String {
    val distanceInMeters = currentLocation.distanceTo(destination)
    return formatDistance(distanceInMeters)
  }

  fun calculateDistancesByGpsLocation(location: Resource): String {
    val currentLocation = generateLocation(location)

    CoroutineScope(Dispatchers.IO).launch {
      fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

      val retrievedLocation =
        if (hasFineLocationPermissions(context)) {
          getAccurateLocation(fusedLocationProviderClient, Dispatchers.IO)
        } else if (hasCoarseLocationPermissions(context)) {
          getApproximateLocation(fusedLocationProviderClient, Dispatchers.IO)
        } else {
          null
        }

      retrievedLocation?.let {
        writeLocation(LocationCoordinates(it.latitude, it.longitude, it.altitude, Instant.now()))
      }
    }

    val locationTest =
      sharedPreferences.read<LocationCoordinates>(key = SharedPreferenceKey.GEO_LOCATION.name)

    val generatedLocation =
      Location("Test location").apply {
        if (locationTest != null && isWithinLast30Minutes(locationTest.timeStamp)) {
          longitude = locationTest.longitude!!
          latitude = locationTest.latitude!!
        }
      }

    val distanceInMeters = calculateDistance(generatedLocation, currentLocation)
    return formatDistance(distanceInMeters)
  }

  fun generateLocation(location: Resource): Location {
    return (location as? org.hl7.fhir.r4.model.Location).let {
      Location("CustomLocationProvider").apply {
        if (it != null) {
          longitude = it.position.longitude.toDouble()
          latitude = it.position.latitude.toDouble()
        }
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

  fun writeLocation(
    location: LocationCoordinates,
  ) {
    sharedPreferences.write(
      key = SharedPreferenceKey.GEO_LOCATION.name,
      value = location,
    )
  }

  private fun isWithinLast30Minutes(timeStamp: Instant?): Boolean {
    val thirtyMinutesAgo = Instant.now().minusSeconds(1800)
    return timeStamp != null && timeStamp > thirtyMinutesAgo
  }


  companion object {
    fun create(
      context: Context,
      sharedPreferencesHelper: SharedPreferencesHelper,
    ): LocationService {
      return LocationService(context, sharedPreferencesHelper)
    }

    const val METERS_IN_A_KILOMETER = 1000
  }
}
