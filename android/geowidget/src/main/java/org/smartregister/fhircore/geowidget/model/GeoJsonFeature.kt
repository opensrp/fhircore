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

import com.mapbox.geojson.Feature
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import org.smartregister.fhircore.engine.util.extension.encodeJson

const val TYPE = "type"
const val POINT = "Point"
const val FEATURE = "Feature"

@Serializable
data class GeoJsonFeature(
  val geometry: Geometry? = null,
  val id: String = "",
  val properties: Map<String, JsonPrimitive>? = emptyMap(),
  val type: String = FEATURE,
  val serverVersion: Int = 0,
) : java.io.Serializable {
  fun toFeature(): Feature = Feature.fromJson(this.encodeJson())
}

/**
 * A representation f a Point in a Map. Please note that these coordinates use longitude, latitude
 * coordinate order (as opposed to latitude, longitude) to match the GeoJSON specification, which is
 * equivalent to the OGC:CRS84 coordinate reference system. Refer to
 * [MapBox Geography and geometry documentation](https://docs.mapbox.com/mapbox-gl-js/api/geography/)
 */
@Serializable
data class Geometry(
  val coordinates: List<Double>? = emptyList(),
  val type: String = POINT,
) : java.io.Serializable
