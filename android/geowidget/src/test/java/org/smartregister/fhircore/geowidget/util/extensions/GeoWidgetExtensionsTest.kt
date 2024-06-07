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

import android.os.Build
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class GeoWidgetExtensionsTest {
  @Test
  fun `returns geometry object when input JSONObject has non-null geometry and coordinates fields`() {
    // Given
    val jsonObject = JSONObject()
    val geometryObject = JSONObject()
    val coordinatesArray = JSONArray()
    coordinatesArray.put(1.0)
    coordinatesArray.put(2.0)
    geometryObject.put("coordinates", coordinatesArray)
    jsonObject.put("geometry", geometryObject)

    // When
    val result = jsonObject.geometry()

    // Then
    Assert.assertNotNull(result)
    Assert.assertEquals(1, result?.coordinates?.size)
    Assert.assertEquals(1.0, result?.coordinates?.get(0)?.latitude)
    Assert.assertEquals(2.0, result?.coordinates?.get(0)?.longitude)
  }

  // Should return an empty map when input map is empty
  @Test
  fun `should return empty map when input map is empty`() {
    // Given
    val inputMap = emptyMap<String, JsonElement>()

    // When
    val result = inputMap.featureProperties()

    // Then
    Assert.assertTrue(result.isEmpty())
  }

  // Should return a map with boolean values converted to strings when input map has boolean values
  @Test
  fun `should return map with boolean values converted to strings when input map has boolean values`() {
    // Given
    val inputMap =
      mapOf(
        "key1" to JsonPrimitive(true),
        "key2" to JsonPrimitive(false),
      )

    // When
    val result = inputMap.featureProperties()

    // Then
    Assert.assertEquals("true", result["key1"])
    Assert.assertEquals("false", result["key2"])
  }
}
