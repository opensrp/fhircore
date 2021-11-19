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
    // passing days value for 1y 1m 4d
    Assert.assertEquals(expectedAge2, getAgeStringFromDays(399))

    val expectedAge3 = "1y"
    // passing days value for 1y 1w
    Assert.assertEquals(expectedAge3, getAgeStringFromDays(372))

    val expectedAge4 = "1m"
    Assert.assertEquals(expectedAge4, getAgeStringFromDays(35))

    val expectedAge5 = "1m 2w"
    Assert.assertEquals(expectedAge5, getAgeStringFromDays(49))

    val expectedAge6 = "1w"
    Assert.assertEquals(expectedAge6, getAgeStringFromDays(7))

    val expectedAge7 = "1w 2d"
    Assert.assertEquals(expectedAge7, getAgeStringFromDays(9))

    val expectedAge8 = "3d"
    Assert.assertEquals(expectedAge8, getAgeStringFromDays(3))

    val expectedAge9 = "1y 2m"
    Assert.assertEquals(expectedAge9, getAgeStringFromDays(450))

    val expectedAge10 = "40y 3m"
    Assert.assertNotEquals(expectedAge10, getAgeStringFromDays(14700))

    val expectedAge11 = "40y"
    Assert.assertEquals(expectedAge11, getAgeStringFromDays(14700))

    val expectedAge12 = "0d"
    // if difference b/w current date and DOB is O from extractAge extension
    Assert.assertEquals(expectedAge12, getAgeStringFromDays(0))
  }

  @Test
  fun testExtractFamilyName() {
    val patient =
      Patient().apply {
        addName().apply {
          addGiven("Given Name")
          family = "genealogy"
        }
      }

    Assert.assertEquals("Genealogy Family", patient.extractFamilyName())
  }
}
