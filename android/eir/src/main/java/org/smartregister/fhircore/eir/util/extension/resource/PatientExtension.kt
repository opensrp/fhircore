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

package org.smartregister.fhircore.eir.util.extension.resource

import android.content.Context
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.smartregister.fhircore.eir.R

fun Patient.extractName(): String {
  if (!hasName()) return ""
  val humanName = this.name.firstOrNull()
  return if (humanName != null)
    "${humanName.given.joinToString(" ") { it.toString().trim() }} ${humanName.family ?: ""}"
  else ""
}

fun Patient.extractGender(context: Context) =
  when (AdministrativeGender.valueOf(this.gender.name)) {
    AdministrativeGender.MALE -> context.getString(R.string.male)
    AdministrativeGender.FEMALE -> context.getString(R.string.female)
    AdministrativeGender.OTHER -> context.getString(R.string.other)
    AdministrativeGender.UNKNOWN -> context.getString(R.string.unknown)
    AdministrativeGender.NULL -> ""
  }

fun Patient.extractAge(): String {
  if (!hasBirthDate()) return ""
  val ageDiffMilli = Instant.now().toEpochMilli() - this.birthDate.time
  return (TimeUnit.DAYS.convert(ageDiffMilli, TimeUnit.MILLISECONDS) / 365).toString()
}
