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
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.datastore.ProtoDataStore
import org.smartregister.fhircore.engine.datastore.locationCoordinatesDatastore
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.location.LocationUtils.getAccurateLocation
import org.smartregister.fhircore.engine.util.location.LocationUtils.getApproximateLocation
import org.smartregister.fhircore.engine.util.location.PermissionUtils.hasCoarseLocationPermissions
import org.smartregister.fhircore.engine.util.location.PermissionUtils.hasFineLocationPermissions

typealias FhirLocation = org.hl7.fhir.r4.model.Location

@Singleton
class LocationService
@Inject
constructor(
  @ApplicationContext val context: Context,
  val dispatcherProvider: DispatcherProvider,
  val protoDataStore: ProtoDataStore,
) {

  private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

  fun calculateDistanceBetweenLocations(
    destination: Location,
    currentLocation: Location,
  ): String {
    val distanceInMeters = currentLocation.distanceTo(destination)
    return formatDistance(distanceInMeters)
  }

  fun calculateDistanceByGpsLocation(location: Resource): String {
    val currentLocation = generateLocation(location)
    CoroutineScope(dispatcherProvider.main()).launch {
      val retrievedLocation =
        if (hasFineLocationPermissions(context)) {
          getAccurateLocation(fusedLocationProviderClient)
        } else if (hasCoarseLocationPermissions(context)) {
          getApproximateLocation(fusedLocationProviderClient)
        } else {
          null
        }

      retrievedLocation?.let {
        protoDataStore.writeLocationCoordinates(
          LocationCoordinate(it.latitude, it.longitude, it.altitude, Instant.now()),
        )
      }
    }

    val locationCoordinate = runBlocking { context.locationCoordinatesDatastore.data.firstOrNull() }

    val generatedLocation =
      Location("").apply {
        if (locationCoordinate != null) {
          longitude = locationCoordinate.longitude!!
          latitude = locationCoordinate.latitude!!
        }
      }

    val distanceInMeters = calculateDistance(generatedLocation, currentLocation)
    return formatDistance(distanceInMeters)
  }

  private fun generateLocation(location: Resource): Location {
    return (location as? FhirLocation).let {
      Location("").apply {
        if (it != null) {
          longitude = it.position.longitude.toDouble()
          latitude = it.position.latitude.toDouble()
        }
      }
    }
  }

  private fun calculateDistance(startLocation: Location, endLocation: Location): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
      startLocation.latitude,
      startLocation.longitude,
      endLocation.latitude,
      endLocation.longitude,
      results,
    )
    return results[0]
  }

  private fun formatDistance(distanceInMeters: Float): String {
    return if (distanceInMeters < METERS_IN_A_KILOMETER) {
      String.format("%.2f mtrs", distanceInMeters)
    } else {
      val distanceInKilometers = distanceInMeters / METERS_IN_A_KILOMETER
      String.format("%.2f km", distanceInKilometers)
    }
  }

  private fun isWithinLast30Minutes(timeStamp: Instant?): Boolean {
    val thirtyMinutesAgo = Instant.now().minusSeconds(THIRTY_MINUTES_T0_SECONDS.toLong())
    return timeStamp != null && timeStamp > thirtyMinutesAgo
  }

  companion object {
    const val METERS_IN_A_KILOMETER = 1000
    const val THIRTY_MINUTES_T0_SECONDS = 1800
  }
}
