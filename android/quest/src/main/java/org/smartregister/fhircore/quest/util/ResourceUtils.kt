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

package org.smartregister.fhircore.quest.util

import java.util.UUID
import org.hl7.fhir.r4.model.Location

class ResourceUtils {
  companion object {
    fun createLocationResource(gpsLocation: android.location.Location?): Location {
      val locationResource =
        gpsLocation?.let { location ->
          Location().apply {
            id = UUID.randomUUID().toString()
            position =
              Location.LocationPositionComponent().apply {
                latitude = location.latitude.toBigDecimal()
                longitude = location.longitude.toBigDecimal()
                altitude = location.altitude?.toBigDecimal()
              }
          }
        }
          ?: Location()

      return locationResource
    }
  }
}
