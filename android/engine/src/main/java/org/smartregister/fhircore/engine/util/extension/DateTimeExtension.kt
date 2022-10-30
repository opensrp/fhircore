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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.ocpsoft.prettytime.PrettyTime

val SDF_DD_MMM_YYYY = simpleDateFormatFor("dd-MMM-yyyy")
val SDF_DD_MMM = simpleDateFormatFor("dd MMM")
val SDF_YYYY_MM_DD = simpleDateFormatFor("yyyy-MM-dd")
val SDF_MMM_YYYY = simpleDateFormatFor("MMM-yyyy")
val SDF_YYYY_MMM = simpleDateFormatFor("yyyy-MMM")
val SDF_YYYY = simpleDateFormatFor("yyyy")
val SDF_MMM = simpleDateFormatFor("MMM")
val SDF_MM = simpleDateFormatFor("MM")
val SDF_MMMM_YYYY = simpleDateFormatFor("MMMM-yyyy\"")
val SDF_MMMM = simpleDateFormatFor("MMMM")


fun simpleDateFormatFor(pattern: String, locale: Locale = Locale.getDefault()) =
  SimpleDateFormat(pattern, locale)

fun OffsetDateTime.asString(): String {
  return this.format(DateTimeFormatter.RFC_1123_DATE_TIME)
}

fun Date?.asDdMmm(): String {
  if (this == null) return ""
  return SDF_DD_MMM.format(this)
}

fun Date?.asMmmYyyy(): String {
  if (this == null) return ""
  return SDF_MMM_YYYY.format(this)
}

fun Date?.asMmmmYyyy(): String {
  if (this == null) return ""
  return SDF_MMMM_YYYY.format(this)
}

fun Date?.asYyyy(): String {
  if (this == null) return ""
  return SDF_YYYY.format(this)
}

fun Date?.asMmm(): String {
  if (this == null) return ""
  return SDF_MMM.format(this)
}

fun Date?.asMm(): String {
  if (this == null) return ""
  return SDF_MM.format(this)
}

fun Date?.asMmmm(): String {
  if (this == null) return ""
  return SDF_MMMM.format(this)
}

fun SimpleDateFormat.tryParse(date: String): Date? =
  kotlin.runCatching { parse(date) }.getOrNull() ?: tryParse(date, Locale.US)

fun SimpleDateFormat.tryParse(date: String, locale: Locale): Date? =
  kotlin.runCatching { SimpleDateFormat(this.toPattern(), locale).parse(date) }.getOrNull()

fun List<SimpleDateFormat>.tryParse(date: String): Date? {
  forEach { dateFormat ->
    dateFormat.tryParse(date)?.let {
      return it
    }
  }
  return null
}

fun Date.asDdMmmYyyy(): String {
  return SDF_DD_MMM_YYYY.format(this)
}
fun Date.asDdMmYyyy(): String {
  return SDF_DD_MMM_YYYY.format(this)
}

fun Date.asYyyyMmDd(): String {
  return SDF_YYYY_MM_DD.format(this)
}

fun Date.toHumanDisplay(): String =
  SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.getDefault()).format(this)

fun Date?.makeItReadable(): String {
  return if (this == null) "N/A"
  else {
    SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).run { format(this@makeItReadable) }
  }
}

fun Date?.prettifyDate(): String =
  if (this == null) "" else PrettyTime(Locale.getDefault()).formatDuration(this)

fun isSameMonthYear(yearMonthValue1: String, yearMonthValue2: String) =
  listOf(SDF_MMM_YYYY, SDF_YYYY_MMM).let {
    it.tryParse(yearMonthValue1)?.asMmmYyyy()
      ?.equals(it.tryParse(yearMonthValue2)?.asMmmYyyy()) ==
            true
  }

fun Date.daysPassed() =
  TimeUnit.DAYS.convert(Calendar.getInstance().timeInMillis - this.time, TimeUnit.MILLISECONDS)

fun Date.yearsPassed() = this.daysPassed().div(365).toInt()

fun Date.monthsPassed() = this.daysPassed().div(30.5).toInt()

fun Date?.toAgeDisplay() = if (this == null) "" else getAgeStringFromDays(this.daysPassed())

fun DateType.plusWeeksAsString(weeks: Int): String {
  val clone = this.copy()
  clone.add(Calendar.DATE, weeks * 7)
  return clone.format()
}

fun DateType.plusMonthsAsString(months: Int): String {
  val clone = this.copy()
  clone.add(Calendar.MONTH, months)
  return clone.format()
}

fun Date.calendar(): Calendar = Calendar.getInstance().apply { time = this@calendar }

fun Date.plusYears(years: Int): Date {
  val clone = this.calendar()
  clone.add(Calendar.YEAR, years)
  return clone.time
}

fun Date.plusDays(days: Int): Date {
  val clone = this.calendar()
  clone.add(Calendar.DATE, days)
  return clone.time
}

fun Date.plusMonths(months: Int, startOfMonth: Boolean = false): Date {
  val clone = this.calendar()
  clone.add(Calendar.MONTH, months)
  return clone.time.let { if (startOfMonth) it.firstDayOfMonth() else it }
}

fun Date.firstDayOfMonth(): Date {
  val clone = this.calendar()
  clone.set(Calendar.DATE, clone.getActualMinimum(Calendar.DATE))
  return clone.time
}

fun Date.lastDayOfMonth(): Date {
  val clone = this.calendar()
  clone.set(Calendar.DATE, clone.getActualMaximum(Calendar.DATE))
  return clone.time
}

fun DateType.format(): String = SDF_YYYY_MM_DD.format(value)

fun DateTimeType.format(): String =
  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(value).let {
    StringBuilder(it).insert(it.length - 2, ":").toString()
  }

fun DateTimeType.plusDaysAsString(days: Int): String {
  val clone = this.copy()
  clone.add(Calendar.DATE, days)
  return clone.value.asDdMmmYyyy()
}

fun DateTimeType.toDisplay(): String {
  return value?.asDdMmmYyyy() ?: ""
}
