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

package org.smartregister.fhircore.anc.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Goal
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.setPropertySafely

class RegisterUtilsTest : RobolectricTest() {

  @Test
  fun testLoadRegisterConfig() {
    val config =
      ApplicationProvider.getApplicationContext<Application>()
        .loadRegisterConfig(RegisterType.ANC_REGISTER_ID)

    assertEquals(Enumerations.SearchParamType.TOKEN, config.primaryFilter!!.filterType)
    assertEquals(Enumerations.DataType.CODEABLECONCEPT, config.primaryFilter!!.valueType)
    assertEquals("code", config.primaryFilter!!.key)
    assertEquals("LA15173-0", config.primaryFilter!!.valueCoding!!.code)

    assertEquals(Enumerations.SearchParamType.TOKEN, config.secondaryFilter!!.filterType)
    assertEquals(Enumerations.DataType.CODEABLECONCEPT, config.secondaryFilter!!.valueType)
    assertEquals("clinical-status", config.secondaryFilter!!.key)
    assertEquals("active", config.secondaryFilter!!.valueCoding!!.code)
  }

  @Test
  fun testFamilyRegistrationStructureMapExtraction() {
    val formResourcesDir = "questionnaires/family-registration"
    val structureMap = "$formResourcesDir/structure-map.txt".readFile()
    val response = "$formResourcesDir/questionnaire-response.json".readFile()
    val scu = buildStructureMapUtils()

    val targetResource = transform(scu, structureMap, response, "PatientRegistration")

    assertEquals(2, targetResource.entry.size)

    val patient = targetResource.entry[0].resource as Patient
    val samplePatient = "$formResourcesDir/sample/patient.json".parseSampleResource() as Patient

    assertResourceContent(samplePatient, patient)

    val flag = targetResource.entry[1].resource as Flag
    val sampleFlag = "$formResourcesDir/sample/flag.json".parseSampleResource() as Flag

    assertEquals(patient.asReference().reference, flag.subject.reference)
    // replace with inline generated patient id to compare text
    sampleFlag.subject.reference = flag.subject.reference

    assertResourceContent(sampleFlag, flag)
  }

  @Test
  fun testFamilyMemberRegistrationStructureMapExtraction() {
    val formResourcesDir = "questionnaires/family-member-registration"
    val structureMap = "$formResourcesDir/structure-map.txt".readFile()
    val response = "$formResourcesDir/questionnaire-response.json".readFile()
    val scu = buildStructureMapUtils()

    val targetResource = transform(scu, structureMap, response, "PatientRegistration")

    assertEquals(1, targetResource.entry.size)

    val patient = targetResource.entry[0].resource as Patient
    val samplePatient = "$formResourcesDir/sample/patient.json".parseSampleResource() as Patient

    assertResourceContent(samplePatient, patient)
  }

