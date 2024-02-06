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
import java.util.UUID

class ResourceUtils {
  companion object {
    fun createLocationResource(
      gpsLocation: Location?,
      locationResource: org.hl7.fhir.r4.model.Location? = null,
    ): org.hl7.fhir.r4.model.Location {
      var locationResourceCopy = locationResource

      if (locationResourceCopy == null) {
        locationResourceCopy = org.hl7.fhir.r4.model.Location()
      }

      locationResourceCopy.id = UUID.randomUUID().toString()
      locationResourceCopy.position.latitude = gpsLocation!!.latitude.toBigDecimal()
      locationResourceCopy.position.longitude = gpsLocation!!.longitude.toBigDecimal()
      locationResourceCopy.position.altitude = gpsLocation!!.altitude.toBigDecimal()

      return locationResourceCopy
    }
  }
}
