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

  private val servicePointTypes =
    listOf(
      ServicePointType.EPP,
      ServicePointType.CEG,
      ServicePointType.CHRD1,
      ServicePointType.CHRD2,
      ServicePointType.DRSP,
      ServicePointType.MSP,
      ServicePointType.SDSP,
      ServicePointType.CSB1,
      ServicePointType.CSB2,
      ServicePointType.CHRR,
      ServicePointType.WAREHOUSE,
      ServicePointType.WATER_POINT,
      ServicePointType.PRESCO,
      ServicePointType.MEAH,
      ServicePointType.DREAH,
      ServicePointType.MPPSPF,
      ServicePointType.DRPPSPF,
      ServicePointType.NGO_PARTNER,
      ServicePointType.SITE_COMMUNAUTAIRE,
      ServicePointType.DRJS,
      ServicePointType.INSTAT,
      ServicePointType.BSD,
      ServicePointType.MEN,
      ServicePointType.DREN,
      ServicePointType.DISTRICT_PPSPF,
      ServicePointType.MAIRIE,
      ServicePointType.ECOLE_COMMUNAUTAIRE,
      ServicePointType.ECOLE_PRIVÉ,
      ServicePointType.ECOLE_PUBLIQUE,
      ServicePointType.CENTRE_DE_SANTE,
      ServicePointType.CENTRE_DE_TRAITEMENT_DU_CHOLERA,
      ServicePointType.HOPITAL_COMMUNAL,
      ServicePointType.HOPITAL,
      ServicePointType.BUREAU_DES_PARTENAIRES,
      ServicePointType.LYCÉE,
      ServicePointType.DIRECTION_COMMUNALE_DE_L_ENSEIGNEMENT,
      ServicePointType.ECOLE_PRIVE,
      ServicePointType.HOPITAL_COMMUNAUTAIRE,
      ServicePointType.HOPITAL_DE_DISTRICT,
      ServicePointType.SITE_DES_REGUFIES,
      ServicePointType.FARN_FAN,
      ServicePointType.SITES_DES_DEPLACES,
      ServicePointType.CHOLERA_HOT_SPOT,
      ServicePointType.BUREAU_DE_DISTRICT_SANITAIRE,
      ServicePointType.SALLE_DE_CLASSE,
      ServicePointType.SERVICE_COMMUNAUTAIRE,
      ServicePointType.ENTREPOT,
      ServicePointType.HOME,
      ServicePointType.VISITED,
      ServicePointType.REJECTED,
    )

  fun updateMapFeatures(geoJsonFeatures: List<GeoJsonFeature>) {
    if (mapFeatures.size <= MAP_FEATURES_LIMIT) {
      mapFeatures.addAll(geoJsonFeatures.map { it.toFeature() })
    }
  }

  fun clearMapFeatures() = mapFeatures.clear()

  fun getServicePointKeyToType(): Map<String, ServicePointType> {
    // Use the class-level list to create the map
    return servicePointTypes.associateBy { it.name.lowercase() }
  }
}
