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
import org.smartregister.fhircore.geowidget.model.GeoJsonFeature
import org.smartregister.fhircore.geowidget.model.ServicePointType
import org.smartregister.fhircore.geowidget.screens.GeoWidgetFragment.Companion.MAP_FEATURES_LIMIT

class GeoWidgetViewModel : ViewModel() {

  val mapFeatures = ArrayDeque<Feature>()

  fun updateMapFeatures(geoJsonFeatures: List<GeoJsonFeature>) {
    if (mapFeatures.size <= MAP_FEATURES_LIMIT) {
      mapFeatures.addAll(geoJsonFeatures.map { it.toFeature() })
    }
  }

  fun clearMapFeatures() = mapFeatures.clear()

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
    map[ServicePointType.DISTRICT_PPSPF.name.lowercase()] = ServicePointType.DISTRICT_PPSPF
    map[ServicePointType.MAIRIE.name.lowercase()] = ServicePointType.MAIRIE
    map[ServicePointType.ECOLE_COMMUNAUTAIRE.name.lowercase()] =
      ServicePointType.ECOLE_COMMUNAUTAIRE
    map[ServicePointType.ECOLE_PRIVÉ.name.lowercase()] = ServicePointType.ECOLE_PRIVÉ
    map[ServicePointType.LYCÉE.name.lowercase()] = ServicePointType.LYCÉE
    return map
  }
}
