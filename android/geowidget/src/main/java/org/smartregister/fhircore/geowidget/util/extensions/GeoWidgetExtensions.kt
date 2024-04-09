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
import org.smartregister.fhircore.geowidget.model.Coordinates
import org.smartregister.fhircore.geowidget.model.Feature
import org.smartregister.fhircore.geowidget.model.Geometry

fun Feature.getGeoJsonGeometry(): JSONObject {
  geometry ?: return JSONObject()

  val geometryObject = JSONObject()

  geometryObject.put("type", geometry.type)
  geometryObject.put("coordinates", JSONArray(arrayOf(geometry.coordinates?.longitude, geometry.coordinates?.latitude)))
  return geometryObject
}

fun JSONObject.geometry(): Geometry? {
  return optJSONObject("geometry")?.run {
    optJSONArray("coordinates")?.run { Geometry(Coordinates(optDouble(0), optDouble(1)) ) }
  }
}

fun Feature.getProperties(): JSONObject {
  properties ?: return JSONObject()

  val propertiesObject = JSONObject()
  properties.forEach { key, value ->
    propertiesObject.put(key, value)
  }
  return propertiesObject
}
