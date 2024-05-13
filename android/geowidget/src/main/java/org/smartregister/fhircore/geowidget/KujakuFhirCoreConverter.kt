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

package org.smartregister.fhircore.geowidget

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import java.util.LinkedList
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Resource
import org.json.JSONObject
import org.smartregister.fhircore.geowidget.util.extensions.getGeoJsonGeometry
import org.smartregister.fhircore.geowidget.util.extensions.updateBoundaryGeoJsonProperties

class KujakuFhirCoreConverter {

  private lateinit var conversionGuide: HashMap<String, LinkedList<Pair<String, String>>>

  fun checkConversionGuide(context: Context) {
    if (!this::conversionGuide.isInitialized) {
      fetchConversionGuide(context)
    }
  }

  fun fetchConversionGuide(context: Context) {
    val fileString = String(context.assets.open("conversion_config.json").readBytes())

    val jsonObject = JSONObject(fileString)

    conversionGuide = HashMap()

    jsonObject.keys().forEach { propertyKey ->
      val propertiesList = LinkedList<Pair<String, String>>()

      conversionGuide[propertyKey] = propertiesList

      val declaredKeyValues = jsonObject.getJSONObject(propertyKey)

      declaredKeyValues.keys().forEach { key ->
        propertiesList.add(Pair(key, declaredKeyValues.optString(key)))
      }
    }
  }

  fun generateFeatureCollection(
    context: Context,
    resourcesGroups: List<List<Resource>>,
  ): FeatureCollection {
    checkConversionGuide(context)

    val featureCollection = arrayOfNulls<Feature>(resourcesGroups.size)
    val fhirPath = FhirContext.forR4Cached().newFhirPath()

    resourcesGroups.forEachIndexed { index, resourceGroup ->
      val feature = JSONObject()
      feature.put("type", "Feature")

      val properties = JSONObject()
      feature.put("properties", properties)

      resourceGroup.forEach { resource ->
        if (resource is Location) {
          val geometry = resource.getGeoJsonGeometry()
          feature.put("geometry", geometry)

          resource.updateBoundaryGeoJsonProperties(feature)
        }

        conversionGuide[resource.resourceType.name]?.forEach { path ->
          val value = fhirPath.evaluate(resource, path.second, Base::class.java).firstOrNull()
          properties.put(path.first, value)
        }
      }

      featureCollection[index] = Feature.fromJson(feature.toString())
    }

    return FeatureCollection.fromFeatures(featureCollection)
  }

  companion object {
    const val BOUNDARY_GEOJSON_EXT_URL =
      "http://hl7.org/fhir/StructureDefinition/location-boundary-geojson"
  }
}
