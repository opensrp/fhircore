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

import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Feature
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.geowidget.model.GeoWidgetLocation
import org.smartregister.fhircore.geowidget.util.extensions.getGeoJsonGeometry

@HiltViewModel
class GeoWidgetViewModel @Inject constructor(val dispatcherProvider: DispatcherProvider) :
  ViewModel() {

  private val _featuresFlow: MutableStateFlow<Set<Feature>> =
    MutableStateFlow(setOf())
  val featuresFlow: StateFlow<Set<Feature>> = _featuresFlow

  fun addLocationToMap(location: GeoWidgetLocation) {
    val contexts = location.contexts.map { context ->
      JSONObject().apply {
        put("id", context.id)
        put("type", context.type)
      }
    }
    val properties = JSONObject().apply {
      put("id", location.id)
      put("name", location.name)
      put("contexts", JSONArray(contexts))
    }

    val jsonFeature =
      JSONObject().apply {
        put("type", "Feature")
        put("properties", properties)
        put("geometry", location.getGeoJsonGeometry())
      }
    val feature = Feature.fromJson(jsonFeature.toString())

    _featuresFlow.value = _featuresFlow.value + feature
  }

  fun addLocationsToMap(locations: Set<GeoWidgetLocation>) {
    locations.forEach { location ->
      addLocationToMap(location)
    }
  }

  fun clearLocations() {
    _featuresFlow.value = setOf()
  }
}
