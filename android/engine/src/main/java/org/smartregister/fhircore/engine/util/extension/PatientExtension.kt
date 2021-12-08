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
import org.hl7.fhir.r4.model.Condition
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.smartregister.fhircore.engine.R

private const val RISK = "risk"
const val DAYS_IN_YEAR = 365
const val DAYS_IN_MONTH = 30
const val DAYS_IN_WEEK = 7

fun Patient.extractName(): String {
  if (!hasName()) return ""
  val humanName = this.name.firstOrNull()
  return if (humanName != null) {
    "${
    humanName.given.joinToString(" ")
    { it.toString().trim().toTitleCase() }
    } ${humanName.family?.toTitleCase() ?: ""}"
  } else ""
}

fun Patient.extractFamilyName(): String {
  if (!hasName()) return ""
  val humanName = this.name.firstOrNull()
  return if (humanName != null) {
    humanName.family?.toTitleCase()?.plus(" Family") ?: ""
  } else ""
}

private fun String.toTitleCase() = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

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
  val ageDiffMilli = Instant.now().toEpochMilli() - this.birthDate.time
  return getAgeStringFromDays(TimeUnit.DAYS.convert(ageDiffMilli, TimeUnit.MILLISECONDS))
}

fun getAgeStringFromDays(days: Long): String {
  var elapseYearsString = ""
  var elapseMonthsString = ""
  var elapseWeeksString = ""
  var elapseDaysString = ""
  val elapsedYears = days / DAYS_IN_YEAR
  val diffDaysFromYear = days % DAYS_IN_YEAR
  val elapsedMonths = diffDaysFromYear / DAYS_IN_MONTH
  val diffDaysFromMonth = diffDaysFromYear % DAYS_IN_MONTH
  val elapsedWeeks = diffDaysFromMonth / DAYS_IN_WEEK
  val elapsedDays = diffDaysFromMonth % DAYS_IN_WEEK
  if (elapsedYears > 0) elapseYearsString = elapsedYears.toString() + "y"
  if (elapsedMonths > 0) elapseMonthsString = elapsedMonths.toString() + "m"
  if (elapsedWeeks > 0) elapseWeeksString = elapsedWeeks.toString() + "w"
  if (elapsedDays >= 0) elapseDaysString = elapsedDays.toString() + "d"

  return if (days >= DAYS_IN_YEAR * 10) {
    elapseYearsString
  } else if (days >= DAYS_IN_YEAR) {
    if (elapsedMonths > 0) {
      elapseYearsString + " " + elapseMonthsString
    } else elapseYearsString
  } else if (days >= DAYS_IN_MONTH) {
    if (elapsedWeeks > 0) {
      elapseMonthsString + " " + elapseWeeksString
    } else elapseMonthsString
  } else if (days >= DAYS_IN_WEEK) {
    if (elapsedDays > 0) {
      elapseWeeksString + " " + elapseDaysString
    } else elapseWeeksString
  } else elapseDaysString
}

fun Patient.atRisk() =
  this.extension.singleOrNull { it.value.toString().contains(RISK) }?.value?.toString() ?: ""

fun Patient.getLastSeen(immunizations: List<Immunization>): String {
  return immunizations
    .maxByOrNull { it.protocolAppliedFirstRep.doseNumberPositiveIntType.value }
    ?.occurrenceDateTimeType
    ?.toDisplay()
    ?: this.meta?.lastUpdated.lastSeenFormat()
}

private fun Date?.lastSeenFormat(): String {
  return if (this != null) {
    SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH).run { format(this@lastSeenFormat) }
  } else ""
}

fun Patient.extractAddress(): String {
  if (!hasAddress()) return ""
  return with(addressFirstRep) {
    val addressLine =
      if (this.hasLine()) this.line.joinToString(separator = ", ", postfix = ", ") else ""

    addressLine
      .join(this.district, " ")
      .join(this.city, " ")
      .join(this.state, " ")
      .join(this.country, " ")
      .trim()
  }
}

fun Patient.extractDeathDate() = if (this.hasDeceasedDateTimeType()) deceasedDateTimeType?.value else null

fun String?.join(other: String?, separator: String) =
  this.orEmpty().plus(other?.plus(separator).orEmpty())

fun Patient.extractFamilyTag() = this.meta.tag.singleOrNull { it.display.contentEquals("family", true) || it.display.contains("head", true) }

fun Patient.isFamilyHead() = this.extractFamilyTag() != null

fun List<Condition>.hasActivePregnancy() =
  this.any {
    it.clinicalStatus.coding.any { it.code == "active" }
    it.code.text.contentEquals("pregnant", true)
    it.code.coding.any { it.display.contentEquals("pregnant", true) }
  }