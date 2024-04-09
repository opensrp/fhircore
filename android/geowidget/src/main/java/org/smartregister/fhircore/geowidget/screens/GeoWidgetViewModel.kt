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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.geowidget.model.Feature
import org.smartregister.fhircore.geowidget.model.ServicePointType
import org.smartregister.fhircore.geowidget.util.extensions.getGeoJsonGeometry
import org.smartregister.fhircore.geowidget.util.extensions.getProperties
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GeoWidgetViewModel @Inject constructor(val dispatcherProvider: DispatcherProvider) :
  ViewModel() {

  private val _featuresFlow: MutableStateFlow<Set<com.mapbox.geojson.Feature>> =
    MutableStateFlow(setOf())
  val featuresFlow: StateFlow<Set<com.mapbox.geojson.Feature>> = _featuresFlow

  fun addLocationToMap(location: Feature) {
      try {
          val jsonFeature =
              JSONObject().apply {
                  put("type", "Feature")
                  put("properties", location.getProperties())
                  put("geometry", location.getGeoJsonGeometry())
              }
          val feature = com.mapbox.geojson.Feature.fromJson(jsonFeature.toString())
          _featuresFlow.value = _featuresFlow.value + feature
      } catch (e:Exception) {
          Timber.e(e)
      }
  }

  fun addLocationsToMap(locations: Set<Feature>) {
    locations.forEach { location ->
      addLocationToMap(location)
    }
  }

  fun clearLocations() {
    _featuresFlow.value = setOf()
  }

  fun getServicePointKeyToType(): Map<String, ServicePointType> {
      val map: MutableMap<String, ServicePointType> = HashMap()
      map[ServicePointType.EPP.name.lowercase()] = ServicePointType.EPP
      map[ServicePointType.CEG.name.lowercase()] = ServicePointType.CEG
      map[ServicePointType.CHRD1.name.lowercase()] = ServicePointType.CHRD1
      map[ServicePointType.CHRD2.name.lowercase()] = ServicePointType.CHRD2
      map[ServicePointType.DRSP.name.lowercase()] = ServicePointType.DRSP
      map[ServicePointType.MSP.name.lowercase()] = ServicePointType.MSP
      map[ServicePointType.SDSP.name.lowercase()] = ServicePointType.SDSP
      map[ServicePointType.CSB1.name.lowercase()] = ServicePointType.CSB1
      map[ServicePointType.CSB2.name.lowercase()] = ServicePointType.CSB2
      map[ServicePointType.CHRR.name.lowercase()] = ServicePointType.CHRR
      map[ServicePointType.WAREHOUSE.name.lowercase()] = ServicePointType.WAREHOUSE
      map[ServicePointType.WATER_POINT.name.lowercase()] = ServicePointType.WATER_POINT
      map[ServicePointType.PRESCO.name.lowercase()] = ServicePointType.PRESCO
      map[ServicePointType.MEAH.name.lowercase()] = ServicePointType.MEAH
      map[ServicePointType.DREAH.name.lowercase()] = ServicePointType.DREAH
      map[ServicePointType.MPPSPF.name.lowercase()] = ServicePointType.MPPSPF
      map[ServicePointType.DRPPSPF.name.lowercase()] = ServicePointType.DRPPSPF
      map[ServicePointType.NGO_PARTNER.name.lowercase()] = ServicePointType.NGO_PARTNER
      map[ServicePointType.SITE_COMMUNAUTAIRE.name.lowercase()] = ServicePointType.SITE_COMMUNAUTAIRE
      map[ServicePointType.DRJS.name.lowercase()] = ServicePointType.DRJS
      map[ServicePointType.INSTAT.name.lowercase()] = ServicePointType.INSTAT
      map[ServicePointType.BSD.name.lowercase()] = ServicePointType.BSD
      map[ServicePointType.MEN.name.lowercase()] = ServicePointType.MEN
      map[ServicePointType.DREN.name.lowercase()] = ServicePointType.DREN
      return map

  }
}
