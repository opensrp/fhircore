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

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfMeasurement
import io.ona.kujaku.callbacks.AddPointCallback
import io.ona.kujaku.utils.CoordinateUtils
import io.ona.kujaku.views.KujakuMapView
import java.math.BigDecimal
import java.util.LinkedList
import java.util.UUID
import org.apache.commons.codec.binary.Base64
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Location
import org.json.JSONObject
import org.smartregister.fhircore.geowidget.BuildConfig
import org.smartregister.fhircore.geowidget.KujakuFhirCoreConverter
import org.smartregister.fhircore.geowidget.R
import org.smartregister.fhircore.geowidget.ext.Coordinate
import org.smartregister.fhircore.geowidget.ext.latitude
import org.smartregister.fhircore.geowidget.ext.longitude
import org.smartregister.fhircore.geowidget.model.GeowidgetViewModel
import timber.log.Timber

class GeowidgetActivity : AppCompatActivity(), Observer<FeatureCollection> {

  lateinit var kujakuMapView: KujakuMapView
  val geowidgetViewModel: GeowidgetViewModel by viewModels()
  var geoJsonSource: GeoJsonSource? = null

  var featureCollection: FeatureCollection? = null
  var registerFamilyMode = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Mapbox.getInstance(this, BuildConfig.MAPBOX_SDK_TOKEN)
    setContentView(R.layout.activity_geowidget)
    geowidgetViewModel.context = this

    kujakuMapView = findViewById(R.id.mapView)
    kujakuMapView.getMapAsync { mapboxMap ->
      Timber.i("Get Map async finished")
      val builder = Style.Builder().fromUri("asset://fhircore_style.json")

      mapboxMap.setStyle(builder) { style ->
        Timber.i("Finished setting the style")
        renderResourcesOnMap(style)
      }
    }

    setFeatureClickListener()
    enableFamilyRegistration()

    // Display the groups
    geowidgetViewModel.getFamiliesFeatureCollectionStream().observe(this, this)
  }

  private fun renderResourcesOnMap(style: Style) {
    geoJsonSource = style.getSourceAs<GeoJsonSource>("quest-data-set")

    geoJsonSource?.also { source ->
      featureCollection?.also { collection ->
        Timber.i("Setting the feature collection")
        source.setGeoJson(collection)

        zoomToPointsOnMap(featureCollection)
      }
    }
  }

  private fun enableFamilyRegistration() {
    kujakuMapView.addPoint(
      true,
      object : AddPointCallback {

        override fun onPointAdd(featureJSONObject: JSONObject?) {
          // Open the family registration with the coordinates
          featureJSONObject ?: return
          val coordinates = featureJSONObject.coordinates() ?: return

          Toast.makeText(this@GeowidgetActivity, getString(R.string.please_wait), Toast.LENGTH_LONG)
            .show()

          val location = generateLocation(featureJSONObject, coordinates)

          // Save it in the viewModel
          geowidgetViewModel.saveLocation(location).observe(this@GeowidgetActivity) {
            if (it) {
              Toast.makeText(
                  this@GeowidgetActivity,
                  getString(R.string.openning_family_registration_form),
                  Toast.LENGTH_LONG
                )
                .show()
              setLocationReferenceAsResult(location)
            }
          }
        }

        override fun onCancel() {}
      }
    )
  }

  private fun setLocationReferenceAsResult(location: Location) {
    val intentData = Intent().apply { putExtra(LOCATION_ID, location.idElement.value) }

    setResult(RESULT_OK, intentData)
    this@GeowidgetActivity.finish()
  }

  private fun generateLocation(featureJSONObject: JSONObject, coordinates: Coordinate): Location {
    return Location().apply {
      id = UUID.randomUUID().toString()
      status = Location.LocationStatus.INACTIVE
      position =
        Location.LocationPositionComponent().apply {
          longitude = BigDecimal(coordinates.longitude)
          latitude = BigDecimal(coordinates.latitude)
        }

      extension =
        listOf(
          Extension(KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL).apply {
            setValue(
              Attachment().apply {
                contentType = "application/geo+json"
                data = Base64.encodeBase64(featureJSONObject.toString().encodeToByteArray())
              }
            )
          }
        )
    }
  }

  private fun setFeatureClickListener() {
    kujakuMapView.setOnFeatureClickListener(
      { featuresList ->
        featuresList.firstOrNull { it.hasProperty("family-id") }?.let {
          it.getStringProperty("family-id")?.also { setFamilyIdAsResult(it) }
        }
      },
      "quest-data-points"
    )
  }

  private fun setFamilyIdAsResult(familyId: String) {
    val intentData = Intent().apply { putExtra(FAMILY_ID, familyId) }

    setResult(RESULT_OK, intentData)
    this@GeowidgetActivity.finish()
  }

  override fun onChanged(featureCollection: FeatureCollection?) {
    Timber.e("Feature collection loaded")
    this.featureCollection = featureCollection

    geoJsonSource?.also { source ->
      featureCollection?.also { collection ->
        source.setGeoJson(collection)
        zoomToPointsOnMap(featureCollection)
      }
    }
  }

  fun zoomToPointsOnMap(featureCollection: FeatureCollection?) {
    featureCollection ?: return

    val points = LinkedList<Point>()

    featureCollection.features()?.forEach { feature ->
      val geometry = feature.geometry()

      if (geometry is Point) {
        points.add(geometry)
      }
    }

    val bbox = TurfMeasurement.bbox(MultiPoint.fromLngLats(points))

    // Generate the padded bbox
    val paddedBbox = CoordinateUtils.getPaddedBbox(bbox, 1000.0)

    kujakuMapView.getMapAsync { mapboxMap ->
      mapboxMap.easeCamera(
        CameraUpdateFactory.newLatLngBounds(
          LatLngBounds.from(paddedBbox[3], paddedBbox[2], paddedBbox[1], paddedBbox[0]),
          50
        )
      )
    }
  }

  fun JSONObject.coordinates(): Coordinate? {
    return optJSONObject("geometry")?.run {
      optJSONArray("coordinates")?.run { Coordinate(optDouble(0), optDouble(1)) }
    }
  }

  override fun onStart() {
    super.onStart()
    if (this::kujakuMapView.isInitialized) kujakuMapView.onStart()
  }

  override fun onResume() {
    super.onResume()
    if (this::kujakuMapView.isInitialized) kujakuMapView.onResume()
  }

  override fun onPause() {
    super.onPause()
    if (this::kujakuMapView.isInitialized) kujakuMapView.onPause()
  }

  override fun onStop() {
    super.onStop()
    if (this::kujakuMapView.isInitialized) kujakuMapView.onStop()
  }

  override fun onDestroy() {
    super.onDestroy()
    if (this::kujakuMapView.isInitialized) kujakuMapView.onDestroy()
  }

  override fun onLowMemory() {
    super.onLowMemory()
    if (this::kujakuMapView.isInitialized) kujakuMapView.onLowMemory()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (this::kujakuMapView.isInitialized) kujakuMapView.onSaveInstanceState(outState)
  }

  companion object {
    const val LOCATION_ID = "LOCATION-ID"
    const val FAMILY_ID = "FAMILY-ID"
    const val FAMILY_REGISTRATION_QUESTIONNAIRE = "82952"
  }
}
