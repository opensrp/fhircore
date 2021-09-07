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

import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.smartregister.fhircore.engine.sdk.PatientExtended
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.parser

fun Patient.extractName(): String {
  if (!hasName()) return ""
  val humanName = this.name.firstOrNull()
  return if (humanName != null) {
    "${humanName.given.joinToString(" ")
    { it.toString().trim().toTitleCase() }} ${humanName.family?.toTitleCase() ?: ""}"
  } else ""
}

private fun String.toTitleCase() = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

fun Patient.extractGender() =
  when (AdministrativeGender.valueOf(this.gender.name)) {
    AdministrativeGender.MALE -> "M"
    AdministrativeGender.FEMALE -> "F"
    AdministrativeGender.OTHER -> "O"
    AdministrativeGender.UNKNOWN -> "U"
    AdministrativeGender.NULL -> ""
  }

fun Patient.extractAge(): String {
  if (!hasBirthDate()) return ""
  val ageDiffMilli = Instant.now().toEpochMilli() - this.birthDate.time
  return (TimeUnit.DAYS.convert(ageDiffMilli, TimeUnit.MILLISECONDS) / 365).toString()
}

fun Patient.extractAddress(): String {
  if (!hasAddress()) return ""
  return with(addressFirstRep) { "${district ?: ""} $city" }
}

fun Patient.atRisk(riskCode: String) =
  this.extension.singleOrNull { it.value.toString().contains(riskCode) }?.value?.toString() ?: ""

fun Patient.isPregnant() = this.extension.any { it.value.toString().contains("pregnant", true) }

fun Patient.extractExtendedPatient(): PatientExtended {
  val patientEncoded = parser.encodeResourceToString(this)
  return parser.parseResource(PatientExtended::class.java, patientEncoded)
}
