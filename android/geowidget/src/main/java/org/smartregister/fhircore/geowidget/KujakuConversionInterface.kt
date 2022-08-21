package org.smartregister.fhircore.geowidget

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 08-08-2022. */
import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import java.util.LinkedList
import org.apache.commons.codec.binary.Base64
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Resource
import org.json.JSONArray
import org.json.JSONObject
import org.smartregister.fhircore.geowidget.ext.getGeoJsonGeometry
import org.smartregister.fhircore.geowidget.ext.updateBoundaryGeoJsonProperties

class KujakuConversionInterface {

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

      conversionGuide.put(propertyKey, propertiesList)

      val declaredKeyValues = jsonObject.getJSONObject(propertyKey)

      declaredKeyValues.keys().forEach { key ->
        propertiesList.add(Pair(key, declaredKeyValues.optString(key)))
      }
    }
  }

  fun generateFeatureCollection(
    context: Context,
    resourcesGroups: List<List<Resource>>
  ): FeatureCollection {
    checkConversionGuide(context)

    val featureCollection = arrayOfNulls<Feature>(resourcesGroups.size)
    val fhirPath = FhirContext.forCached(FhirVersionEnum.R4).newFhirPath()

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

        conversionGuide.get(resource.resourceType.name)?.forEach { path ->
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
