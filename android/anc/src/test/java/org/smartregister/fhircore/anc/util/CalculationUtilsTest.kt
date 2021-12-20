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

package org.smartregister.fhircore.anc.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationUtilsTest {

  @Test
  fun testBMIViaUSCUnitIsComputed() {
    val expectedBMI = 22.96
    val computedBMI = computeBMIViaUSCUnits(70.0, 160.0)
    assertEquals(expectedBMI, computedBMI, 0.1)
  }

  @Test
  fun testBMIviaMetricIsComputed() {
    val expectedBMI = 22.90
    val computedBMI = computeBMIViaMetricUnits(178.0, 72.57)
    assertEquals(expectedBMI, computedBMI, 0.1)
  }
}
