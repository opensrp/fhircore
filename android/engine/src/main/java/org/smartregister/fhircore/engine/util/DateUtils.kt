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

package org.smartregister.fhircore.engine.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.hl7.fhir.r4.model.DateTimeType
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.smartregister.fhircore.engine.util.extension.asYyyyMmDd

object DateUtils {
  fun yesterday(): Date = DateTimeType.now().apply { add(Calendar.DATE, -1) }.value

  fun today(): Date = DateTimeType.today().value

  fun Date.isToday() = this.asYyyyMmDd() == today().asYyyyMmDd()

  fun addDays(
    initialDate: String,
    daysToAdd: Int = 0,
    returnDateFormat: String = "M-d-Y",
    dateTimeFormat: String? = null
  ): String {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern(returnDateFormat)
    val date: DateTime =
      if (dateTimeFormat == null) DateTime.parse(initialDate)
      else DateTime.parse(initialDate, DateTimeFormat.forPattern(dateTimeFormat))
    return date.plusDays(daysToAdd).toString(fmt)
  }

  fun hasPastDays(initialDate: DateTimeType, days: Int = 0): Boolean {
    val copy = initialDate.copy()
    copy.add(Calendar.DATE, days)
    return copy.before(DateTimeType.now())
  }

  fun simpleDateFormat(pattern: String = "hh:mm aa, MMM d") =
    SimpleDateFormat(pattern, Locale.getDefault())

  fun String.getDate(formatNeeded: String): Date {
    val format = SimpleDateFormat(formatNeeded)
    var date = Date()
    try {
      date = format.parse(this)
      println(date)
    } catch (e: ParseException) {
      e.printStackTrace()
    }
    return date
  }
}
