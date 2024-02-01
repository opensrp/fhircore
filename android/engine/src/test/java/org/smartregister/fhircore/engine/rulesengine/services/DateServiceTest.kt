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

package org.smartregister.fhircore.engine.rulesengine.services

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.apache.commons.lang3.NotImplementedException
import org.joda.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.SDF_DD_MMM_YYYY
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate

@HiltAndroidTest
class DateServiceTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Test
  fun testSubtractingDaysFromCurrentDate() {
    val result =
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        5,
        "-",
        timeUnit = "DAY",
        dateFormat = SDF_DD_MMM_YYYY,
      )
    val expected = LocalDate.now().minusDays(5).toDate().formatDate(SDF_DD_MMM_YYYY)
    assertEquals(expected, result)
  }

  @Test
  fun testAddingDaysToCurrentDate() {
    val result =
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        3,
        "+",
        timeUnit = "DAY",
      )
    val expected = LocalDate.now().plusDays(3).toDate().formatDate(SDF_YYYY_MM_DD)
    assertEquals(expected, result)
  }

  @Test
  fun testUnsupportedOperationThrowExceptionWhenTimeUnitIsDay() {
    assertThrows(NotImplementedException::class.java) {
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        3,
        "*",
        timeUnit = "DAY",
      )
    }
  }

  @Test
  fun testSubtractingWeeksFromCurrentDate() {
    val result =
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        5,
        "-",
        timeUnit = "WEEK",
        dateFormat = SDF_DD_MMM_YYYY,
      )
    val expected = LocalDate.now().minusWeeks(5).toDate().formatDate(SDF_DD_MMM_YYYY)
    assertEquals(expected, result)
  }

  @Test
  fun testAddingWeeksToCurrentDate() {
    val result =
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        3,
        "+",
        timeUnit = "WEEK",
      )
    val expected = LocalDate.now().plusWeeks(3).toDate().formatDate(SDF_YYYY_MM_DD)
    assertEquals(expected, result)
  }

  @Test
  fun testUnsupportedOperationThrowExceptionWhenTimeUnitIsWeek() {
    assertThrows(NotImplementedException::class.java) {
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        3,
        "*",
        timeUnit = "WEEK",
      )
    }
  }

  @Test
  fun testSubtractingMonthsFromCurrentDate() {
    val result =
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        5,
        "-",
        timeUnit = "MONTH",
        dateFormat = SDF_DD_MMM_YYYY,
      )
    val expected = LocalDate.now().minusMonths(5).toDate().formatDate(SDF_DD_MMM_YYYY)
    assertEquals(expected, result)
  }

  @Test
  fun testAddingMonthsToCurrentDate() {
    val result =
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        3,
        "+",
        timeUnit = "MONTH",
      )
    val expected = LocalDate.now().plusMonths(3).toDate().formatDate(SDF_YYYY_MM_DD)
    assertEquals(expected, result)
  }

  @Test
  fun testUnsupportedOperationThrowExceptionWhenTimeUnitIsMonth() {
    assertThrows(NotImplementedException::class.java) {
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        3,
        "*",
        timeUnit = "MONTH",
      )
    }
  }

  @Test
  fun testGenericSubtractingYearsFromCurrentDate() {
    val result =
      DateService.addOrSubtractTimeUnitFromCurrentDate(
        5,
        "-",
        timeUnit = "YEAR",
        dateFormat = SDF_DD_MMM_YYYY,
      )
    val expected = LocalDate.now().minusYears(5).toDate().formatDate(SDF_DD_MMM_YYYY)
    assertEquals(expected, result)
  }

  @Test
  fun testGenericAddingYearsToCurrentDate() {
    val result = DateService.addOrSubtractTimeUnitFromCurrentDate(3, "+", timeUnit = "YEAR")
    val expected = LocalDate.now().plusYears(3).toDate().formatDate(SDF_YYYY_MM_DD)
    assertEquals(expected, result)
  }

  @Test
  fun testUnsupportedOperationThrowExceptionWhenTimeUnitIsYear() {
    assertThrows(NotImplementedException::class.java) {
      DateService.addOrSubtractTimeUnitFromCurrentDate(2, "*", timeUnit = "YEAR")
    }
  }

  @Test
  fun testCompareDates() {
    val result =
      DateService.compareDates(
        firstDateFormat = SDF_YYYY_MM_DD,
        firstDateString = "2023-09-01",
        secondDateFormat = SDF_YYYY_MM_DD,
        secondDateString = "2024-01-01",
      )
    assertEquals(-1, result)

    val result2 =
      DateService.compareDates(
        firstDateFormat = SDF_YYYY_MM_DD,
        firstDateString = "2024-31-01",
        secondDateFormat = SDF_YYYY_MM_DD,
        secondDateString = "2024-01-01",
      )
    assertEquals(1, result2)

    val result3 =
      DateService.compareDates(
        firstDateFormat = SDF_YYYY_MM_DD,
        firstDateString = "2024-01-01",
        secondDateFormat = SDF_YYYY_MM_DD,
        secondDateString = "2024-01-01",
      )
    assertEquals(0, result3)
  }
}
