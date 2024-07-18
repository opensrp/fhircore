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

data class Feature(
  val geometry: Geometry? = null,
  val id: String = "",
  val properties: Map<String, Any> = emptyMap(),
  val serverVersion: Int = 0,
  val type: String = FEATURE,
)

data class Geometry(
  val coordinates: List<Coordinates>? = emptyList(),
  val type: String = POINT,
)

data class Coordinates(
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
)

const val TYPE = "type"
const val POINT = "Point"
const val FEATURE = "Feature"
