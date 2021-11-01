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

package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.jupiter.api.Test

class PatientExtensionTest {

  @Test
  fun testExtractAddressShouldReturnFullAddress() {
    val patient =
      Patient().apply {
        addAddress().apply {
          this.addLine("12 B")
          this.addLine("Gulshan")
          this.district = "Karimabad"
          this.state = "Sindh"
        }
      }

    Assert.assertEquals("12 B, Gulshan, Karimabad Sindh", patient.extractAddress())
  }

  @Test
  fun testGetAgeString() {
    val expectedAge = "1y"
    Assert.assertEquals(expectedAge, getAgeStringFromDays(365))

    val expectedAge2 = "1y 1m"
    Assert.assertEquals(expectedAge2, getAgeStringFromDays(395))

    val expectedAge3 = "1y 1w"
    Assert.assertEquals(expectedAge3, getAgeStringFromDays(372))

    val expectedAge4 = "1m 5d"
    Assert.assertEquals(expectedAge4, getAgeStringFromDays(35))

    val expectedAge5 = "1w"
    Assert.assertEquals(expectedAge5, getAgeStringFromDays(7))

    val expectedAge6 = "4d"
    Assert.assertEquals(expectedAge6, getAgeStringFromDays(4))

    val expectedAge7 = "1w 2d"
    Assert.assertEquals(expectedAge7, getAgeStringFromDays(9))

    val expectedAge8 = "1m 2w 3d"
    Assert.assertEquals(expectedAge8, getAgeStringFromDays(47))

    val expectedAge9 = "1y 2m 3w 4d"
    Assert.assertEquals(expectedAge9, getAgeStringFromDays(450))

    val expectedAge10 = "40y 3m 1w 3d"
    Assert.assertEquals(expectedAge10, getAgeStringFromDays(14700))
  }
}
