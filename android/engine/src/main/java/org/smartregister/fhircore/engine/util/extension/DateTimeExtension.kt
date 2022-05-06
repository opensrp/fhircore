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

val SDF_DD_MMM_YYYY = SimpleDateFormat("dd-MMM-yyyy")
val SDF_YYYY_MM_DD = SimpleDateFormat("yyyy-MM-dd")

fun OffsetDateTime.asString(): String {
  return this.format(DateTimeFormatter.RFC_1123_DATE_TIME)
}

fun Date.asDdMmmYyyy(): String {
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

fun Date.daysPassed() =
  TimeUnit.DAYS.convert(Calendar.getInstance().timeInMillis - this.time, TimeUnit.MILLISECONDS)

fun Date.yearsPassed() = this.daysPassed().div(365).toInt()

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

fun Date.plusYears(years: Int): Date {
  val date = this
  val clone = Calendar.getInstance().apply { time = date }
  clone.add(Calendar.YEAR, years)
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
