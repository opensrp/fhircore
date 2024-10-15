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

package org.smartregister.fhircore.engine.util.extension

import android.content.Context
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.ocpsoft.prettytime.PrettyTime
import org.smartregister.fhircore.engine.R
import timber.log.Timber

const val SDF_DD_MMM_YYYY = "dd-MMM-yyyy"
const val SDF_YYYY_MM_DD = "yyyy-MM-dd"
const val SDF_MMM_YYYY = "MMM-yyyy"
const val SDF_YYYY_MMM = "yyyy-MMM"
const val SDF_MMMM = "MMMM"
const val SDF_YYYY = "yyyy"
const val SDF_D_MMM_YYYY_WITH_COMA = "d MMM, yyyy"
const val SDFHH_MM = "HH:mm"
const val SDF_E_MMM_DD_YYYY = "E, MMM dd yyyy"
const val DEFAULT_FORMAT_SDF_DD_MM_YYYY = "EEE, MMM dd - hh:mm a"
const val SDF_YYYY_MMM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss"
const val MMM_D_HH_MM_AA = "MMM d, hh:mm aa"

fun yesterday(): Date = DateTimeType.now().apply { add(Calendar.DATE, -1) }.value

fun today(): Date = DateTimeType.today().value

fun Date.formatDate(pattern: String = "dd-MMM-yyyy"): String =
  SimpleDateFormat(pattern, Locale.ENGLISH).format(this)

fun Date.isToday() = this.formatDate(SDF_YYYY_MM_DD) == today().formatDate(SDF_YYYY_MM_DD)

fun SimpleDateFormat.tryParse(date: String): Date? =
  kotlin.runCatching { parse(date) }.getOrNull()
    ?: kotlin
      .runCatching { SimpleDateFormat(this.toPattern(), Locale.ENGLISH).parse(date) }
      .getOrNull()

fun Date?.makeItReadable(pattern: String = "dd-MMM-yyyy"): String {
  return if (this == null) {
    "N/A"
  } else {
    SimpleDateFormat(pattern, Locale.getDefault()).run { format(this@makeItReadable) }
  }
}

fun Date?.prettifyDate(): String =
  if (this == null) "" else PrettyTime(Locale.getDefault()).formatDuration(this)

fun Date.daysPassed() =
  TimeUnit.DAYS.convert(Calendar.getInstance().timeInMillis - this.time, TimeUnit.MILLISECONDS)

fun Date.yearsPassed() = this.daysPassed().div(365).toInt()

fun Date.monthsPassed() = this.daysPassed().div(30.5).toInt()

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
  val clone = calendar()
  clone.add(Calendar.YEAR, years)
  return clone.time
}

fun Date.plusDays(days: Int): Date {
  val clone = calendar()
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

fun DateType.format(): String = value.formatDate(SDF_YYYY_MM_DD)

/**
 * This function calculates the age from [date] then translates the abbreviation for the the
 * periods. If year is > 0 display the age in years, if year is 0 then display age in month and
 * weeks, if month is 0 display age in weeks and days otherwise if week is 0 display age in days.
 */
fun calculateAge(date: Date, context: Context, localDateNow: LocalDate = LocalDate.now()): String {
  val theDate: LocalDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
  val period = Period.between(theDate, localDateNow)
  val years = period.years
  val months = period.months
  val weeks = period.days / 7
  val days = period.days % 7

  return when {
    years in 1..4 ->
      context.abbreviateString(R.string.year, years) +
        context.abbreviateString(R.string.month, months)
    years >= 5 -> context.abbreviateString(R.string.year, years)
    months > 0 ->
      context.abbreviateString(R.string.month, months) +
        context.abbreviateString(R.string.weeks, weeks)
    weeks > 0 ->
      context.abbreviateString(R.string.weeks, weeks) +
        context.abbreviateString(R.string.days, days)
    else -> "$days${context.getString(R.string.days).lowercase().abbreviate()} "
  }.trim()
}

private fun Context.abbreviateString(resourceId: Int, content: Int) =
  if (content > 0) "$content${this.getString(resourceId).lowercase().abbreviate()} " else ""

fun formatDate(timeMillis: Long, desireFormat: String): String {
  return try {
    // Try formatting with the provided format
    val format = SimpleDateFormat(desireFormat, Locale.getDefault())
    val date = Date(timeMillis)
    format.format(date)
  } catch (e: Exception) {
    // If formatting fails, fall back to the default format
    val defaultFormat = SimpleDateFormat(DEFAULT_FORMAT_SDF_DD_MM_YYYY, Locale.getDefault())
    val date = Date(timeMillis)
    defaultFormat.format(date)
  }
}

/**
 * Reformats a given date string from its current format to a specified desired format. If the date
 * string cannot be parsed in the provided current format, the method will return the original date
 * string as a fallback.
 *
 * @param inputDateString The date string that needs to be reformatted.
 * @param currentFormat The format in which the input date string is provided (e.g., "dd-MM-yyyy").
 * @param desiredFormat The format in which the output date string should be returned (default:
 *   "yyyy-MM-dd HH:mm:ss"). If no desired format is specified, it defaults to "yyyy-MM-dd
 *   HH:mm:ss".
 * @return A string representing the date in the desired format if parsing succeeds, or the original
 *   input date string if parsing fails.
 *
 * Example usage:
 * ```
 * val reformattedDate = reformatDate("08-10-2024 15:30", "dd-MM-yyyy HH:mm", "yyyy-MM-dd HH:mm:ss")
 * println(reformattedDate) // Output: "2024-10-08 15:30:00"
 *
 * val invalidDate = reformatDate("InvalidDate", "dd-MM-yyyy", "yyyy-MM-dd")
 * println(invalidDate) // Output: "InvalidDate"
 * ```
 */
fun reformatDate(
  inputDateString: String,
  currentFormat: String,
  desiredFormat: String,
): String {
  return try {
    // Create a SimpleDateFormat with the current format of the input date
    val inputDateFormat = SimpleDateFormat(currentFormat, Locale.getDefault())
    // Parse the input date string into a Date object
    val date = inputDateFormat.parse(inputDateString)
    // Create a SimpleDateFormat for the desired format
    val outputDateFormat = SimpleDateFormat(desiredFormat, Locale.getDefault())
    // Format the date into the desired format and return the result
    outputDateFormat.format(date ?: Date())
  } catch (e: Exception) {
    // In case of any exception, return the original input date string
    Timber.e(e)
    inputDateString
  }
}
