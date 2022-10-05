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

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.apache.commons.lang3.time.DateUtils
import org.hl7.fhir.r4.model.DateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
  fun `Date asMmm() should return correct formatted date`() {
    val date = DateUtils.parseDate("2022-02-02", "yyyy-MM-dd")

    val result = date.asMmm()

    assertEquals("Feb", result)
  }

  @Test
  fun `SimpleDateFormat tryParse() should parse given date correctly`() {
    val dateFormat = SimpleDateFormat("yyyy-MMM-dd")

    val result = dateFormat.tryParse("2022-Feb-28")
    val calendarDate = result!!.calendar()

    assertEquals(2022, calendarDate.get(Calendar.YEAR))
    assertEquals(1, calendarDate.get(Calendar.MONTH)) // months are 0 indexed
    assertEquals(28, calendarDate.get(Calendar.DATE))
  }

  @Test
  fun `SimpleDateFormat tryParse() should parse given date with specified locale`() {
    val dateFormat = SimpleDateFormat("yyyy-MMM-dd", Locale.FRENCH)

    val result = dateFormat.tryParse("2022-févr.-28")
    val calendarDate = result!!.calendar()

    assertEquals(2022, calendarDate.get(Calendar.YEAR))
    assertEquals(1, calendarDate.get(Calendar.MONTH)) // months are 0 indexed
    assertEquals(28, calendarDate.get(Calendar.DATE))
  }

  @Test
  fun `SimpleDateFormat tryParse() with locale should parse given date of US locale`() {
    val dateFormat = SimpleDateFormat("yyyy-MMM-dd", Locale.FRENCH)

    val result = dateFormat.tryParse("2022-Feb-28")
    val calendarDate = Calendar.getInstance().apply { time = result }

    assertEquals(2022, calendarDate.get(Calendar.YEAR))
    assertEquals(1, calendarDate.get(Calendar.MONTH)) // months are 0 indexed
    assertEquals(28, calendarDate.get(Calendar.DATE))
  }

  @Test
  fun `SimpleDateFormat tryParse() should return null with invalid date`() {
    val dateFormat = SimpleDateFormat("yyyy-MMM-dd", Locale.FRENCH)

    val result = dateFormat.tryParse("2022-Fee-28")

    assertNull(result)
  }

  @Test
  fun `SimpleDateFormat tryParse() with list should parse given date with locale`() {
    val dateFormat1 = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH)
    val dateFormat2 = SimpleDateFormat("yyyy-MMM-dd", Locale.FRENCH)

    val result = listOf(dateFormat1, dateFormat2).tryParse("2022-Feb-28")
    val calendarDate = Calendar.getInstance().apply { time = result }

    assertEquals(2022, calendarDate.get(Calendar.YEAR))
    assertEquals(1, calendarDate.get(Calendar.MONTH)) // months are 0 indexed
    assertEquals(28, calendarDate.get(Calendar.DATE))
  }

  @Test
  fun `isSameMonthYear() should return true when month strings are exactly same format`() {
    val result = isSameMonthYear("Feb-2021", "Feb-2021")

    assertTrue(result)
  }

  @Test
  fun `isSameMonthYear() should return true when month strings are same with different format`() {
    val result = isSameMonthYear("Feb-2021", "2021-Feb")

    assertTrue(result)
  }

  @Test
  fun `isSameMonthYear() should return false when month strings are different with same format`() {
    val result = isSameMonthYear("Feb-2021", "Jan-2021")

    assertFalse(result)
  }

  @Test
  fun `isSameMonthYear() should return false when month strings are have different year with same format`() {
    val result = isSameMonthYear("Jan-2022", "Jan-2021")

    assertFalse(result)
  }

  @Test
  fun `isSameMonthYear() should return false when month strings are have different year with different format`() {
    val result = isSameMonthYear("Jan-2022", "2021-Jan")

    assertFalse(result)
  }

  @Test
  fun `isSameMonthYear() should return false when month strings have one with invalid format`() {
    val result = isSameMonthYear("Jan-21", "2021-Jan")

    assertFalse(result)
  }

  @Test
  fun `isSameMonthYear() should return false when month strings have both with invalid format`() {
    val result = isSameMonthYear("Jan-21", "2021-Ja")

    assertFalse(result)
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
