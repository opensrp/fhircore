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

package org.smartregister.fhircore.quest.ui.geowidget

import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery

sealed class GeoWidgetEvent {
  data object ClearMap : GeoWidgetEvent()

  data class RetrieveFeatures(
    val geoWidgetConfig: GeoWidgetConfiguration,
    val searchQuery: SearchQuery = SearchQuery.emptyText,
  ) : GeoWidgetEvent()
}
