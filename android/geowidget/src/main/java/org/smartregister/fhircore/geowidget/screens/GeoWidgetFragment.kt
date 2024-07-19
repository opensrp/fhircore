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

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.exceptions.MapboxConfigurationException
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfMeasurement
import dagger.hilt.android.AndroidEntryPoint
import io.ona.kujaku.callbacks.AddPointCallback
import io.ona.kujaku.plugin.switcher.BaseLayerSwitcherPlugin
import io.ona.kujaku.plugin.switcher.layer.StreetsBaseLayer
import io.ona.kujaku.utils.CoordinateUtils
import io.ona.kujaku.views.KujakuMapView
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import org.json.JSONObject
import org.smartregister.fhircore.engine.configuration.geowidget.MapLayer
import org.smartregister.fhircore.engine.configuration.geowidget.MapLayerConfig
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.geowidget.BuildConfig
import org.smartregister.fhircore.geowidget.R
import org.smartregister.fhircore.geowidget.baselayers.MapBoxSatelliteLayer
import org.smartregister.fhircore.geowidget.baselayers.StreetSatelliteLayer
import org.smartregister.fhircore.geowidget.model.GeoJsonFeature
import org.smartregister.fhircore.geowidget.model.ServicePointType
import org.smartregister.fhircore.geowidget.model.TYPE
import org.smartregister.fhircore.geowidget.util.ResourceUtils
import timber.log.Timber

@AndroidEntryPoint
class GeoWidgetFragment : Fragment() {

