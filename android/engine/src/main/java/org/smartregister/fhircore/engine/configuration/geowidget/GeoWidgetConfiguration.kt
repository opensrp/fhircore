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

package org.smartregister.fhircore.engine.configuration.geowidget

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterContentConfig
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig

@Serializable
data class GeoWidgetConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.GeoWidget.name,
  val id: String,
  val profileId: String,
  val topScreenSection: TopScreenSection?= null,
  val registrationQuestionnaire: QuestionnaireConfig,
  val mapLayers : List<MapLayer> = arrayListOf(MapLayer.StreetLayer(true)),
  val shouldShowLocationButton: Boolean = false,
  val shouldShowPlaneSwitcherButton: Boolean = false,
  val shouldShowAddLocationButton: Boolean = false,
  val resourceConfig: FhirResourceConfig? = null
) : Configuration()

//TODO : would need to change the type of class to use in the configuration
@Serializable
sealed class MapLayer {
  data class StreetLayer(val isDefault: Boolean) : MapLayer()
  data class SatelliteLayer(val isDefault: Boolean) : MapLayer()
  data class StreetSatelliteLayer(val isDefault: Boolean) : MapLayer()
}
// TODO : this can be moved to Configuration class since TopScreenSection is being used on both Register and GeoWidget ConfigTypes
@Serializable
data class TopScreenSection(
  val searchBar: RegisterContentConfig?,
  val toggleAction: List<ActionConfig>?,
  val screenTitle: String,
  val toggleIconConfig: ImageConfig,
  val showToggleButton: Boolean? = false
)