  @Test
  fun testAncServiceEnrollmentStructureMapExtraction() {
    val formResourcesDir = "questionnaires/anc-service-enrollment"
    val structureMap = "$formResourcesDir/structure-map.txt".readFile()
    val response = "$formResourcesDir/questionnaire-response.json".readFile()
    val scu = buildStructureMapUtils()

    val targetResource = transform(scu, structureMap, response, "ANCServiceEnrollment")

    assertEquals(9, targetResource.entry.size)

    val goal = targetResource.entry[0].resource as Goal
    val sampleGoal = "$formResourcesDir/sample/goal.json".parseSampleResource() as Goal

    assertResourceContent(sampleGoal, goal)

    val condition = targetResource.entry[1].resource as Condition
    val sampleCondition =
      "$formResourcesDir/sample/condition.json".parseSampleResource() as Condition

    assertResourceContent(sampleCondition, condition)

    val episode = targetResource.entry[2].resource as EpisodeOfCare
    val sampleEpisode =
      "$formResourcesDir/sample/episode.json".parseSampleResource() as EpisodeOfCare

    assertEquals(condition.asReference().reference, episode.diagnosisFirstRep.condition.reference)
    // replace with inline generated condition id to compare text
    sampleEpisode.diagnosisFirstRep.condition = episode.diagnosisFirstRep.condition

    assertResourceContent(sampleEpisode, episode)

    val encounter = targetResource.entry[3].resource as Encounter
    val sampleEncounter =
      "$formResourcesDir/sample/encounter.json".parseSampleResource() as Encounter

    assertEquals(condition.asReference().reference, encounter.diagnosisFirstRep.condition.reference)
    assertEquals(episode.asReference().reference, encounter.episodeOfCareFirstRep.reference)
    // replace with inline generated condition, episode id to compare text
    sampleEncounter.diagnosisFirstRep.condition = encounter.diagnosisFirstRep.condition
    sampleEncounter.episodeOfCare = encounter.episodeOfCare

    assertResourceContent(sampleEncounter, encounter)

    val lmp = targetResource.entry[4].resource as Observation
    val sampleLmp = "$formResourcesDir/sample/obs_lmp.json".parseSampleResource() as Observation

    assertEquals(encounter.asReference().reference, lmp.encounter.reference)
    // replace with inline generated encounter id to compare text
    sampleLmp.encounter = lmp.encounter

    assertResourceContent(sampleLmp, lmp)

    val edd = targetResource.entry[5].resource as Observation
    val sampleEdd = "$formResourcesDir/sample/obs_edd.json".parseSampleResource() as Observation

    assertEquals(encounter.asReference().reference, edd.encounter.reference)
    // replace with inline generated encounter id to compare text
    sampleEdd.encounter = edd.encounter

    assertResourceContent(sampleEdd, edd)

    val gravida = targetResource.entry[6].resource as Observation
    val sampleGravida =
      "$formResourcesDir/sample/obs_gravida.json".parseSampleResource() as Observation

    assertEquals(encounter.asReference().reference, gravida.encounter.reference)
    // replace with inline generated encounter id to compare text
    sampleGravida.encounter = gravida.encounter

    assertResourceContent(sampleGravida, gravida)

    val liveDel = targetResource.entry[7].resource as Observation
    val sampleLiveDel =
      "$formResourcesDir/sample/obs_live_del.json".parseSampleResource() as Observation

    assertEquals(encounter.asReference().reference, liveDel.encounter.reference)
    // replace with inline generated encounter id to compare text
    sampleLiveDel.encounter = liveDel.encounter

    assertResourceContent(sampleLiveDel, liveDel)

    val careplan = targetResource.entry[8].resource as CarePlan
    val sampleCareplan = "$formResourcesDir/sample/careplan.json".parseSampleResource() as CarePlan

    assertEquals(goal.asReference().reference, careplan.goalFirstRep.reference)
    assertEquals(condition.asReference().reference, careplan.addressesFirstRep.reference)
    assertEquals(
      episode.asReference().reference,
      careplan.extension[0].value.let { it.castToReference(it) }.reference
    )
    assertEquals(
      encounter.asReference().reference,
      careplan.activityFirstRep.outcomeReference.first().reference
    )
    // replace with inline generated goal, condition, episode, encounter id to compare text
    sampleCareplan.goal[0] = careplan.goal[0]
    sampleCareplan.addresses[0] = careplan.addresses[0]
    sampleCareplan.extension[0] = careplan.extension[0]
    sampleCareplan.activityFirstRep.outcomeReferenceFirstRep.reference =
      careplan.activityFirstRep.outcomeReferenceFirstRep.reference

    assertResourceContent(sampleCareplan, careplan)
  }

  fun assertResourceContent(expected: Resource, actual: Resource) {
    // replace properties generating dynamically
    expected.setPropertySafely("id", actual.idElement)

    val expectedStr = expected.convertToString(true)
    val actualStr = actual.convertToString(true)

    System.out.println(expectedStr)
    System.out.println(actualStr)

    assertEquals(expectedStr, actualStr)
  }
}
