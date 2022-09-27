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

import java.util.Calendar
import java.util.Date
import org.apache.commons.lang3.time.DateUtils
import org.hl7.fhir.r4.model.DateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class DateTimeExtensionTest : RobolectricTest() {

  @Test
  fun testDateTypeAsDdMmmYyyyShouldReturnFormattedDate() {
    val date = DateType("2012-10-12").dateTimeValue().value

    val result = date.asDdMmmYyyy()

    assertEquals("12-Oct-2012", result)
  }

  @Test
  fun testDateTypePlusWeeksAsStringShouldAddWeeksAndReturnFormattedDate() {
    val date = DateType("2012-10-12")

    val formatted = date.plusWeeksAsString(2)

    assertTrue("2012-10-26".contentEquals(formatted))
  }

  @Test
  fun testDateTypePlusWeeksAsStringShouldAddMonthsAndReturnFormattedDate() {
    val date = DateType("2012-10-12")

    val formatted = date.plusMonthsAsString(3)

    assertTrue("2013-01-12".contentEquals(formatted))
  }

  @Test
  fun testDateAsMmmYyyyShouldReturnFormattedDate() {
    val date = DateUtils.parseDate("2022-02-02", "yyyy-MM-dd")

    val result = date.asMmmYyyy()

    assertEquals("Feb-2022", result)
  }

  @Test
  fun testDatePlusYearsShouldAddYearsToDate() {
    val date = DateType("2010-10-12").value

    val added = date.plusYears(8).asYyyyMmDd()

    assertTrue("2018-10-12".contentEquals(added))
  }

  @Test
  fun testDateYearsPassedShouldReturnCorrectValue() {
    val date = Calendar.getInstance().apply { add(Calendar.YEAR, -9) }.time

    val years = date.yearsPassed()

    assertEquals(9, years)
  }

  @Test
  fun testDateAgeDisplayShouldReturnCorrectAge() {
    val date = Calendar.getInstance().apply { add(Calendar.YEAR, -9) }.time

    val age = date.toAgeDisplay()

    assertEquals("9y", age)
  }

  @Test
  fun `Date#toHumanDisplay() should return Date in the correct format`() {
    val date = Date("Fri, 1 Oct 2021 13:30:00")
    val formattedDate = date.toHumanDisplay()
    assertEquals("Oct 1, 2021 1:30:00 PM", formattedDate)
  }
}
