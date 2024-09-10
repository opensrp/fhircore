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

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import timber.log.Timber

object LocationUtils {

  fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
      locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
  }

  @SuppressLint("MissingPermission")
  suspend fun getAccurateLocation(fusedLocationClient: FusedLocationProviderClient): Location {
    return suspendCoroutine { continuation ->
      fusedLocationClient
        .getCurrentLocation(
          LocationRequest.PRIORITY_HIGH_ACCURACY,
          object : CancellationToken() {
            override fun onCanceledRequested(p0: OnTokenCanceledListener) =
              CancellationTokenSource().token

            override fun isCancellationRequested() = false
          },
        )
        .addOnSuccessListener { location: Location? ->
          if (location != null) {
            Timber.d(
              "Accurate location - lat: ${location.latitude}; long: ${location.longitude}; alt: ${location.altitude}",
            )
            continuation.resume(location)
          }
        }
        .addOnFailureListener { e ->
          Timber.e(e, "Failed to get accurate location")
          continuation.resumeWithException(e)
        }
    }
  }

  @SuppressLint("MissingPermission")
  suspend fun getApproximateLocation(fusedLocationClient: FusedLocationProviderClient): Location {
    return suspendCoroutine { continuation ->
      fusedLocationClient
        .getCurrentLocation(
          LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
          object : CancellationToken() {
            override fun onCanceledRequested(p0: OnTokenCanceledListener) =
              CancellationTokenSource().token

            override fun isCancellationRequested() = false
          },
        )
        .addOnSuccessListener { location: Location? ->
          if (location != null) {
            Timber.d(
              "Approximate location - lat: ${location.latitude}; long: ${location.longitude}; alt: ${location.altitude}",
            )
            continuation.resume(location)
          }
        }
        .addOnFailureListener { e -> continuation.resumeWithException(e) }
    }
  }
}
