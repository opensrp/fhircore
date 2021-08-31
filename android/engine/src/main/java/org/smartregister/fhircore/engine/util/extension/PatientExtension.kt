package org.smartregister.fhircore.engine.util.extension

import android.content.Context
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit

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

fun Patient.atRisk(riskCode: String) =
    this.extension.singleOrNull { it.value.toString().contains(riskCode) }?.value?.toString() ?: ""
