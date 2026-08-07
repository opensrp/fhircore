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

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource

/**
 * TRICC / flexible client register convention: mother, father and guardian always exist as
 * [org.hl7.fhir.r4.model.Patient] clients. [RelatedPerson] links them to a child:
 * - [RelatedPerson.patient] → child Patient
 * - [RelatedPerson.relationship] → mother | father | guardian (e.g. RoleCode `MTH` / `FTH` /
 *   RoleClass `GUARD`)
 * - [RelatedPerson.identifier] → guardian / mother / father **Patient URL** (who this related
 *   person is as a registered client), preferably typed `PI` and `use=secondary`
 *
 * See `feature/register-tricc.md` and
 * [RelatedPerson.identifier](https://build.fhir.org/relatedperson-definitions.html#RelatedPerson.identifier).
 */

/**
 * Prefer absolute URI identifiers ([FHIR Identifier with URI value](https://hl7.org/fhir/datatypes.html#Identifier)).
 * Value holds a Patient reference or absolute Patient URL, e.g. `Patient/marie` or
 * `https://fhir.example.org/Patient/marie`.
 */
const val RELATED_PERSON_PATIENT_IDENTIFIER_SYSTEM = "urn:ietf:rfc:3986"

/** HL7 Identifier Type Codes (v2-0203) — Patient internal identifier. */
const val IDENTIFIER_TYPE_SYSTEM_V2_0203 = "http://terminology.hl7.org/CodeSystem/v2-0203"

/** Patient internal identifier — preferred type for the linked client Patient URL. */
const val IDENTIFIER_TYPE_PI = "PI"

/**
 * Patient external identifier — accepted as a weaker alternative when authors used `PT` instead of
 * `PI` for the same Patient URL link.
 */
const val IDENTIFIER_TYPE_PT = "PT"

/**
 * Map key used in
 * [org.smartregister.fhircore.engine.domain.model.RepositoryResourceData.relatedResourcesMap] for
 * nested dependent children.
 */
const val DEPENDENT_CHILDREN_RESOURCE_KEY = "dependentChildren"

/**
 * Map key for RelatedPerson rows that define dependent children of the current guardian Patient.
 */
const val DEPENDENT_RELATED_PERSONS_RESOURCE_KEY = "dependentRelatedPersons"

/**
 * Returns the guardian / mother / father Patient reference (`Patient/{id}`) from
 * [RelatedPerson.identifier], or null if none is present.
 *
 * Resolution order:
 * 1. Identifier with type `PI` (or `PT`) and a Patient URL/ref value
 * 2. Identifier with [RELATED_PERSON_PATIENT_IDENTIFIER_SYSTEM] and a Patient URL/ref value
 * 3. Any other identifier whose value looks like a Patient URL or `Patient/{id}`
 */
fun RelatedPerson.guardianPatientReference(): String? {
  val typed =
    identifier
      .firstOrNull {
        it.hasPatientLinkIdentifierType() &&
          it.value.patientReferenceFromIdentifierValue() != null
      }
      ?.value
      ?.patientReferenceFromIdentifierValue()
  if (typed != null) return typed

  val preferredSystem =
    identifier
      .firstOrNull {
        it.system == RELATED_PERSON_PATIENT_IDENTIFIER_SYSTEM &&
          it.value.patientReferenceFromIdentifierValue() != null
      }
      ?.value
      ?.patientReferenceFromIdentifierValue()
  if (preferredSystem != null) return preferredSystem

  return identifier
    .asSequence()
    .mapNotNull { it.value?.patientReferenceFromIdentifierValue() }
    .firstOrNull()
}

/** True when this identifier is typed as patient internal/external id (`PI` / `PT`). */
fun Identifier.hasPatientLinkIdentifierType(): Boolean {
  return type?.coding?.any {
    it.system == IDENTIFIER_TYPE_SYSTEM_V2_0203 &&
      (it.code.equals(IDENTIFIER_TYPE_PI, ignoreCase = true) ||
        it.code.equals(IDENTIFIER_TYPE_PT, ignoreCase = true))
  } == true
}

