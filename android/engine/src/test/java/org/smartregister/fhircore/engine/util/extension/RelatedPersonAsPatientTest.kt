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
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.junit.Assert
import org.junit.Test

class RelatedPersonAsPatientTest {

  @Test
  fun guardianPatientReference_readsIdentifierPatientUrl() {
    val rp =
      RelatedPerson().apply {
        patient = Reference("Patient/child-1")
        addRelationship(
          CodeableConcept()
            .addCoding(
              Coding(
                "http://terminology.hl7.org/CodeSystem/v3-RoleCode",
                "MTH",
                "mother",
              ),
            ),
        )
        addIdentifier(patientUrlIdentifier("Patient/mother-1"))
      }

    val linkId = rp.identifierFirstRep
    Assert.assertEquals(Identifier.IdentifierUse.SECONDARY, linkId.use)
    Assert.assertEquals(IDENTIFIER_TYPE_PI, linkId.type.codingFirstRep.code)
    Assert.assertEquals(RELATED_PERSON_PATIENT_IDENTIFIER_SYSTEM, linkId.system)
    Assert.assertEquals("Patient/mother-1", rp.guardianPatientReference())
    Assert.assertTrue(rp.isDependentOfGuardian("mother-1"))
    Assert.assertFalse(rp.isDependentOfGuardian("other"))
    Assert.assertEquals("child-1", rp.childPatientId())
  }

  @Test
  fun guardianPatientReference_prefersPiTypedIdentifier() {
    val rp =
      RelatedPerson().apply {
        patient = Reference("Patient/child-1")
        // National ID (not a Patient URL) — should not win over typed PI link
        addIdentifier(
          Identifier().apply {
            use = Identifier.IdentifierUse.OFFICIAL
            system = "http://example.org/national-id"
            value = "123456789"
          },
        )
        addIdentifier(patientUrlIdentifier("Patient/mother-1"))
      }

    Assert.assertEquals("Patient/mother-1", rp.guardianPatientReference())
  }

  @Test
  fun guardianPatientReference_acceptsAbsolutePatientUrl() {
    val rp =
      RelatedPerson().apply {
        patient = Reference("Patient/child-1")
        addIdentifier(
          Identifier().apply {
            use = Identifier.IdentifierUse.SECONDARY
            type =
              CodeableConcept()
                .addCoding(
                  Coding(
                    IDENTIFIER_TYPE_SYSTEM_V2_0203,
                    IDENTIFIER_TYPE_PT,
                    "Patient external identifier"
                  ),
                )
            system = RELATED_PERSON_PATIENT_IDENTIFIER_SYSTEM
            value = "https://fhir.example.org/Patient/mother-1"
          },
        )
      }

    Assert.assertEquals("Patient/mother-1", rp.guardianPatientReference())
  }

  @Test
  fun groupByGuardianPatientId_groupsRelatedPersons() {
    val rp1 =
      RelatedPerson().apply {
        id = "rp1"
        patient = Reference("Patient/child-a")
        addIdentifier(patientUrlIdentifier("Patient/parent-1"))
      }
    val rp2 =
      RelatedPerson().apply {
        id = "rp2"
        patient = Reference("Patient/child-b")
        addIdentifier(patientUrlIdentifier("Patient/parent-1"))
      }
    val rpOther =
      RelatedPerson().apply {
        id = "rp3"
        patient = Reference("Patient/child-c")
        addIdentifier(patientUrlIdentifier("https://example.org/fhir/Patient/parent-2"))
      }

    val grouped = listOf(rp1, rp2, rpOther).groupByGuardianPatientId()
    Assert.assertEquals(2, grouped.size)
    Assert.assertEquals(2, grouped["parent-1"]?.size)
    Assert.assertEquals(1, grouped["parent-2"]?.size)
  }

  @Test
  fun patientReferenceFromIdentifierValue_parsesVariants() {
    Assert.assertEquals(
      "Patient/marie",
      "Patient/marie".patientReferenceFromIdentifierValue(),
    )
    Assert.assertEquals(
      "Patient/marie",
      "https://fhir.example.org/Patient/marie".patientReferenceFromIdentifierValue(),
    )
    Assert.assertEquals(
      "Patient/marie",
      "https://fhir.example.org/Patient/marie/_history/2".patientReferenceFromIdentifierValue(),
    )
    Assert.assertNull("not-a-patient".patientReferenceFromIdentifierValue())
  }

  @Test
  fun defaultAgeFilter_matchesRole() {
    Assert.assertEquals(RelatedPersonAgeFilter.UNDER_18, RelatedPersonRole.CHILD.defaultAgeFilter())
    Assert.assertEquals(
      RelatedPersonAgeFilter.AGE_18_OR_OVER,
      RelatedPersonRole.GUARDIAN.defaultAgeFilter(),
    )
  }

  @Test
  fun inferGuardianRelationship_usesGender() {
    val mother = Patient().apply { gender = Enumerations.AdministrativeGender.FEMALE }
    val father = Patient().apply { gender = Enumerations.AdministrativeGender.MALE }
    Assert.assertEquals(RELATIONSHIP_MOTHER, inferGuardianRelationship(mother).code)
    Assert.assertEquals(RELATIONSHIP_FATHER, inferGuardianRelationship(father).code)
    Assert.assertEquals(RELATIONSHIP_GUARDIAN, inferGuardianRelationship(Patient()).code)
  }

  @Test
  fun buildRelatedPersonLink_copiesGuardianDemographics() {
    val child = Patient().apply { id = "child-1" }
    val mother =
      Patient().apply {
        id = "mother-1"
        gender = Enumerations.AdministrativeGender.FEMALE
        addName(
          org.hl7.fhir.r4.model.HumanName().apply {
            family = "Doe"
            addGiven("Marie")
          },
        )
      }

    val rp = buildRelatedPersonLink(child, mother)
    Assert.assertEquals("child-1", rp.childPatientId())
    Assert.assertEquals("Patient/mother-1", rp.guardianPatientReference())
    Assert.assertEquals(RELATIONSHIP_MOTHER, rp.relationshipFirstRep.codingFirstRep.code)
    Assert.assertFalse(rp.isPrimaryCaregiver())
    Assert.assertEquals("Marie Doe", rp.nameFirstRep.nameAsSingleString)
    Assert.assertEquals(Enumerations.AdministrativeGender.FEMALE, rp.gender)
  }

  @Test
  fun buildRelatedPersonLink_setsPrimaryCaregiverExtension() {
    val child = Patient().apply { id = "child-1" }
    val mother = Patient().apply { id = "mother-1" }
    val rp =
      buildRelatedPersonLink(
        child = child,
        guardian = mother,
        relationship = RelatedPersonKinship.MOTHER.toCoding(),
        isPrimaryCaregiver = true,
      )
    Assert.assertTrue(rp.isPrimaryCaregiver())
    Assert.assertEquals(RELATIONSHIP_MOTHER, rp.relationshipFirstRep.codingFirstRep.code)
    Assert.assertEquals(1, rp.relationship.size)

    rp.setPrimaryCaregiver(false)
    Assert.assertFalse(rp.isPrimaryCaregiver())
  }
}
