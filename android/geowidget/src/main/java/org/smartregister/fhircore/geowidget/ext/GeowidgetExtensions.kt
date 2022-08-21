/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.geowidget.ext

import org.apache.commons.codec.binary.Base64
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Location
import org.json.JSONArray
import org.json.JSONObject
import org.smartregister.fhircore.geowidget.KujakuConversionInterface

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 16-08-2022. */
typealias Coordinate = Pair<Double, Double>

val Coordinate.latitude: Double
  get() = this.second

val Coordinate.longitude: Double
  get() = this.first

val Location.boundaryGeoJsonExtAttachment: Attachment?
  get() {
    return if (hasBoundaryGeoJsonExt) {
      getExtensionByUrl(KujakuConversionInterface.BOUNDARY_GEOJSON_EXT_URL).value as Attachment
    } else {
      return null
    }
  }

val Location.hasBoundaryGeoJsonExt
  get(): Boolean {
    if (hasExtension(KujakuConversionInterface.BOUNDARY_GEOJSON_EXT_URL)) {
      val boundaryGeojsonFeature =
        getExtensionByUrl(KujakuConversionInterface.BOUNDARY_GEOJSON_EXT_URL)
      if (boundaryGeojsonFeature != null && boundaryGeojsonFeature.value is Attachment) {
        val attachment = boundaryGeojsonFeature.value as Attachment

        if (attachment.contentType != null && attachment.contentType.equals("application/geo+json")
        ) {
          return true
        }
      }
    }
    return false
  }

fun Location.updateBoundaryGeoJsonProperties(feature: JSONObject) {
  if (hasBoundaryGeoJsonExt) {
    val featureFromExt =
      Base64.decodeBase64(boundaryGeoJsonExtAttachment!!.data).run { JSONObject(String(this)) }

    // Copy over the properties
    val extFeatureProperties = featureFromExt.optJSONObject("properties")
    extFeatureProperties?.keys()?.forEach { key ->
      if (!feature.getJSONObject("properties").has(key)) {
        feature.getJSONObject("properties").put(key, extFeatureProperties.get(key))
      }
    }
  }
}

fun Location.getGeoJsonGeometry(): JSONObject {
  val geometry = JSONObject()

  geometry.put("type", "Point")
  geometry.put("coordinates", JSONArray(arrayOf(position.longitude, position.latitude)))

  // Boundary GeoJson Extension geometry overrides any lat, long declared in the Location.lat
  // & Location.long
  if (hasBoundaryGeoJsonExt) {
    val featureFromExt =
      Base64.decodeBase64(boundaryGeoJsonExtAttachment!!.data).run { JSONObject(String(this)) }
    return featureFromExt.getJSONObject("geometry")
  }
  return geometry
}
