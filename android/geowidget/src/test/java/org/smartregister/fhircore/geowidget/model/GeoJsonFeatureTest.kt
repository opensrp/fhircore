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

import com.mapbox.geojson.Point
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert
import org.junit.Test

class GeoJsonFeatureTest {

  @Test
  fun testToFeatureIncludesIdGeometryAndProperties() {
    val geoJsonFeature =
      GeoJsonFeature(
        id = "feature-123",
        geometry = Geometry(coordinates = listOf(10.5, -20.25)),
        properties =
          mapOf(
            "category" to JsonPrimitive("clinic"),
            "count" to JsonPrimitive(3),
          ),
        serverVersion = 42,
      )

    val feature = geoJsonFeature.toFeature()

    Assert.assertEquals("feature-123", feature.id())
    val point = feature.geometry() as Point
    Assert.assertEquals(10.5, point.longitude(), 0.0)
    Assert.assertEquals(-20.25, point.latitude(), 0.0)
    Assert.assertEquals("clinic", feature.getStringProperty("category"))
    Assert.assertEquals(3, feature.getNumberProperty("count").toInt())
  }

  @Test
  fun testToFeatureHandlesNullProperties() {
    val geoJsonFeature =
      GeoJsonFeature(
        id = "no-props",
        geometry = Geometry(coordinates = listOf(1.0, 2.0)),
        properties = null,
      )

    val feature = geoJsonFeature.toFeature()

    Assert.assertEquals("no-props", feature.id())
    Assert.assertFalse(feature.hasProperty("category"))
    Assert.assertTrue(feature.geometry() is Point)
  }
}

