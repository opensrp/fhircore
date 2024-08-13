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

package org.smartregister.fhircore.engine

import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import com.google.android.fhir.get
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Date
import javax.inject.Inject
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

@HiltAndroidTest
class FhirExtractionTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  val fhirEngine: FhirEngine = mockk()

  @Inject lateinit var transformSupportServices: TransformSupportServices
  private lateinit var structureMapUtilities: StructureMapUtilities
  private val defaultRepository: DefaultRepository = mockk()

  @Before
  fun setup() {
    hiltRule.inject()
    structureMapUtilities = StructureMapUtilities(transformSupportServices.simpleWorkerContext)
    val workManager = mockk<WorkManager>()
    every { defaultRepository.fhirEngine } returns fhirEngine
    every { workManager.enqueue(any<WorkRequest>()) } returns mockk()
  }

  @Test
  @ExperimentalCoroutinesApi
  fun `record-all-immunization extract should generate immunization and encounter`() = runTest {
    val resources = loadExtractionResources("record-all-immunization")
    val questionnaire = resources.questionnaire
    val patient = resources.patient.apply { birthDate = Date() }
    val questionnaireResponse = resources.questionnaireResponse
    val result =
      ResourceMapper.extract(
          questionnaire = questionnaire,
          questionnaireResponse = questionnaireResponse,
          structureMapExtractionContext =
            StructureMapExtractionContext(
              structureMapProvider = { _, _ -> resources.structureMap },
              transformSupportServices = transformSupportServices,
            ),
        )
        .also { println(it.encodeResourceToString()) }
    val encounter = result.entry.find { it.resource is Encounter }!!.resource as Encounter
    result.entry
      .filter { it.resource is Task }
      .also { taskList ->
        assertTrue(taskList.size == 3)

        questionnaireResponse.item
          .find { it.linkId == "vaccines" }!!
          .answer
          .map { it.value as Reference }
          .forEach { taskReference ->
            val outputTask =
              taskList
                .find {
                  (it.resource as Task).id.extractLogicalIdUuid() == taskReference.extractId()
                }
                ?.resource as? Task
            assertNotNull(outputTask)
            assertTrue(outputTask!!.output.size == 3)
            assertNotNull(
              outputTask.output.find {
                it.castToReference(it.value).reference.startsWith(ResourceType.Immunization.name)
              },
            )
            assertNotNull(
              outputTask.output.find {
                it.castToReference(it.value).reference.startsWith(ResourceType.Encounter.name)
              },
            )
          }

        val firstTask = taskList.first().resource as Task
        assertTrue(
          firstTask.output.first().type.coding.first().code ==
            encounter.type.first().coding.first().code,
        )
        assertTrue(!firstTask.output.first().value.isEmpty)

        val administrationEncounter =
          result.entry.filter { it.resource is Encounter }.last().resource as Encounter
        assertTrue(administrationEncounter.partOf.reference == encounter.id)
      }
    result.entry
      .filter { it.resource is Immunization }
      .map { it.resource as Immunization }
      .also {
        assertEquals(3, it.size)
        assertTrue(it.all { it.encounter.reference == encounter.referenceValue() })
        assertTrue(it.all { it.status == Immunization.ImmunizationStatus.COMPLETED })
        assertTrue(it.all { it.recorded.makeItReadable() == Date().makeItReadable() })
        val bcg = it[0]
        assertEquals(bcg.occurrenceDateTimeType.value.makeItReadable(), "14-Apr-2023")
        assertEquals(bcg.vaccineCode.text, "BCG")
        assertEquals(bcg.vaccineCode.codingFirstRep.code, "42284007")
        assertEquals(bcg.vaccineCode.codingFirstRep.display, "BCG vaccine")
        assertEquals(bcg.vaccineCode.codingFirstRep.system, "http://snomed.info/sct")
        val opv1 = it[1]
        assertEquals(opv1.occurrenceDateTimeType.value.makeItReadable(), "21-Apr-2023")
        assertEquals(opv1.vaccineCode.text, "OPV 1")
        assertEquals(opv1.vaccineCode.codingFirstRep.code, "111164008")
        assertEquals(opv1.vaccineCode.codingFirstRep.display, "Poliovirus vaccine")
        assertEquals(opv1.vaccineCode.codingFirstRep.system, "http://snomed.info/sct")
        val rota1 = it[2]
        assertEquals(rota1.occurrenceDateTimeType.value.makeItReadable(), "28-Apr-2023")
        assertEquals(rota1.vaccineCode.text, "ROTA 1")
        assertEquals(rota1.vaccineCode.codingFirstRep.code, "415354003")
        assertEquals(rota1.vaccineCode.codingFirstRep.display, "Rotavirus vaccine")
        assertEquals(rota1.vaccineCode.codingFirstRep.system, "http://snomed.info/sct")
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun `covid19 extract should generate immunization and encounter`() = runTest {
    val resources = loadExtractionResources("covid19")
    val questionnaire = resources.questionnaire
    val patient = resources.patient.apply { birthDate = Date() }
    val questionnaireResponse = resources.questionnaireResponse
    val result =
      ResourceMapper.extract(
          questionnaire = questionnaire,
          questionnaireResponse = questionnaireResponse,
          structureMapExtractionContext =
            StructureMapExtractionContext(
              structureMapProvider = { _, _ -> resources.structureMap },
              transformSupportServices = transformSupportServices,
            ),
        )
        .also { println(it.encodeResourceToString()) }
    val encounter = result.entry.find { it.resource is Encounter }!!.resource as Encounter
    result.entry
      .filter { it.resource is Task }
      .also { taskList ->
        questionnaireResponse.item
          .find { it.linkId == "previous_vaccine" }!!
          .answer
          .map { it.value as Reference }
          .forEach { taskReference ->
            val outputTask =
              taskList
                .find {
                  (it.resource as Task).id.extractLogicalIdUuid() == taskReference.extractId()
                }
                ?.resource as? Task
            assertNotNull(outputTask)
            assertTrue(outputTask!!.output.size == 3)
            assertNotNull(
              outputTask.output.find {
                it.castToReference(it.value).reference.startsWith(ResourceType.Immunization.name)
              },
            )
            assertNotNull(
              outputTask.output.find {
                it.castToReference(it.value).reference.startsWith(ResourceType.Encounter.name)
              },
            )
          }

        val administrationEncounter =
          result.entry.last { it.resource is Encounter }.resource as Encounter
        assertTrue(administrationEncounter.partOf.reference == encounter.referenceValue())
      }
  }

  data class ExtractionResources(
    val questionnaire: Questionnaire,
    val patient: Patient,
    val questionnaireResponse: QuestionnaireResponse,
    val structureMap: StructureMap,
    val resourcesSlot: MutableList<Resource>,
  )

  private fun loadExtractionResources(name: String): ExtractionResources {
    val questionnaire =
      "extractions/$name/questionnaire.json".readFile().decodeResourceFromString<Questionnaire>()
    val patient =
      "extractions/$name/sample/patient.json".readFile().decodeResourceFromString<Patient>()
    val questionnaireResponse =
      "extractions/$name/sample/questionnaire-response.json"
        .readFile()
        .decodeResourceFromString<QuestionnaireResponse>()
    val structureMap =
      "extractions/$name/structure-map.txt"
        .readFile()
        .let {
          structureMapUtilities.parse(
            it,
            "${name.uppercase().replace("-", "").replace(" ", "")}Extraction",
          )
        }
        .also { println(it.encodeResourceToString()) }
    val resourcesSlot = mutableListOf<Resource>()
    val booleanSlot = slot<Boolean>()
    coEvery { defaultRepository.create(capture(booleanSlot), capture(resourcesSlot)) } returns
      emptyList()
    coEvery { fhirEngine.get<StructureMap>(structureMap.logicalId) } returns structureMap
    return ExtractionResources(
      questionnaire,
      patient,
      questionnaireResponse,
      structureMap,
      resourcesSlot,
    )
  }
}
