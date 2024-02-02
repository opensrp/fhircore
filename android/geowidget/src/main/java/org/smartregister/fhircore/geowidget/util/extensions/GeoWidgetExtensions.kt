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

package org.smartregister.fhircore.geowidget.util.extensions

import org.json.JSONArray
import org.json.JSONObject
import org.smartregister.fhircore.geowidget.model.GeoWidgetLocation
import org.smartregister.fhircore.geowidget.model.Position

fun GeoWidgetLocation.getGeoJsonGeometry(): JSONObject {
  position ?: return JSONObject()

  val geometry = JSONObject()

  geometry.put("type", "Point")
  geometry.put("coordinates", JSONArray(arrayOf(position.latitude, position.longitude)))

  return geometry
}

fun JSONObject.position(): Position? {
  return optJSONObject("geometry")?.run {
    optJSONArray("coordinates")?.run { Position(optDouble(0), optDouble(1)) }
  }
}
