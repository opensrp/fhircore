/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.ona.kujaku.views.KujakuMapView
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.geowidget.model.GeoWidgetEvent
import org.smartregister.fhircore.geowidget.shadows.ShadowConnectivityReceiver
import org.smartregister.fhircore.geowidget.shadows.ShadowKujakuMapView

@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.O_MR1],
  shadows = [ShadowConnectivityReceiver::class, ShadowKujakuMapView::class],
  application = HiltTestApplication::class
)
@HiltAndroidTest
class GeoWidgetFragmentTest {
  lateinit var geowidgetFragment: GeoWidgetFragment
  var kujakuMapView = mockk<KujakuMapView>()
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()
  lateinit var kujakuMapViewLifecycle: String

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
  }

  @Test
  fun onStartSetsKujakuOnStart() {
    geowidgetFragment.onStart()
    Assert.assertEquals("onStart", kujakuMapViewLifecycle)
  }

  @Test
  fun onPauseSetsKujakuOnPause() {
    geowidgetFragment.onPause()
    Assert.assertEquals("onPause", kujakuMapViewLifecycle)
  }

  @Test
  fun onDestroySetsKujakuOnDestroy() {
    geowidgetFragment.onDestroy()
    Assert.assertEquals("onDestroy", kujakuMapViewLifecycle)
  }

  @Test
  fun onStopSetsKujakuOnStop() {
    geowidgetFragment.onStop()
    Assert.assertEquals("onStop", kujakuMapViewLifecycle)
  }

  @Test
  fun onLowMemorySetsKujakuOnLowMemory() {
    geowidgetFragment.onLowMemory()
    Assert.assertEquals("onLowMemory", kujakuMapViewLifecycle)
  }

  @Test
  fun renderResourcesOnMapShouldSetGeoJsonAndCallZoomToPointsOnMap() {
    val featureCollection = FeatureCollection.fromFeatures(emptyList())
    val style = mockk<Style>()
    val source = mockk<GeoJsonSource>()
    every { style.getSourceAs<GeoJsonSource>("quest-data-set") } returns source
    geowidgetFragment.featureCollection = featureCollection
    every { source.setGeoJson(any<FeatureCollection>()) } just runs

    geowidgetFragment.renderResourcesOnMap(style)

    verify { source.setGeoJson(featureCollection) }
  }

  @Test
  fun testOnChanged() {
    val featureCollection = mockk<FeatureCollection>()
    val source = mockk<GeoJsonSource>()
    every { source.setGeoJson(any<FeatureCollection>()) } just runs
    geowidgetFragment.onChanged(featureCollection)
    source.setGeoJson(featureCollection)
    verify { source.setGeoJson(featureCollection) }
  }

  @Test
  fun testSetFeatureClickListener() {
    val geoWidgetConfiguration = mockk<GeoWidgetConfiguration>()
    val geoWidgetViewModel = mockk<GeoWidgetViewModel>()
    val familyId = "123456"

    shadowOf(Looper.getMainLooper()).idle()
    val geoWidgetFragment = mockk<GeoWidgetFragment>(relaxed = true)
    every { geoWidgetFragment.setFeatureClickListener() } just runs
    every {
      geoWidgetViewModel.geoWidgetEventLiveData.postValue(
        GeoWidgetEvent.OpenProfile(familyId, geoWidgetConfiguration)
      )
    } just runs
    geoWidgetViewModel.geoWidgetEventLiveData.postValue(
      GeoWidgetEvent.OpenProfile(familyId, geoWidgetConfiguration)
    )

    GeoWidgetEvent.OpenProfile(familyId, mockk())
    geoWidgetViewModel.geoWidgetEventLiveData

    verify {
      geoWidgetViewModel.geoWidgetEventLiveData.postValue(
        GeoWidgetEvent.OpenProfile(familyId, geoWidgetConfiguration)
      )
    }
  }

  @Test
  fun `zoomToPointsOnMap with empty feature collection returns`() {
    val featureCollection = FeatureCollection.fromFeatures(arrayOf())

    geowidgetFragment.zoomToPointsOnMap(featureCollection)

    verify(inverse = true) { kujakuMapView.getMapAsync(any()) }
  }

  @Test
  fun `zoomToPointsOnMap zooms to feature`() {
    val featureCollection =
      FeatureCollection.fromFeatures(arrayOf(Feature.fromGeometry(Point.fromLngLat(0.0, 0.0))))

    every { kujakuMapView.getMapAsync(any()) } just runs

    geowidgetFragment.zoomToPointsOnMap(featureCollection)

    verify { kujakuMapView.getMapAsync(any()) }
  }
}
