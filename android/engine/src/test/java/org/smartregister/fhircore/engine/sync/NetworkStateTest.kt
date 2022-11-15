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

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

internal class NetworkStateTest : RobolectricTest() {

  private val context: Application = ApplicationProvider.getApplicationContext()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Suppress("DEPRECATION")
  @Test
  fun invoke() {
    val networkState = mockk<NetworkState>()
    every { networkState.invoke() } returns false

    networkState.invoke()

    verify { networkState.invoke() }

    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val networkCapabilities = ShadowNetworkCapabilities.newInstance()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      shadowOf(connectivityManager)
        .setNetworkCapabilities(connectivityManager.activeNetwork, networkCapabilities)
      shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
      shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
      assertTrue(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
      assertTrue(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    } else {
      assertTrue(connectivityManager.activeNetworkInfo?.type != ConnectivityManager.TYPE_WIFI)
      assertTrue(connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE)
    }
  }
}
