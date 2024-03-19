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

package org.smartregister.fhircore.quest.util

import android.location.Location
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test

class ResourceUtilsTest {

  @Test
  fun testLocationResourceIsCreated() {
    val location =
      Location("").apply {
        longitude = 10.0
        latitude = 20.0
        altitude = 30.0
      }

    val locationResource = ResourceUtils.createFhirLocationFromGpsLocation(location)

    assertNotNull(locationResource.id)
    assertEquals(location.longitude.toBigDecimal(), locationResource.position.longitude)
    assertEquals(location.latitude.toBigDecimal(), locationResource.position.latitude)
    assertEquals(location.altitude.toBigDecimal(), locationResource.position.altitude)
  }
}
