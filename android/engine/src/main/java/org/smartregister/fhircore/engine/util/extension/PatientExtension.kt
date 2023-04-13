/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.smartregister.fhircore.engine.R

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

fun Patient.extractAge(context: Context): String {
  if (!hasBirthDate()) return ""
  return calculateAge(birthDate, context)
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
