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

package org.smartregister.fhircore.quest

import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.*
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.setPropertySafely
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class RegisterContentTest : RobolectricTest() {

  @Test
  fun testG6pdTestResultsExtraction() {
    val structureMap = "test-results-questionnaire/structure-map.txt".readFile()
    val response = "test-results-questionnaire/questionnaire-response.json".readFile()

    val scu = buildStructureMapUtils()
    val targetResource = transform(scu, structureMap, response, "TestResults")

    Assert.assertEquals(4, targetResource.entry.size)

    val encounter = targetResource.entry[0].resource as Encounter
    val sampleEncounter =
      "test-results-questionnaire/sample/enc.json".parseSampleResourceFromFile() as Encounter

    assertResourceContent(sampleEncounter, encounter)

    val tType = targetResource.entry[1].resource as Observation
    val sampleTType =
      "test-results-questionnaire/sample/obs_res_type.json".parseSampleResourceFromFile() as
        Observation

    assertResourceContent(tType, sampleTType)

    val g6pd = targetResource.entry[2].resource as Observation
    val sampleG6pd =
      "test-results-questionnaire/sample/obs_g6pd.json".parseSampleResourceFromFile() as Observation

    assertResourceContent(g6pd, sampleG6pd)

    val hb = targetResource.entry[3].resource as Observation
    val sampleHb =
      "test-results-questionnaire/sample/obs_hb.json".parseSampleResourceFromFile() as Observation

    assertResourceContent(hb, sampleHb)
  }

  fun assertResourceContent(expected: Resource, actual: Resource) {
    // replace properties generating dynamically
    actual.setPropertySafely("id", expected.idElement)

    if (expected.resourceType == ResourceType.Observation)
      actual.setPropertySafely("encounter", expected.getNamedProperty("encounter").values[0])

    val expectedStr = expected.convertToString(true)
    val actualStr = actual.convertToString(true)

    System.out.println(expectedStr)
    System.out.println(actualStr)

    Assert.assertEquals(expectedStr, actualStr)
  }

  @Test
  fun testG6pdPatientRegistrationExtraction() {
    val structureMap = "patient-registration-questionnaire/structure-map.txt".readFile()
    val response = "patient-registration-questionnaire/questionnaire-response.json".readFile()

    val scu = buildStructureMapUtils()
    val targetResource = transform(scu, structureMap, response, "PatientRegistration")

    Assert.assertEquals(2, targetResource.entry.size)

    val patient = targetResource.entry[0].resource as Patient
    val samplePatient =
      "patient-registration-questionnaire/sample/patient.json".parseSampleResourceFromFile() as
        Patient

    assertResourceContent(patient, samplePatient)

    val condition = targetResource.entry[1].resource as Condition
    val sampleCondition =
      "patient-registration-questionnaire/sample/condition.json".parseSampleResourceFromFile() as
        Condition
    // replace subject as registration forms generate uuid on the fly
    sampleCondition.subject = condition.subject

    assertResourceContent(condition, sampleCondition)
  }

  @Test
  fun testMwCorePopulationResources() = runTest{
    val questionnaire = "mwcore-registration/questionnaire.json".readFile().decodeResourceFromString<Questionnaire>()
    //Patient().apply { addTags(listOf(Coding("https://www.d-tree.org", "p-category", "P Category"))) }
    val patient = Patient().apply {
      val codingList = arrayListOf<Coding>()
      codingList.add(Coding("https://www.d-tree.org", "p-category", "hello"))
      meta.tag.apply { addAll(codingList) }
    }
    val result = ResourceMapper.populate(questionnaire, patient)
    Assert.assertEquals("p-category", result.item.first().itemFirstRep.answerFirstRep.valueCoding.code)
  }

  @Test
  fun testMwCorePopulationResources2() = runTest{

    val structureMap = "mwcore-registration/patient-edit-profile-structure-map.txt".readFile()
    val response = "mwcore-registration/questionnaire-resposne.json".readFile()

    val scu = buildStructureMapUtils()
    val targetResource = transform(scu, structureMap, response, "TestResults")

    Assert.assertEquals(2, targetResource.entry.size)

  }

}
