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

package org.smartregister.fhircore.engine.rulesengine.services

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.apache.commons.lang3.NotImplementedException
import org.joda.time.LocalDate
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.SDF_DD_MMM_YYYY
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate

@HiltAndroidTest
class DateServiceTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  private lateinit var dateService: DateService

  @Before
  fun setUp() {
    hiltRule.inject()
    dateService = DateService
    Assert.assertNotNull(dateService)
  }

  @Test
  fun `test subtracting years from current date`() {
    val result = dateService.addOrSubtractYearFromCurrentDate(5, "-", dateFormat = SDF_DD_MMM_YYYY)
    val expected = LocalDate.now().minusYears(5).toDate().formatDate(SDF_DD_MMM_YYYY)
    assertEquals(expected, result)
  }

  @Test
  fun `test adding years to current date`() {
    val result = dateService.addOrSubtractYearFromCurrentDate(3, "+")
    val expected = LocalDate.now().plusYears(3).toDate().formatDate(SDF_YYYY_MM_DD)
    assertEquals(expected, result)
  }

  @Test
  fun `test unsupported operation`() {
    assertThrows(NotImplementedException::class.java) {
      dateService.addOrSubtractYearFromCurrentDate(2, "*")
    }
  }
}
