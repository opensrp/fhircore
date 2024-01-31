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
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import javax.inject.Inject
import org.hl7.fhir.r4.model.Location
import org.json.JSONObject
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.geowidget.BuildConfig
import org.smartregister.fhircore.geowidget.R
import org.smartregister.fhircore.geowidget.model.GeoWidgetEvent
import org.smartregister.fhircore.geowidget.util.extensions.coordinates
import org.smartregister.fhircore.geowidget.util.extensions.generateLocation
import timber.log.Timber

@AndroidEntryPoint
open class GeoWidgetFragment : Fragment(), Observer<FeatureCollection> {
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var geoWidgetConfiguration: GeoWidgetConfiguration
  val geoWidgetActivityArgs by navArgs<GeoWidgetFragmentArgs>()
  val geoWidgetViewModel by activityViewModels<GeoWidgetViewModel>()
  lateinit var kujakuMapView: KujakuMapView
  var geoJsonSource: GeoJsonSource? = null
  var featureCollection: FeatureCollection? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    Mapbox.getInstance(requireContext(), BuildConfig.MAPBOX_SDK_TOKEN)
    geoWidgetConfiguration = geoWidgetConfiguration()

    return setupViews()
  }

  private fun geoWidgetConfiguration(): GeoWidgetConfiguration =
    configurationRegistry.retrieveConfiguration(
      ConfigType.GeoWidget,
      geoWidgetActivityArgs.configId,
    )

  /** Create the fragment views. Add the toolbar and KujakuMapView to a LinearLayout */
  private fun setupViews(): LinearLayout {
    val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 168)

    val toolbar =
      Toolbar(requireContext()).apply {
        popupTheme = org.smartregister.fhircore.engine.R.style.AppTheme
        visibility = View.VISIBLE
        navigationIcon =
          ContextCompat.getDrawable(context, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        setLayoutParams(layoutParams)
        setBackgroundColor(
          ContextCompat.getColor(
            requireContext(),
            org.smartregister.fhircore.engine.R.color.colorPrimary,
          ),
        )
        setNavigationOnClickListener { findNavController().popBackStack() }
      }
    kujakuMapView =
      KujakuMapView(requireActivity()).apply {
        id = R.id.kujaku_widget
        getMapAsync { mapboxMap ->
          Timber.i("Get Map async finished")
          val builder = Style.Builder().fromUri("asset://fhircore_style.json")

          mapboxMap.setStyle(builder) { style ->
            Timber.i("Finished setting the style")
            renderResourcesOnMap(style)
          }
        }
      }
    return LinearLayout(requireContext()).apply {
      orientation = LinearLayout.VERTICAL
      addView(toolbar)
      addView(kujakuMapView)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setFeatureClickListener()
    enableFamilyRegistration()
  }

  fun renderResourcesOnMap(style: Style) {
    geoJsonSource = style.getSourceAs("quest-data-set")

    geoJsonSource?.also { source ->
      featureCollection?.also { collection ->
        Timber.i("Setting the feature collection")
        source.setGeoJson(collection)

        zoomToPointsOnMap(featureCollection)
      }
    }
  }

  fun enableFamilyRegistration() {
    kujakuMapView.addPoint(
      true,
      object : AddPointCallback {

        override fun onPointAdd(featureJSONObject: JSONObject?) {
          // Open the family registration with the coordinates
          featureJSONObject ?: return
          val coordinates = featureJSONObject.coordinates() ?: return

          val geoWidgetActivity = requireContext()
          Toast.makeText(
              geoWidgetActivity,
              getString(org.smartregister.fhircore.engine.R.string.please_wait),
              Toast.LENGTH_LONG,
            )
            .show()

          val location: Location = generateLocation(featureJSONObject, coordinates)

          geoWidgetViewModel.saveLocation(location).observe(viewLifecycleOwner) { saveLocation ->
            if (saveLocation) {
              geoWidgetViewModel.geoWidgetEventLiveData.postValue(
                GeoWidgetEvent.RegisterClient(
                  location.idElement.value,
                  geoWidgetConfiguration.registrationQuestionnaire,
                ),
              )
            }
          }
        }

        override fun onCancel() {}
      },
    )
  }

  fun setFeatureClickListener() {
    kujakuMapView.setOnFeatureClickListener(
      { featuresList ->
        featuresList
          .firstOrNull { it.hasProperty("family-id") }
          ?.let {
            it.getStringProperty("family-id")?.also { familyId ->
              geoWidgetViewModel.geoWidgetEventLiveData.postValue(
                GeoWidgetEvent.OpenProfile(familyId, geoWidgetConfiguration),
              )
            }
          }
      },
      "quest-data-points",
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
