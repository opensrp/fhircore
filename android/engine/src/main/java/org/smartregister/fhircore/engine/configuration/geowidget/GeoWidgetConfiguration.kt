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
import org.smartregister.fhircore.engine.configuration.register.RegisterContentConfig
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RuleConfig

@Serializable
data class GeoWidgetConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.GeoWidget.name,
  val id: String,
  val profileId: String,
  val topScreenSection: TopScreenSectionConfig?= null,
  val registrationQuestionnaire: QuestionnaireConfig,
  val mapLayers : List<MapLayerConfig> = listOf(MapLayerConfig(Pair(MapLayer.STREET, true))),
  val showLocation: Boolean = false,
  val showPlaneSwitcher: Boolean = false,
  val showAddLocation: Boolean = false,
  val resourceConfig: FhirResourceConfig,
  val servicePointConfig: ServicePointConfig?,
  val summaryBottomSheetConfig: SummaryBottomSheetConfig?= null,
  val actions : List<ActionConfig>? = emptyList()
) : Configuration()

//fixme - move it to engine,
@Serializable
enum class MapLayer {
  STREET,
  SATELLITE,
  STREET_SATELLITE
}
@Serializable
data class MapLayerConfig(val pair: Pair<MapLayer, Boolean>)


//fixme - move it to Engine, domain -> model package.
@Serializable
data class TopScreenSectionConfig(
  val searchBar: RegisterContentConfig?,
  val title: String,
  val menuIcons: List<ImageProperties>? = null
)

@Serializable
data class ServicePointConfig(
  val rules: List<RuleConfig> = emptyList(),
  val servicePointProperties: Map<String, String> = emptyMap()
)