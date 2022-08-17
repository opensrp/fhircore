package org.smartregister.fhircore.geowidget.screens

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import io.ona.kujaku.callbacks.AddPointCallback
import io.ona.kujaku.views.KujakuMapView
import java.math.BigDecimal
import java.util.UUID
import org.hl7.fhir.r4.model.Location
import org.json.JSONObject
import org.smartregister.fhircore.geowidget.BuildConfig
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

    Mapbox.getInstance(
      this,
      BuildConfig.MAPBOX_SDK_TOKEN
    )

    setContentView(R.layout.activity_geowidget)

    geowidgetViewModel.context = this

    kujakuMapView = findViewById(R.id.mapView)
    kujakuMapView.getMapAsync { mapboxMap ->
      Timber.e("Get Map async finished")
      val builder = Style.Builder().fromUri("asset://fhircore_style.json")

      mapboxMap.setStyle(
        builder,
        { style ->
          Timber.e("Finished setting the style")
          geoJsonSource = style.getSourceAs<GeoJsonSource>("reveal-data-set")

          geoJsonSource?.also { source ->
            featureCollection?.also { collection ->
              Timber.e("Setting the feature collection")
              source.setGeoJson(collection)
            }
          }
        }
      )
    }

    kujakuMapView.setOnFeatureClickListener(
      { featuresList ->
        val names =
          featuresList.filter { it.hasProperty("name") }.map { it.getStringProperty("name") }
        Toast.makeText(
            this@GeowidgetActivity,
            "Family clicked: ${TextUtils.join(",", names)}",
            Toast.LENGTH_LONG
          )
          .show()
      },
      "reveal-data-points"
    )

    // Add the onclick listener
    findViewById<Button>(R.id.register_family).setOnClickListener {
        // TODO: Possibly disable the register_family button
      if (registerFamilyMode) {
          registerFamilyMode = false;

          // TODO: FIND A WAY TO DISABLE THIS MODE
      } else {

        registerFamilyMode = true
        kujakuMapView.addPoint(
          true,
          object : AddPointCallback {

            override fun onPointAdd(featureJSONObject: JSONObject?) {
              // Open the family registration with the coordinates

              featureJSONObject ?: return

              val coordinates = featureJSONObject.coordinates() ?: return

              val location =
                Location().apply {
                  id = UUID.randomUUID().toString()
                  status = Location.LocationStatus.INACTIVE
                  position =
                    Location.LocationPositionComponent().apply {
                      longitude = BigDecimal(coordinates.longitude)
                      latitude = BigDecimal(coordinates.latitude)
                    }
                }

              // Save it in the viewModel
              geowidgetViewModel.saveLocation(location).observe(this@GeowidgetActivity) {
                if (it) {
                  val intentData = Intent().apply { putExtra(LOCATION_ID, location.idElement.value) }

                  setResult(RESULT_OK, intentData)
                  this@GeowidgetActivity.finish()
                }
              }
            }

            override fun onCancel() {}
          }
        )
      }
    }

    findViewById<Button>(R.id.register_family)

    // Display the groups
    geowidgetViewModel.getFamiliesFeatureCollectionStream().observe(this, this)
  }

  override fun onChanged(featureCollection: FeatureCollection?) {
    Timber.e("Feature collection loaded")
    this.featureCollection = featureCollection

    geoJsonSource?.also { source ->
      featureCollection?.also { collection ->
        Timber.e("Feature loaded : ${featureCollection.toJson()}")

        source.setGeoJson(collection)
        this.featureCollection = null
      }
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
  }
}
