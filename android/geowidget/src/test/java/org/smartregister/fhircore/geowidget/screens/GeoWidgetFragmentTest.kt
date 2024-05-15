/*
 * Copyright 2021-2024 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.geowidget.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.LinearLayout
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavArgsLazy
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import io.ona.kujaku.plugin.switcher.BaseLayerSwitcherPlugin
import io.ona.kujaku.views.KujakuMapView
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.configuration.geowidget.MapLayerConfig
import org.smartregister.fhircore.geowidget.BuildConfig
import org.smartregister.fhircore.geowidget.shadows.ShadowConnectivityReceiver
import org.smartregister.fhircore.geowidget.shadows.ShadowKujakuMapView
import org.smartregister.fhircore.geowidget.shadows.ShadowMapbox
import javax.annotation.Resources

@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.O_MR1],
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

    Robolectric.buildActivity(GeoWidgetTestActivity::class.java).create().resume().get()

    geowidgetFragment = GeoWidgetFragment()
    geowidgetFragment.kujakuMapView = kujakuMapView

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
    fun test_add_and_cancel_location_points() {
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
