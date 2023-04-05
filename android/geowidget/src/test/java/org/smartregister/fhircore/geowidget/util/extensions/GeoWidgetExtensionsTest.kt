/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import android.os.Build
import org.apache.commons.codec.binary.Base64
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Location
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.smartregister.fhircore.geowidget.KujakuFhirCoreConverter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class GeoWidgetExtensionsTest {
  @Test
  fun `boundaryGeoJsonExtAttachment should return null if no extension is present`() {
    val location = Location()
    Assert.assertNull(location.boundaryGeoJsonExtAttachment)
  }

  @Test
  fun `hasBoundaryGeoJsonExt should return false if has extension but no boundary value`() {
    val location =
      Location().apply {
        extension =
          mutableListOf(
            Extension().apply { url = KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL }
          )
      }
    Assert.assertFalse(location.hasBoundaryGeoJsonExt)
  }

  @Test
  fun `hasBoundaryGeoJsonExt should return false if has attachment but no or wrong content type`() {
    var location =
      Location().apply {
        extension =
          mutableListOf(
            Extension().apply {
              url = KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL
              setValue(Attachment())
            }
          )
      }
    Assert.assertFalse(location.hasBoundaryGeoJsonExt)

    location =
      Location().apply {
        extension =
          mutableListOf(
            Extension().apply {
              url = KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL
              setValue(Attachment().apply { contentType = "application/something-else" })
            }
          )
      }
    Assert.assertFalse(location.hasBoundaryGeoJsonExt)
  }

  @Test
  fun `updateBoundaryGeoJsonProperties gets and adds new property key`() {
    val location =
      Location().apply {
        extension =
          listOf(
            Extension(KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL).apply {
              setValue(
                Attachment().apply {
                  contentType = "application/geo+json"
                  data =
                    Base64.encodeBase64("""{"properties":{"key":"value"}}""".encodeToByteArray())
                }
              )
            }
          )
      }
    val feature = JSONObject().apply { put("properties", JSONObject()) }
    location.updateBoundaryGeoJsonProperties(feature)

    Assert.assertEquals("value", feature.getJSONObject("properties").get("key"))
  }

  @Test
  fun `updateBoundaryGeoJsonProperties does nothing if no location properties keys`() {
    val location =
      Location().apply {
        extension =
          listOf(
            Extension(KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL).apply {
              setValue(
                Attachment().apply {
                  contentType = "application/geo+json"
                  data = Base64.encodeBase64("""{"properties":{}}""".encodeToByteArray())
                }
              )
            }
          )
      }
    val feature =
      JSONObject().apply { put("properties", JSONObject().apply { put("key", "old-value") }) }
    location.updateBoundaryGeoJsonProperties(feature)

    Assert.assertEquals("old-value", feature.getJSONObject("properties").get("key"))
  }

  @Test
  fun `updateBoundaryGeoJsonProperties does nothing if no location properties`() {
    val location =
      Location().apply {
        extension =
          listOf(
            Extension(KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL).apply {
              setValue(
                Attachment().apply {
                  contentType = "application/geo+json"
                  data = Base64.encodeBase64("""{}""".encodeToByteArray())
                }
              )
            }
          )
      }
    val feature =
      JSONObject().apply { put("properties", JSONObject().apply { put("key", "old-value") }) }
    location.updateBoundaryGeoJsonProperties(feature)

    Assert.assertEquals("old-value", feature.getJSONObject("properties").get("key"))
  }

  @Test
  fun `updateBoundaryGeoJsonProperties does nothing if property key exists`() {
    val location =
      Location().apply {
        extension =
          listOf(
            Extension(KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL).apply {
              setValue(
                Attachment().apply {
                  contentType = "application/geo+json"
                  data =
                    Base64.encodeBase64("""{"properties":{"key":"value"}}""".encodeToByteArray())
                }
              )
            }
          )
      }
    val feature =
      JSONObject().apply { put("properties", JSONObject().apply { put("key", "old-value") }) }
    location.updateBoundaryGeoJsonProperties(feature)

    Assert.assertEquals("old-value", feature.getJSONObject("properties").get("key"))
  }

  @Test
  fun `generateLocation creates location with feature and coordinates`() {
    val coordinate = Coordinate(12.83203125, 28.304380682962783)
    val feature = JSONObject().apply { put("type", "Feature") }
    val location = generateLocation(feature, coordinate)

    Assert.assertEquals(
      "Feature",
      Base64.decodeBase64(location.boundaryGeoJsonExtAttachment!!.data)
        .run { JSONObject(String(this)) }
        .get("type")
    )
    Assert.assertEquals(12.83203125, location.position?.longitude?.toDouble())
    Assert.assertEquals(28.304380682962783, location.position?.latitude?.toDouble())
    Assert.assertEquals(Location.LocationStatus.INACTIVE, location.status)
    Assert.assertNotNull(location.id)
  }

  @Test
  fun `coordinates returns a geometry coordinates from a JSONObject`() {
    var feature = JSONObject()
    Assert.assertNull(feature.coordinates())

    feature = JSONObject(String("""{"geometry":{}}""".encodeToByteArray()))
    Assert.assertNull(feature.coordinates())

    val coordinate = Coordinate(12.83203125, 28.304380682962783)
    feature =
      JSONObject(
        String(
          """{"geometry":{"coordinates":[12.83203125,28.304380682962783]}}""".encodeToByteArray()
        )
      )
    Assert.assertEquals(coordinate, feature.coordinates())
  }
}
