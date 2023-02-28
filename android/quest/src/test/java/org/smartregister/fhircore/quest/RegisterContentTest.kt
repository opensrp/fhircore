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

package org.smartregister.fhircore.quest

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.setPropertySafely
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class RegisterContentTest : RobolectricTest() {

  val context = ApplicationProvider.getApplicationContext<Context>()
  val worker =
    SimpleWorkerContext().apply {
      this.setExpansionProfile(Parameters())
      this.isCanRunWithoutTerminology = true
    }
  val transformSupportServices = TransformSupportServices(worker)

  fun buildStructureMapExtractionContext(
    structureMapString: String,
    sourceGroup: String
  ): StructureMapExtractionContext {
    return StructureMapExtractionContext(
      context = context,
      transformSupportServices = transformSupportServices,
      structureMapProvider = { structureMapUrl: String, _: IWorkerContext ->
        StructureMapUtilities(worker, transformSupportServices)
          .parse(structureMapString, sourceGroup)
      }
    )
  }

  @Test
  fun testG6pdTestResultsExtraction() = runTest {
    val structureMap = "test-results-questionnaire/structure-map.txt".readFile()
    val response = "test-results-questionnaire/questionnaire-response.json".readFile()
    val questionnaire = "test-results-questionnaire/questionnaire.json".readFile()

    val targetResource =
      ResourceMapper.extract(
          questionnaire.decodeResourceFromString(),
          response.decodeResourceFromString(),
          buildStructureMapExtractionContext(structureMap, "TestResults")
        )
        .also { println(it.encodeResourceToString()) }

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
  fun testG6pdPatientRegistrationExtraction() = runTest {
    val structureMap = "patient-registration-questionnaire/structure-map.txt".readFile()
    val response = "patient-registration-questionnaire/questionnaire-response.json".readFile()
    val questionnaire = "patient-registration-questionnaire/questionnaire.json".readFile()

    val targetResource =
      ResourceMapper.extract(
          questionnaire.decodeResourceFromString(),
          response.decodeResourceFromString(),
          buildStructureMapExtractionContext(structureMap, "PatientRegistration")
        )
        .also { println(it.encodeResourceToString()) }

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
}
