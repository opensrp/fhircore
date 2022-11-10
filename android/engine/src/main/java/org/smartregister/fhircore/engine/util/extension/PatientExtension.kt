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

import android.content.Context
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.smartregister.fhircore.engine.R

private const val RISK = "risk"
const val DAYS_IN_YEAR = 365
const val DAYS_IN_MONTH = 30
const val DAYS_IN_WEEK = 7

fun Patient.extractGender(context: Context): String? =
  if (hasGender()) {
    when (AdministrativeGender.valueOf(this.gender.name)) {
      AdministrativeGender.MALE -> context.getString(R.string.male)
      AdministrativeGender.FEMALE -> context.getString(R.string.female)
      AdministrativeGender.OTHER -> context.getString(R.string.other)
      AdministrativeGender.UNKNOWN -> context.getString(R.string.unknown)
      AdministrativeGender.NULL -> ""
    }
  } else null

fun Patient.extractAge(): String {
  if (!hasBirthDate()) return ""
  return getAgeStringFromDays(birthDate)
}

fun getAgeStringFromDays(date: Date): String {
  var elapseYearsString = ""
  var elapseMonthsString = ""
  var elapseWeeksString = ""
  var elapseDaysString = ""
  val startDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
  val endDate = LocalDate.now()
  val period: Period = Period.between(startDate, endDate)
  val days = ChronoUnit.DAYS.between(startDate, endDate)
  val diffDaysFromYear = days % DAYS_IN_YEAR
  val diffDaysFromMonth = diffDaysFromYear % DAYS_IN_MONTH
  val elapsedYears = period.years
  val elapsedMonths = period.months
  val elapsedWeeks = diffDaysFromMonth / DAYS_IN_WEEK
  val elapsedDays = diffDaysFromMonth % DAYS_IN_WEEK
  // TODO use translatable abbreviations - extract abbr to string resource
  if (elapsedYears > 0) elapseYearsString = elapsedYears.toString() + "y"
  if (elapsedMonths > 0) elapseMonthsString = elapsedMonths.toString() + "m"
  if (elapsedWeeks > 0) elapseWeeksString = elapsedWeeks.toString() + "w"
  if (elapsedDays >= 0) elapseDaysString = elapsedDays.toString() + "d"

  return if (days >= DAYS_IN_YEAR * 10) {
    elapseYearsString
  } else if (days >= DAYS_IN_YEAR) {
    if (elapsedMonths > 0) {
      "$elapseYearsString $elapseMonthsString"
    } else if (elapsedWeeks > 0) {
      "$elapseYearsString $elapseWeeksString"
    } else elapseYearsString
  } else if (days >= DAYS_IN_MONTH) {
    if (elapsedWeeks > 0) {
      "$elapseMonthsString $elapseWeeksString"
    } else elapseMonthsString
  } else if (days >= DAYS_IN_WEEK) {
    if (elapsedDays > 0) {
      "$elapseWeeksString $elapseDaysString"
    } else elapseWeeksString
  } else elapseDaysString
}

fun String?.join(other: String?, separator: String) =
  this.orEmpty().plus(other?.plus(separator).orEmpty())

fun Patient.extractFamilyTag() =
  this.meta.tag.firstOrNull {
    it.display.contentEquals("family", true) || it.display.contains("head", true)
  }

fun Enumerations.AdministrativeGender.translateGender(context: Context) =
  when (this) {
    Enumerations.AdministrativeGender.MALE -> context.getString(R.string.male)
    Enumerations.AdministrativeGender.FEMALE -> context.getString(R.string.female)
    else -> context.getString(R.string.unknown)
  }
