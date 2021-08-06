package org.smartregister.fhircore.util

import android.content.Context
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.R
import org.smartregister.fhircore.domain.Gender

fun Patient.extractName(): String {
  if (!hasName()) return ""
  val humanName = this.name.firstOrNull()
  return if (humanName != null)
    "${humanName.given.joinToString(" ") { it.toString().trim() }} ${humanName.family ?: ""}"
  else ""
}

fun Patient.extractGender(context: Context) =
  when (Gender.valueOf(this.gender.name)) {
    Gender.MALE -> context.getString(R.string.male)
    Gender.FEMALE -> context.getString(R.string.female)
    Gender.OTHER -> context.getString(R.string.other)
    Gender.UNKNOWN -> context.getString(R.string.unknown)
  }

fun Patient.extractAge(): String {
  if (!hasBirthDate()) return ""
  val ageDiffMilli = Instant.now().toEpochMilli() - this.birthDate.time
  return (TimeUnit.DAYS.convert(ageDiffMilli, TimeUnit.MILLISECONDS) / 365).toString()
}
