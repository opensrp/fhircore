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

package org.smartregister.fhircore.anc.ui.anccare

import android.content.Context
import com.google.android.fhir.logicalId
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.local.repository.patient.model.AncItem

object AncItemMapper : DomainMapper<Patient, AncItem> {
  private val simpleDateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH)
  private const val RISK = "risk"

  override fun mapToDomainModel(dto: Patient): AncItem {
    val name = dto.extractName()
    val gender = dto.extractGender(AncApplication.getContext()).first()
    val age = dto.extractAge()
    return AncItem(
      patientIdentifier = dto.logicalId,
      name = name,
      gender = gender.toString(),
      age = age,
      demographics = "$name, $gender, $age",
      atRisk = dto.atRisk()
    )
  }

  override fun mapFromDomainModel(domainModel: AncItem): Patient {
    throw UnsupportedOperationException()
  }

  private fun Date?.makeItReadable() = if (this != null) simpleDateFormat.format(this) else ""

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

  fun Patient.extractGender(context: Context) =
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

  private fun Patient.atRisk() =
    this.extension.singleOrNull { it.value.toString().contains(RISK) }?.value?.toString() ?: ""
}
