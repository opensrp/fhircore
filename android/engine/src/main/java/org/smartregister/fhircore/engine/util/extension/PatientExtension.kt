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
private val simpleDateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH)

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
  return (TimeUnit.DAYS.convert(ageDiffMilli, TimeUnit.MILLISECONDS) / 365).toString()
}

fun Patient.atRisk() =
  this.extension.singleOrNull { it.value.toString().contains(RISK) }?.value?.toString() ?: ""

fun Patient.getLastSeen(immunizations: List<Immunization>): String {
  return immunizations
    .maxByOrNull { it.protocolApplied.first().doseNumberPositiveIntType.value }
    ?.occurrenceDateTimeType
    ?.toHumanDisplay()
    ?: this.meta?.lastUpdated.makeItReadable()
}

private fun Date?.makeItReadable() = if (this != null) simpleDateFormat.format(this) else ""

fun Patient.extractAddress(): String {
  if (!hasAddress()) return ""
  return with(addressFirstRep) { "${district ?: ""} ${city ?: ""}" }
}

fun Patient.extractHeight(): String {
  // Todo: update patient height attribute here
  if (!hasName()) return ""
  val humanName = this.name.firstOrNull()
  return if (humanName != null) {
    "${
    humanName.given.joinToString(" ")
    { it.toString().trim().toTitleCase() }
    } ${humanName.family?.toTitleCase() ?: ""}"
  } else ""
}

fun Patient.extractWeight(): String {
  // Todo: update patient weight attribute here
  if (!hasName()) return ""
  val humanName = this.name.firstOrNull()
  return if (humanName != null) {
    "${
    humanName.given.joinToString(" ")
    { it.toString().trim().toTitleCase() }
    } ${humanName.family?.toTitleCase() ?: ""}"
  } else ""
}

fun Patient.isPregnant() = this.extension.any { it.value.toString().contains("pregnant", true) }