/** True when this RelatedPerson links [guardianPatientId] as mother/father/guardian of a child. */
fun RelatedPerson.isDependentOfGuardian(guardianPatientId: String): Boolean {
  val expected = "Patient/${guardianPatientId.extractLogicalIdUuid()}"
  val ref = guardianPatientReference()?.extractLogicalIdUuid()?.let { "Patient/$it" }
  return ref != null && ref.equals(expected, ignoreCase = true)
}

/** Logical id of the child Patient referenced by [RelatedPerson.patient], or null. */
fun RelatedPerson.childPatientId(): String? =
  patient?.reference?.extractLogicalIdUuid()?.takeIf { it.isNotBlank() }

/**
 * Builds an [Identifier] that stores the guardian Patient as a URL / relative reference in
 * [Identifier.value].
 *
 * Default shape (recommended):
 * - `type` = v2-0203 **`PI`** (Patient internal identifier)
 * - `use` = **`secondary`** (structural link; leaves room for official/usual national IDs)
 * - `system` = [RELATED_PERSON_PATIENT_IDENTIFIER_SYSTEM]
 * - `value` = `Patient/{id}` or absolute Patient URL
 */
fun patientUrlIdentifier(
  patientIdOrReference: String,
  use: Identifier.IdentifierUse = Identifier.IdentifierUse.SECONDARY,
  typeCode: String = IDENTIFIER_TYPE_PI,
): Identifier {
  val reference =
    patientIdOrReference.patientReferenceFromIdentifierValue()
      ?: "Patient/${patientIdOrReference.extractLogicalIdUuid()}"
  return Identifier().apply {
    this.use = use
    type =
      CodeableConcept()
        .addCoding(
          Coding(IDENTIFIER_TYPE_SYSTEM_V2_0203, typeCode, patientLinkTypeDisplay(typeCode)),
        )
    system = RELATED_PERSON_PATIENT_IDENTIFIER_SYSTEM
    value = reference
  }
}

private fun patientLinkTypeDisplay(typeCode: String): String =
  when (typeCode.uppercase()) {
    IDENTIFIER_TYPE_PI -> "Patient internal identifier"
    IDENTIFIER_TYPE_PT -> "Patient external identifier"
    else -> typeCode
  }

/**
 * Groups [relatedPersons] by guardian Patient logical id using
 * [RelatedPerson.guardianPatientReference].
 */
fun List<RelatedPerson>.groupByGuardianPatientId(): Map<String, List<RelatedPerson>> {
  return mapNotNull { rp ->
      val guardianId =
        rp.guardianPatientReference()?.extractLogicalIdUuid() ?: return@mapNotNull null
      guardianId to rp
    }
    .groupBy({ it.first }, { it.second })
}

/** From a mixed list of [Resource], returns only [RelatedPerson] instances. */
fun List<Resource>.asRelatedPersons(): List<RelatedPerson> = mapNotNull { it as? RelatedPerson }

/**
 * Parses [this] as a Patient reference or absolute Patient URL into normalized `Patient/{id}`.
 *
 * Accepts:
 * - `Patient/marie`
 * - `https://example.org/fhir/Patient/marie`
 * - `https://example.org/fhir/Patient/marie/_history/1`
 */
fun String.patientReferenceFromIdentifierValue(): String? {
  val trimmed = trim()
  if (trimmed.isEmpty()) return null

  val afterPatient =
    when {
      trimmed.contains("Patient/", ignoreCase = true) ->
        trimmed.substringAfter("Patient/", "").substringAfter("patient/", "")
      else -> return null
    }
  val logicalId =
    afterPatient
      .substringBefore("/")
      .substringBefore("?")
      .substringBefore("#")
      .trim()
      .takeIf { it.isNotBlank() }
      ?: return null
  return "Patient/$logicalId"
}
