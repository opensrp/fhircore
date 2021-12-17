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

import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class AncOverviewConfigurationTest : RobolectricTest() {

  @Test
  fun testAllClassMemberShouldNotNull() {
    val id = "0"
    val eddFilter = mockk<SearchFilter> { every { key } returns "1" }
    val gaFilter = mockk<SearchFilter> { every { key } returns "2" }
    val fetusesFilter = mockk<SearchFilter> { every { key } returns "3" }
    val riskFilter = mockk<SearchFilter> { every { key } returns "4" }
    val weightFilter = mockk<SearchFilter> { every { key } returns "5" }
    val heightFilter = mockk<SearchFilter> { every { key } returns "6" }
    val bloodOxygenLevelFilter = mockk<SearchFilter> { every { key } returns "7" }
    val BPSFilter = mockk<SearchFilter> { every { key } returns "8" }
    val BPDSFilter = mockk<SearchFilter> { every { key } returns "9" }
    val pulseRateFilter = mockk<SearchFilter> { every { key } returns "10" }
    val bloodGlucoseFilter = mockk<SearchFilter> { every { key } returns "11" }
    val bmiFilter = mockk<SearchFilter> { every { key } returns "12" }

    val ancOverviewConfiguration =
      AncOverviewConfiguration(
        id = id,
        eddFilter = eddFilter,
        gaFilter = gaFilter,
        fetusesFilter = fetusesFilter,
        riskFilter = riskFilter,
        weightFilter = weightFilter,
        heightFilter = heightFilter,
        bloodOxygenLevelFilter = bloodOxygenLevelFilter,
        BPSFilter = BPSFilter,
        BPDSFilter = BPDSFilter,
        pulseRateFilter = pulseRateFilter,
        bloodGlucoseFilter = bloodGlucoseFilter,
        bmiFilter = bmiFilter
      )

    assertEquals("0", ancOverviewConfiguration.id)
    assertEquals("1", ancOverviewConfiguration.eddFilter?.key)
    assertEquals("2", ancOverviewConfiguration.gaFilter?.key)
    assertEquals("3", ancOverviewConfiguration.fetusesFilter?.key)
    assertEquals("4", ancOverviewConfiguration.riskFilter?.key)
    assertEquals("5", ancOverviewConfiguration.weightFilter?.key)
    assertEquals("6", ancOverviewConfiguration.heightFilter?.key)
    assertEquals("7", ancOverviewConfiguration.bloodOxygenLevelFilter?.key)
    assertEquals("8", ancOverviewConfiguration.BPSFilter?.key)
    assertEquals("9", ancOverviewConfiguration.BPDSFilter?.key)
    assertEquals("10", ancOverviewConfiguration.pulseRateFilter?.key)
    assertEquals("11", ancOverviewConfiguration.bloodGlucoseFilter?.key)
    assertEquals("12", ancOverviewConfiguration.bmiFilter?.key)
  }
}
