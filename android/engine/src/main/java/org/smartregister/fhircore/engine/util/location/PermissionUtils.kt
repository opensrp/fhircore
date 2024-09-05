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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {

  fun checkPermissions(context: Context, permissions: List<String>): Boolean =
    permissions.none {
      ContextCompat.checkSelfPermission(
        context,
        it,
      ) != PackageManager.PERMISSION_GRANTED
    }

  fun getLocationPermissionLauncher(
    permissions: Map<String, Boolean>,
    onFineLocationPermissionGranted: () -> Unit,
    onCoarseLocationPermissionGranted: () -> Unit,
    onLocationPermissionDenied: () -> Unit,
  ) {
    when {
      permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ->
        onFineLocationPermissionGranted()
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ->
        onCoarseLocationPermissionGranted()
      else -> onLocationPermissionDenied()
    }
  }

  fun hasFineLocationPermissions(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED

  fun hasCoarseLocationPermissions(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED

  fun hasLocationPermissions(context: Context) =
    checkPermissions(
      context,
      listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
      ),
    )
}
