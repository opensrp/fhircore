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

package org.smartregister.fhircore.geowidget.screens

import android.os.Build
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.ona.kujaku.views.KujakuMapView
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest
import org.smartregister.fhircore.geowidget.shadows.ShadowConnectivityReceiver
import org.smartregister.fhircore.geowidget.shadows.ShadowKujakuMapView
import org.smartregister.fhircore.geowidget.shadows.ShadowMapbox

@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.Q],
  shadows = [ShadowConnectivityReceiver::class, ShadowKujakuMapView::class, ShadowMapbox::class],
  application = HiltTestApplication::class,
)
@HiltAndroidTest
class GeoWidgetFragmentTest {
  private lateinit var geowidgetFragment: GeoWidgetFragment
  private var kujakuMapView = mockk<KujakuMapView>(relaxed = true)

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private lateinit var kujakuMapViewLifecycle: String

  @Before
  fun setup() {
    hiltRule.inject()

    Robolectric.buildActivity(HiltActivityForTest::class.java).create().resume().get()

    geowidgetFragment = GeoWidgetFragment()

    every { kujakuMapView.onStart() } answers { kujakuMapViewLifecycle = "onStart" }
    every { kujakuMapView.onLowMemory() } answers { kujakuMapViewLifecycle = "onLowMemory" }
    every { kujakuMapView.onPause() } answers { kujakuMapViewLifecycle = "onPause" }
    every { kujakuMapView.onResume() } answers { kujakuMapViewLifecycle = "onResume" }
    every { kujakuMapView.onDestroy() } answers { kujakuMapViewLifecycle = "onDestroy" }
    every { kujakuMapView.onStop() } answers { kujakuMapViewLifecycle = "onStop" }
    every { kujakuMapView.onSaveInstanceState(any()) } answers
      {
        kujakuMapViewLifecycle = "onSaveInstanceState"
      }
  }

  @Test
  fun testAddAndCancelLocationPoints() {
    // Mock dependencies
    val mockFeatureCollection = mockk<FeatureCollection>(relaxed = true)
    val mockGeoJsonSource = mockk<GeoJsonSource>(relaxed = true)
    // Set up mocks
    every { mockGeoJsonSource.setGeoJson(mockFeatureCollection) } just Runs

    // Invoke method under test
    geowidgetFragment.setOnAddLocationListener(kujakuMapView)
    geowidgetFragment.setOnClickLocationListener(kujakuMapView)

    // Verify mocks
    verify { kujakuMapView.addPoint(any(), any()) }
  }
}
