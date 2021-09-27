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

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.hl7.fhir.r4.model.DateTimeType
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object DateUtils {

  fun addDays(initialDate: String, daysToAdd: Int = 0, returnDateFormat: String = "M-d-Y"): String {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern(returnDateFormat)
    val date: DateTime = DateTime.parse(initialDate)
    return date.plusDays(daysToAdd).toString(fmt)
  }

  fun hasPastDays(initialDate: DateTimeType, days: Int = 0): Boolean {
    val copy = initialDate.copy()
    copy.add(Calendar.DATE, days)
    return copy.after(DateTimeType.now())
  }

  fun simpleDateFormat(pattern: String = "hh:mm aa, MMM d") =
    SimpleDateFormat(pattern, Locale.getDefault())
}
