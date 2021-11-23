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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType

val SDF_DD_MMM_YYYY = SimpleDateFormat("dd-MMM-yyyy")

fun OffsetDateTime.asString(): String {
  return this.format(DateTimeFormatter.RFC_1123_DATE_TIME)
}

fun Date.asDdMmmYyyy(): String {
  return SDF_DD_MMM_YYYY.format(this)
}

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

fun DateType.format(): String =
  DateTimeFormatter.ISO_LOCAL_DATE.format(
    this.dateTimeValue().value.toInstant().atOffset(ZoneOffset.UTC)
  )

fun DateTimeType.plusDaysAsString(days: Int): String {
  val clone = this.copy()
  clone.add(Calendar.DATE, days)
  return clone.value.asDdMmmYyyy()
}

fun DateTimeType.toDisplay(): String {
  return value?.asDdMmmYyyy() ?: ""
}
