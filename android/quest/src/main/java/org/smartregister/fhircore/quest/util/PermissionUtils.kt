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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object PermissionUtils {

  fun checkPermissions(context: Context, permissions: List<String>): Boolean {
    for (permission in permissions) {
      if (
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
      ) {
        return false
      }
    }
    return true
  }

  fun getLocationPermissionLauncher(
    activity: AppCompatActivity,
    onFineLocationPermissionGranted: () -> Unit,
    onCoarseLocationPermissionGranted: () -> Unit,
    onLocationPermissionDenied: () -> Unit,
  ): ActivityResultLauncher<Array<String>> {
    return activity.registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
      if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
        onFineLocationPermissionGranted()
      } else if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
        onCoarseLocationPermissionGranted()
      } else {
        onLocationPermissionDenied()
      }
    }
  }

  fun getStartActivityForResultLauncher(
    activity: AppCompatActivity,
    onResult: (resultCode: Int, data: Intent?) -> Unit,
  ): ActivityResultLauncher<Intent> {
    return activity.registerForActivityResult(
      ActivityResultContracts.StartActivityForResult(),
    ) { result ->
      onResult(result.resultCode, result.data)
    }
  }
}
