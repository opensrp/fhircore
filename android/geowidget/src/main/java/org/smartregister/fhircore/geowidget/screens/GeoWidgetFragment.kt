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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfMeasurement
import dagger.hilt.android.AndroidEntryPoint
import io.ona.kujaku.callbacks.AddPointCallback
import io.ona.kujaku.utils.CoordinateUtils
import io.ona.kujaku.views.KujakuMapView
import java.util.LinkedList
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.smartregister.fhircore.geowidget.BuildConfig
import org.smartregister.fhircore.geowidget.R
import org.smartregister.fhircore.geowidget.model.Context
import org.smartregister.fhircore.geowidget.model.GeoWidgetLocation
import org.smartregister.fhircore.geowidget.model.Position
import org.smartregister.fhircore.geowidget.util.extensions.position
import timber.log.Timber

@AndroidEntryPoint
class GeoWidgetFragment : Fragment() {
  private val geoWidgetViewModel by viewModels<GeoWidgetViewModel>()
  internal var onAddLocationCallback: (GeoWidgetLocation) -> Unit = {}
  internal var onCancelAddingLocationCallback: () -> Unit = {}
  internal var onClickLocationCallback: (GeoWidgetLocation) -> Unit = {}
  internal var useGpsOnAddingLocation: Boolean = false

  private lateinit var mapView: KujakuMapView
  private var geoJsonSource: GeoJsonSource? = null
  private var featureCollection: FeatureCollection? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    Mapbox.getInstance(requireContext(), BuildConfig.MAPBOX_SDK_TOKEN)
    return setupViews()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setLocationCollector()
  }

  private fun setLocationCollector() {
    viewLifecycleOwner.lifecycleScope.launch {
      geoWidgetViewModel.featuresFlow
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .collect { features ->
          val featureCollection = FeatureCollection.fromFeatures(features.toList())
          this@GeoWidgetFragment.featureCollection = featureCollection
          if (geoJsonSource != null && featureCollection != null) {
            geoJsonSource!!.setGeoJson(featureCollection)
            zoomToLocationsOnMap(featureCollection)
          }
        }
    }
  }

  private fun setupViews(): LinearLayout {
    val toolbar = setUpToolbar()
    mapView = setUpMapView()

    return LinearLayout(requireContext()).apply {
      orientation = LinearLayout.VERTICAL
      addView(toolbar)
      addView(mapView)
    }
  }

  // TODO: move toolbar to quest
  private fun setUpToolbar(): Toolbar {
    return Toolbar(requireContext()).apply {
      popupTheme = R.style.AppTheme
      visibility = View.VISIBLE
      navigationIcon =
        ContextCompat.getDrawable(context, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
      layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 168)
      setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
      setNavigationOnClickListener {
        activity?.onBackPressedDispatcher?.onBackPressed()
      }
    }
  }

  private fun setUpMapView(): KujakuMapView {
    return KujakuMapView(requireActivity()).apply {
      id = R.id.kujaku_widget
      val builder = Style.Builder().fromUri("asset://fhircore_style.json")
      getMapAsync { mapboxMap ->
        mapboxMap.setStyle(builder) { style ->
          geoJsonSource = style.getSourceAs("quest-data-set")
          if (geoJsonSource != null && featureCollection != null) {
            geoJsonSource!!.setGeoJson(featureCollection)
          }
        }
      }
      setOnAddLocationListener(this)
      setOnClickLocationListener(this)
    }
  }

  private fun setOnClickLocationListener(mapView: KujakuMapView) {
    mapView.setOnFeatureClickListener(
      { featuresList ->
        val feature = featuresList.firstOrNull() ?: return@setOnFeatureClickListener
        if (feature.geometry() !is Point) {
          Timber.w("Only feature geometry of type Point is supported!")
          return@setOnFeatureClickListener
        }

        val point = (feature.geometry() as Point)
        val geoWidgetLocation = GeoWidgetLocation(
          id = feature.getStringProperty("id") ?: "",
          name = feature.getStringProperty("name") ?: "",
          position = Position(
            latitude = point.latitude(),
            longitude = point.longitude()
          ),
          contexts = listOf(Context(
            id = feature.getStringProperty("id") ?: "",
            type = feature.getStringProperty("type") ?: "",
          ))
        )

        onClickLocationCallback(geoWidgetLocation)
      },
      "quest-data-points",
    )
  }

  private fun setOnAddLocationListener(mapView: KujakuMapView) {
    mapView.addPoint(
      useGpsOnAddingLocation,
      object : AddPointCallback {

        override fun onPointAdd(featureJSONObject: JSONObject?) {
          // Open the family registration with the coordinates
          featureJSONObject ?: return
          val position = featureJSONObject.position() ?: return
          val geoWidgetLocation = GeoWidgetLocation(position = position)
          onAddLocationCallback(geoWidgetLocation)
        }

        override fun onCancel() {
          onCancelAddingLocationCallback
        }
      },
    )
  }

  override fun onChanged(value: FeatureCollection) {
    Timber.e("Feature collection loaded")
    this.featureCollection = value

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

    if ((featureCollection.features()?.size ?: 0) == 0) {
      return
    }

    val bbox = TurfMeasurement.bbox(MultiPoint.fromLngLats(points))
    val paddedBbox = CoordinateUtils.getPaddedBbox(bbox, 1000.0)

    kujakuMapView.getMapAsync { mapboxMap ->
      mapboxMap.easeCamera(
        CameraUpdateFactory.newLatLngBounds(
          LatLngBounds.from(paddedBbox[3], paddedBbox[2], paddedBbox[1], paddedBbox[0]),
          50,
        ),
      )
    }
  }

  override fun onStart() {
    super.onStart()
    kujakuMapView.onStart()
  }

  override fun onResume() {
    super.onResume()
    kujakuMapView.onResume()
    // Display the groups
    geoWidgetViewModel
      .getFamiliesFeatureCollectionStream(requireContext())
      .observe(viewLifecycleOwner, this)
  }

  override fun onPause() {
    super.onPause()
    kujakuMapView.onPause()
  }

  override fun onStop() {
    super.onStop()
    kujakuMapView.onStop()
  }

  override fun onDestroy() {
    super.onDestroy()
    kujakuMapView.onDestroy()
  }

  override fun onLowMemory() {
    super.onLowMemory()
    kujakuMapView.onLowMemory()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    kujakuMapView.onSaveInstanceState(outState)
  }
}
