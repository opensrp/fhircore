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

package org.smartregister.fhircore.geowidget.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
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
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.ona.kujaku.listeners.OnFeatureClickListener
import io.ona.kujaku.views.KujakuMapView
import org.hl7.fhir.r4.model.Location
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.geowidget.ext.Coordinate
import org.smartregister.fhircore.geowidget.ext.coordinates
import org.smartregister.fhircore.geowidget.ext.generateLocation
import org.smartregister.fhircore.geowidget.shadows.ShadowConnectivityReceiver
import org.smartregister.fhircore.geowidget.shadows.ShadowKujakuMapView

@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.O_MR1],
  shadows = [ShadowConnectivityReceiver::class, ShadowKujakuMapView::class],
  application = HiltTestApplication::class
)
@HiltAndroidTest
class GeoWidgetActivityTest {

  lateinit var geowidgetActivity: GeoWidgetActivity
  var kujakuMapView = mockk<KujakuMapView>()

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setup() {
    val controller = Robolectric.buildActivity(GeoWidgetTestActivity::class.java)
    geowidgetActivity = spyk(controller.create().resume().get())
    geowidgetActivity.kujakuMapView = kujakuMapView

    every { kujakuMapView.onLowMemory() } just runs
    every { kujakuMapView.onPause() } just runs
    every { kujakuMapView.onResume() } just runs
    every { kujakuMapView.onDestroy() } just runs
    every { kujakuMapView.onStop() } just runs
  }

  @Test
  fun renderResourcesOnMapShouldSetGeoJsonAndCallZoomToPointsOnMap() {
    val featureCollection = mockk<FeatureCollection>()
    val style = mockk<Style>()
    val source = mockk<GeoJsonSource>()
    every { style.getSourceAs<GeoJsonSource>("quest-data-set") } returns source
    geowidgetActivity.featureCollection = featureCollection
    every { geowidgetActivity.zoomToPointsOnMap(any()) } just runs
    every { source.setGeoJson(any<FeatureCollection>()) } just runs

    geowidgetActivity.renderResourcesOnMap(style)

    verify { source.setGeoJson(featureCollection) }
    verify { geowidgetActivity.zoomToPointsOnMap(featureCollection) }
  }

  @Test
  fun setLocationReferenceAsResultShouldCallFinishAndSetResultWithLocationId() {
    val intentCapturingSlot = slot<Intent>()

    val location = Location().apply { id = "i2342980kosdf9823" }

    geowidgetActivity.setLocationReferenceAsResult(location)

    verify { geowidgetActivity.setResult(Activity.RESULT_OK, capture(intentCapturingSlot)) }
    Assert.assertEquals(
      "i2342980kosdf9823",
      intentCapturingSlot.captured.getStringExtra(GeoWidgetActivity.LOCATION_ID)
    )
    verify { geowidgetActivity.finish() }
  }

  @Test
  fun setFeatureClickListenerShouldCallSetFamilyIdAsResultWhenFamilyIsClicked() {
    // val callBack
    val listenerSlot = slot<OnFeatureClickListener>()
    every { geowidgetActivity.setFamilyIdAsResult(any()) } just runs
    every {
      kujakuMapView.setOnFeatureClickListener(capture(listenerSlot), "quest-data-points")
    } just runs

    geowidgetActivity.setFeatureClickListener()

    val geometry = Point.fromLngLat(2.0, 7.0)
    val feature = Feature.fromGeometry(geometry)
    feature.addStringProperty("family-id", "john-doe-family-id")

    listenerSlot.captured.onFeatureClick(listOf(feature))

    verify { geowidgetActivity.setFamilyIdAsResult("john-doe-family-id") }
  }

  @Test
  fun setFamilyIdAsResultShouldCallFinishAndSetResultWithLocationId() {
    val intentCapturingSlot = slot<Intent>()

    geowidgetActivity.setFamilyIdAsResult("john-doe-family-id")

    verify { geowidgetActivity.setResult(Activity.RESULT_OK, capture(intentCapturingSlot)) }
    Assert.assertEquals(
      "john-doe-family-id",
      intentCapturingSlot.captured.getStringExtra(GeoWidgetActivity.FAMILY_ID)
    )
    verify { geowidgetActivity.finish() }
  }

  @Test
  fun coordinatesExtFnShouldReturnCoordinate() {
    val feature =
      JSONObject().apply {
        put(
          "geometry",
          JSONObject().apply { put("coordinates", JSONArray(arrayOf(3.43452342, 1.3489834))) }
        )
      }

    val coordinates = feature.coordinates()
    Assert.assertEquals(3.43452342, coordinates!!.first, 0.0)
    Assert.assertEquals(1.3489834, coordinates!!.second, 0.0)
  }

  @Test
  fun generateLocationShouldReturnLocationWithCoordiantes() {
    val feature =
      JSONObject().apply {
        put(
          "geometry",
          JSONObject().apply { put("coordinates", JSONArray(arrayOf(3.43452342, 1.3489834))) }
        )
      }

    val coordinates = Coordinate(3.43452342, 1.3489834)

    val location = generateLocation(feature, coordinates)

    Assert.assertEquals(3.43452342, location.position.longitude.toDouble(), 0.0)
    Assert.assertEquals(1.3489834, location.position.latitude.toDouble(), 0.0)
  }

  @Test
  fun onPause() {
    ReflectionHelpers.callInstanceMethod<Void>(geowidgetActivity, "onPause")
    verify { kujakuMapView.onPause() }
  }

  @Test
  fun onStop() {
    ReflectionHelpers.callInstanceMethod<Void>(geowidgetActivity, "onStop")
    verify { kujakuMapView.onStop() }
  }

  @Test
  fun onDestroy() {
    ReflectionHelpers.callInstanceMethod<Void>(geowidgetActivity, "onDestroy")
    verify { kujakuMapView.onDestroy() }
  }

  @Test
  fun onLowMemory() {
    geowidgetActivity.onLowMemory()
    verify { kujakuMapView.onLowMemory() }
  }
}
