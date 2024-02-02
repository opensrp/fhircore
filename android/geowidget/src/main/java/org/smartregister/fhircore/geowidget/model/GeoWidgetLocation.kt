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

package org.smartregister.fhircore.geowidget.model

import org.json.JSONObject

data class GeoWidgetLocation(
  val id: String = "",
  val name: String = "",
  val position: Position? = null,
  val contexts: List<Context> = listOf(),
)

data class Position(
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
)

data class Context(
  val id: String = "", // the id of 'type'
  val type: String = "", // Group, Patient, Healthcare Service
)