  private val geoWidgetViewModel by activityViewModels<GeoWidgetViewModel>()
  internal var onAddLocationCallback: (GeoJsonFeature) -> Unit = {}
  internal var onCancelAddingLocationCallback: () -> Unit = {}
  internal var onClickLocationCallback: (GeoJsonFeature, FragmentManager) -> Unit =
    { _: GeoJsonFeature, _: FragmentManager ->
    }
  internal var useGpsOnAddingLocation: Boolean = false
  internal var mapLayers: List<MapLayerConfig> = ArrayList()
  internal var showCurrentLocationButton: Boolean = true
  internal var showPlaneSwitcherButton: Boolean = true
  internal var showAddLocationButton: Boolean = true
  private lateinit var mapView: KujakuMapView
  private lateinit var featureCollection: FeatureCollection
  private var geoJsonSource: GeoJsonSource? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    Mapbox.getInstance(requireContext(), BuildConfig.MAPBOX_SDK_TOKEN)
    val view = setupViews()
    mapView.onCreate(savedInstanceState)
    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupMapFeatures()
  }

  override fun onStart() {
    super.onStart()
    mapView.onStart()
  }

  override fun onResume() {
    super.onResume()
    mapView.onResume()
  }

  override fun onPause() {
    super.onPause()
    mapView.onPause()
  }

  override fun onStop() {
    super.onStop()
    mapView.onStop()
  }

  override fun onDestroy() {
    super.onDestroy()
    mapView.onDestroy()
  }

  override fun onLowMemory() {
    super.onLowMemory()
    mapView.onLowMemory()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    mapView.onSaveInstanceState(outState)
  }

  private fun setupMapFeatures() {
    viewLifecycleOwner.lifecycleScope.launch {
      geoWidgetViewModel.features.observe(viewLifecycleOwner) { result ->
        zoomToLocationsOnMap(result.map { it.toFeature() })
      }
    }
  }

  private fun setupViews(): LinearLayout {
    mapView = setUpMapView()
    featureCollection =
      FeatureCollection.fromFeatures(
        geoWidgetViewModel.features.value?.map { it.toFeature() } ?: listOf(),
      )
    return LinearLayout(requireContext()).apply {
      orientation = LinearLayout.VERTICAL
      addView(mapView)
    }
  }

  private fun setUpMapView(): KujakuMapView {
    return try {
      KujakuMapView(requireActivity()).apply {
        id = R.id.kujaku_widget
        val builder = Style.Builder().fromUri(context.getString(R.string.style_map_fhir_core))
        getMapAsync { mapboxMap ->
          mapboxMap.setStyle(builder) { style ->
            geoJsonSource = style.getSourceAs(context.getString(R.string.data_set_quest))
            if (geoJsonSource != null) {
              geoJsonSource!!.setGeoJson(featureCollection)
            }
            addIconsLayer(style)
            addMapStyle(style)
          }
        }

        if (showAddLocationButton) {
          setOnAddLocationListener(this)
        }
        setOnClickLocationListener(this)
      }
    } catch (e: MapboxConfigurationException) {
      Timber.e(e)
      mapView
    }
  }

  private fun addIconsLayer(mMapboxMapStyle: Style) {
    val dynamicIconSize =
      Expression.interpolate(
        Expression.linear(),
        Expression.zoom(),
        Expression.literal(0.98f),
        Expression.literal(0.9f),
      )

    val servicePointTypeMap: Map<String, ServicePointType> =
      geoWidgetViewModel.getServicePointKeyToType()
    for ((key, servicePointType) in servicePointTypeMap) {
      val icon: Bitmap? =
        ResourceUtils.drawableToBitmap(
          ResourcesCompat.getDrawable(
            resources,
            servicePointType.drawableId,
            requireContext().theme,
          )!!,
        )
      icon?.let {
        mMapboxMapStyle.addImage(key, icon)
        val symbolLayer =
          SymbolLayer(
            String.format("%s.layer", key),
            getString(R.string.data_set_quest),
          )
        symbolLayer.setProperties(
          PropertyFactory.iconImage(key),
          PropertyFactory.iconSize(dynamicIconSize),
          PropertyFactory.iconIgnorePlacement(false),
          PropertyFactory.iconAllowOverlap(false),
        )
        symbolLayer.setFilter(
          Expression.eq(
            Expression.get(TYPE),
            servicePointType.name.lowercase(),
          ),
        )
        mMapboxMapStyle.addLayer(symbolLayer)
      }
    }
  }

  private fun KujakuMapView.addMapStyle(style: Style) {
    val baseLayerSwitcherPlugin = BaseLayerSwitcherPlugin(this, style)

    baseLayerSwitcherPlugin.apply {
      mapLayers.forEach {
        when (it.layer) {
          MapLayer.STREET -> addBaseLayer(MapBoxSatelliteLayer(), it.active)
          MapLayer.SATELLITE -> addBaseLayer(StreetsBaseLayer(requireContext()), it.active)
          MapLayer.STREET_SATELLITE ->
            addBaseLayer(StreetSatelliteLayer(requireContext()), it.active)
        }
      }
    }

    showCurrentLocationBtn(showCurrentLocationButton)
    if (showPlaneSwitcherButton) {
      baseLayerSwitcherPlugin.show()
    }
  }

  @VisibleForTesting
  fun setOnClickLocationListener(mapView: KujakuMapView) {
    mapView.setOnFeatureClickListener(
      { featuresList ->
        val mapBoxFeature = featuresList.firstOrNull() ?: return@setOnFeatureClickListener
        // TODO: Support other Geometry types as well other than Point
        if (mapBoxFeature.geometry() !is Point) {
          Timber.w("Only feature geometry of type Point is supported!")
          return@setOnFeatureClickListener
        }
        onClickLocationCallback(mapBoxFeature.toJson().decodeJson(), parentFragmentManager)
      },
      "quest-data-points",
    )
  }

  @VisibleForTesting
  fun setOnAddLocationListener(mapView: KujakuMapView) {
    mapView.addPoint(
      useGpsOnAddingLocation,
      object : AddPointCallback {

        override fun onPointAdd(featureJSONObject: JSONObject?) {
          featureJSONObject ?: return
          onAddLocationCallback(featureJSONObject.toString().decodeJson<GeoJsonFeature>())
        }

        override fun onCancel() {
          onCancelAddingLocationCallback
        }
      },
    )
  }

  private fun zoomToLocationsOnMap(features: List<Feature>) {
    if (features.isEmpty()) return
    featureCollection = FeatureCollection.fromFeatures(features)

    val locationPoints =
      featureCollection
        .features()
        ?.asSequence()
        ?.filter { it.geometry() is Point }
        ?.map { it.geometry() as Point }
        ?.toMutableList() ?: emptyList()

    val bbox = TurfMeasurement.bbox(MultiPoint.fromLngLats(locationPoints))
    val paddedBbox = CoordinateUtils.getPaddedBbox(bbox, 1000.0)
    val bounds = LatLngBounds.from(paddedBbox[3], paddedBbox[2], paddedBbox[1], paddedBbox[0])
    val finalCameraPosition = CameraUpdateFactory.newLatLngBounds(bounds, 50)

    geoJsonSource?.setGeoJson(featureCollection)
    mapView.getMapAsync { mapboxMap -> mapboxMap.easeCamera(finalCameraPosition) }
  }

  class Builder {

    private var onAddLocationCallback: (GeoJsonFeature) -> Unit = {}
    private var onCancelAddingLocationCallback: () -> Unit = {}
    private var onClickLocationCallback: (GeoJsonFeature, FragmentManager) -> Unit =
      { _: GeoJsonFeature, _: FragmentManager ->
      }
    private var useGpsOnAddingLocation: Boolean = false
    private var mapLayers: List<MapLayerConfig> = ArrayList()
    private var showCurrentLocationButton: Boolean = true
    private var showPlaneSwitcherButton: Boolean = true
    private var showAddLocationButton: Boolean = true

    fun setOnAddLocationListener(onAddLocationCallback: (GeoJsonFeature) -> Unit) = apply {
      this.onAddLocationCallback = onAddLocationCallback
    }

    fun setOnCancelAddingLocationListener(onCancelAddingLocationCallback: () -> Unit) = apply {
      this.onCancelAddingLocationCallback = onCancelAddingLocationCallback
    }

    fun setOnClickLocationListener(
      onClickLocationCallback: (GeoJsonFeature, FragmentManager) -> Unit,
    ) = apply { this.onClickLocationCallback = onClickLocationCallback }

    fun setUseGpsOnAddingLocation(value: Boolean) = apply { this.useGpsOnAddingLocation = value }

    fun setMapLayers(list: List<MapLayerConfig>) = apply { this.mapLayers = list }

    fun showCurrentLocationButtonVisibility(show: Boolean) = apply {
      this.showCurrentLocationButton = show
    }

    fun setAddLocationButtonVisibility(show: Boolean) = apply { this.showAddLocationButton = show }

    fun setPlaneSwitcherButtonVisibility(show: Boolean) = apply {
      this.showPlaneSwitcherButton = show
    }

    fun build(): GeoWidgetFragment {
      return GeoWidgetFragment().apply {
        this.onAddLocationCallback = this@Builder.onAddLocationCallback
        this.onCancelAddingLocationCallback = this@Builder.onCancelAddingLocationCallback
        this.onClickLocationCallback = this@Builder.onClickLocationCallback
        this.useGpsOnAddingLocation = this@Builder.useGpsOnAddingLocation
        this.mapLayers = this@Builder.mapLayers
        this.showCurrentLocationButton = this@Builder.showCurrentLocationButton
        this.showPlaneSwitcherButton = this@Builder.showPlaneSwitcherButton
        this.showAddLocationButton = this@Builder.showAddLocationButton
      }
    }
  }

  companion object {
    fun builder() = Builder()
  }
}
