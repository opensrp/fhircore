/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class NetworkState(private val context: Context) {

  @Suppress("DEPRECATION")
  operator fun invoke(): Boolean {

    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val activeNetwork = connectivityManager.activeNetwork ?: return false
      val networkCapability =
        connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
      return when {
        networkCapability.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
          networkCapability.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
          networkCapability.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
      }
    } else {
      connectivityManager.activeNetworkInfo?.run {
        return when (type) {
          ConnectivityManager.TYPE_WIFI,
          ConnectivityManager.TYPE_MOBILE,
          ConnectivityManager.TYPE_ETHERNET -> true
          else -> false
        }
      }
    }
    return false
  }
}
