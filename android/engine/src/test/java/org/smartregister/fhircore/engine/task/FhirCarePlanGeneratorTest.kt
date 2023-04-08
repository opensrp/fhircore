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

package org.smartregister.fhircore.engine.task

import androidx.work.WorkManager
import androidx.work.WorkRequest
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CarePlan.CarePlanActivityComponent
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.CarePlanConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.REFERENCE
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.engine.util.extension.updateDependentTaskDueDate
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

@HiltAndroidTest
class FhirCarePlanGeneratorTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  val fhirEngine: FhirEngine = mockk()

  lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator

  @Inject lateinit var transformSupportServices: TransformSupportServices

  @Inject lateinit var fhirPathEngine: FHIRPathEngine

  lateinit var structureMapUtilities: StructureMapUtilities

  private val defaultRepository: DefaultRepository = mockk()

  private val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
  private var immunizationResource = Immunization()
  private var encounter = Encounter()
  private var groupTask = Task()
  private var dependentTask: Task = Task()

  @Before
  fun setup() {
    hiltRule.inject()

    structureMapUtilities = StructureMapUtilities(transformSupportServices.simpleWorkerContext)
    val workManager = mockk<WorkManager>()
    every { defaultRepository.dispatcherProvider.io() } returns Dispatchers.IO
    every { defaultRepository.fhirEngine } returns fhirEngine

    fhirCarePlanGenerator =
      FhirCarePlanGenerator(
        fhirEngine = fhirEngine,
        transformSupportServices = transformSupportServices,
        fhirPathEngine = fhirPathEngine,
        defaultRepository = defaultRepository,
        workManager = workManager
      )
    every { workManager.enqueue(any<WorkRequest>()) } returns mockk()
    immunizationResource =
      iParser.parseResource(
        Immunization::class.java,
        "{\n" +
          "   \"resourceType\":\"Immunization\",\n" +
          "   \"id\":\"41921cfe-5074-4eec-925f-2bc581237660\",\n" +
          "   \"identifier\":{\n" +
          "      \"use\":\"official\",\n" +
          "      \"value\":\"6a637a79-df7b-4cc9-93b2-f73f965c31ab\"\n" +
          "   },\n" +
          "   \"status\":\"completed\",\n" +
          "   \"patient\":{\n" +
          "      \"reference\":\"Patient/3e3d698a-4edb-48f9-9330-2f1adc0635d1\"\n" +
          "   },\n" +
          "   \"encounter\":{\n" +
          "      \"reference\":\"Encounter/14e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
          "   },\n" +
          "   \"occurrenceDateTime\":\"2021-10-10T00:00:00+00:00\"\n" +
          "}"
      )

    encounter =
      iParser.parseResource(
        Encounter::class.java,
        "{\n" +
          "   \"resourceType\":\"Encounter\",\n" +
          "   \"id\":\"ebcfb052-47ed-4846-bb13-0f136cdc53a1\",\n" +
          "   \"identifier\":{\n" +
          "      \"use\":\"official\",\n" +
          "      \"value\":\"4b62fff3-6010-4674-84a2-71f2bbdbf2e5\"\n" +
          "   },\n" +
          "   \"status\":\"finished\",\n" +
          "   \"type\":[\n" +
          "      {\n" +
          "         \"coding\":[\n" +
          "            {\n" +
          "               \"system\":\"http://snomed.info/sct\",\n" +
          "               \"code\":\"33879002\",\n" +
          "               \"display\":\"Administration of vaccine to produce active immunity (procedure)\"\n" +
          "            }\n" +
          "         ]\n" +
          "      }\n" +
          "   ],\n" +
          "   \"subject\":{\n" +
          "      \"reference\":\"Patient/3e3d698a-4edb-48f9-9330-2f1adc0635d1\"\n" +
          "   },\n" +
          "   \"period\":{\n" +
          "      \"end\":\"2021-10-01T00:00:00+00:00\"\n" +
          "   },\n" +
          "   \"partOf\":{\n" +
          "      \"reference\":\"Encounter/15e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
          "   }\n" +
          "}"
      )
    groupTask =
      iParser.parseResource(
        Task::class.java,
        "{\n" +
          "   \"resourceType\":\"Task\",\n" +
          "   \"id\":\"a9100c01-c84b-404f-9d24-9b830463a152\",\n" +
          "   \"identifier\":[\n" +
          "      {\n" +
          "         \"use\":\"official\",\n" +
          "         \"value\":\"a20e88b4-4beb-4b31-86cd-572e1445e5f3\"\n" +
          "      }\n" +
          "   ],\n" +
          "   \"basedOn\":[\n" +
          "      {\n" +
          "         \"reference\":\"CarePlan/28d7542c-ba08-4f16-b6a2-19e8b5d4c229\"\n" +
          "      }\n" +
          "   ],\n" +
          "   \"partOf\":{\n" +
          "      \"reference\":\"Task/650203d2-f327-4eb4-a9fd-741e0ce29c3f\"\n" +
          "   },\n" +
          "   \"status\":\"requested\",\n" +
          "   \"intent\":\"plan\",\n" +
          "   \"priority\":\"routine\",\n" +
          "   \"code\":{\n" +
          "      \"coding\":[\n" +
          "         {\n" +
          "            \"system\":\"http://snomed.info/sct\",\n" +
          "            \"code\":\"33879002\",\n" +
          "            \"display\":\"Administration of vaccine to produce active immunity (procedure)\"\n" +
          "         }\n" +
          "      ]\n" +
          "   },\n" +
          "   \"description\":\"OPV 1 at 6 wk vaccine\",\n" +
          "   \"for\":{\n" +
          "      \"reference\":\"Patient/3e3d698a-4edb-48f9-9330-2f1adc0635d1\"\n" +
          "   },\n" +
          "   \"executionPeriod\":{\n" +
          "      \"start\":\"2021-11-12T00:00:00+00:00\",\n" +
          "      \"end\":\"2026-11-11T00:00:00+00:00\"\n" +
          "   },\n" +
          "   \"authoredOn\":\"2023-03-28T10:46:59+00:00\",\n" +
          "   \"requester\":{\n" +
          "      \"reference\":\"Practitioner/3812\"\n" +
          "   },\n" +
          "   \"owner\":{\n" +
          "      \"reference\":\"Practitioner/3812\"\n" +
          "   },\n" +
          "   \"reasonCode\":{\n" +
          "      \"coding\":[\n" +
          "         {\n" +
          "            \"system\":\"http://snomed.info/sct\",\n" +
          "            \"code\":\"111164008\",\n" +
          "            \"display\":\"Poliovirus vaccine\"\n" +
          "         }\n" +
          "      ],\n" +
          "      \"text\":\"OPV\"\n" +
          "   },\n" +
          "   \"reasonReference\":{\n" +
          "      \"reference\":\"Questionnaire/9b1aa23b-577c-4fb2-84e3-591e6facaf82\"\n" +
          "   },\n" +
          "   \"input\":[\n" +
          "      {\n" +
          "         \"type\":{\n" +
          "            \"coding\":[\n" +
          "               {\n" +
          "                  \"system\":\"http://snomed.info/sct\",\n" +
          "                  \"code\":\"900000000000457003\",\n" +
          "                  \"display\":\"Reference set attribute (foundation metadata concept)\"\n" +
          "               }\n" +
          "            ]\n" +
          "         },\n" +
          "         \"value\":{\n" +
          "            \"reference\":\"Task/650203d2-f327-4eb4-a9fd-741e0ce29c3f\"\n" +
          "         }\n" +
          "      },\n" +
          "      {\n" +
          "         \"type\":{\n" +
          "            \"coding\":[\n" +
          "               {\n" +
          "                  \"system\":\"http://snomed.info/sct\",\n" +
          "                  \"code\":\"371154000\",\n" +
          "                  \"display\":\"Dependent (qualifier value)\"\n" +
          "               }\n" +
          "            ]\n" +
          "         },\n" +
          "         \"value\":28\n" +
          "      }\n" +
          "   ],\n" +
          "   \"output\":[\n" +
          "      {\n" +
          "         \"type\":{\n" +
          "            \"coding\":[\n" +
          "               {\n" +
          "                  \"system\":\"http://snomed.info/sct\",\n" +
          "                  \"code\":\"41000179103\",\n" +
          "                  \"display\":\"Immunization record (record artifact)\"\n" +
          "               }\n" +
          "            ]\n" +
          "         },\n" +
          "         \"value\":{\n" +
          "            \"reference\":\"Encounter/14e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
          "         }\n" +
          "      }\n" +
          "   ]\n" +
          "} "
      )
    dependentTask =
      iParser.parseResource(
        Task::class.java,
        "{\n" +
          "   \"resourceType\":\"Task\",\n" +
          "   \"id\":\"650203d2-f327-4eb4-a9fd-741e0ce29c3f\",\n" +
          "   \"identifier\":[\n" +
          "      {\n" +
          "         \"use\":\"official\",\n" +
          "         \"value\":\"ad17cda3-0ac8-43c5-8d9a-9f3adee45e2b\"\n" +
          "      }\n" +
          "   ],\n" +
          "   \"basedOn\":[\n" +
          "      {\n" +
          "         \"reference\":\"CarePlan/28d7542c-ba08-4f16-b6a2-19e8b5d4c229\"\n" +
          "      }\n" +
          "   ],\n" +
          "   \"status\":\"requested\",\n" +
          "   \"intent\":\"plan\",\n" +
          "   \"priority\":\"routine\",\n" +
          "   \"code\":{\n" +
          "      \"coding\":[\n" +
          "         {\n" +
          "            \"system\":\"http://snomed.info/sct\",\n" +
          "            \"code\":\"33879002\",\n" +
          "            \"display\":\"Administration of vaccine to produce active immunity (procedure)\"\n" +
          "         }\n" +
          "      ]\n" +
          "   },\n" +
          "   \"description\":\"OPV 0 at 0 d vaccine\",\n" +
          "   \"for\":{\n" +
          "      \"reference\":\"Patient/3e3d698a-4edb-48f9-9330-2f1adc0635d1\"\n" +
          "   },\n" +
          "   \"executionPeriod\":{\n" +
          "      \"start\":\"2021-10-01T00:00:00+00:00\",\n" +
          "      \"end\":\"2021-10-15T00:00:00+00:00\"\n" +
          "   },\n" +
          "   \"authoredOn\":\"2023-03-28T10:46:59+00:00\",\n" +
          "   \"requester\":{\n" +
          "      \"reference\":\"Practitioner/3812\"\n" +
          "   },\n" +
          "   \"owner\":{\n" +
          "      \"reference\":\"Practitioner/3812\"\n" +
          "   },\n" +
          "   \"reasonCode\":{\n" +
          "      \"coding\":[\n" +
          "         {\n" +
          "            \"system\":\"http://snomed.info/sct\",\n" +
          "            \"code\":\"111164008\",\n" +
          "            \"display\":\"Poliovirus vaccine\"\n" +
          "         }\n" +
          "      ],\n" +
          "      \"text\":\"OPV\"\n" +
          "   },\n" +
          "   \"reasonReference\":{\n" +
          "      \"reference\":\"Questionnaire/9b1aa23b-577c-4fb2-84e3-591e6facaf82\"\n" +
          "   },\n" +
          "   \"output\":[\n" +
          "      {\n" +
          "         \"type\":{\n" +
          "            \"coding\":[\n" +
          "               {\n" +
          "                  \"system\":\"http://snomed.info/sct\",\n" +
          "                  \"code\":\"41000179103\",\n" +
          "                  \"display\":\"Immunization record (record artifact)\"\n" +
          "               }\n" +
          "            ]\n" +
          "         },\n" +
          "         \"value\":{\n" +
          "            \"reference\":\"Encounter/14e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
          "         }\n" +
          "      }\n" +
          "   ]\n" +
          "}"
      )
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testGenerateCarePlanForPatientNoBundle() = runTest {
    val planDefinition = PlanDefinition().apply { id = "plan-1" }
    val patient = Patient()

    coEvery { fhirEngine.get<PlanDefinition>(planDefinition.id) } returns planDefinition
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf()

    val carePlan = fhirCarePlanGenerator.generateOrUpdateCarePlan(planDefinition.id, patient)

    Assert.assertNull(carePlan)
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testGenerateCarePlanForPatient() = runTest {
    val planDefinition =
      "plans/child-routine-visit/plandefinition.json"
        .readFile()
        .decodeResourceFromString<PlanDefinition>()

    val patient =
      "plans/child-routine-visit/sample/patient.json".readFile().decodeResourceFromString<Patient>()

    val structureMapScript = "plans/child-routine-visit/structure-map.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(structureMapScript, "ChildRoutineCarePlan").also {
        // TODO: IMP - The parser does not recognize the time unit i.e. months and prints as ''
        //  so use only months and that would have the unit replaced with 'months'
        println(it.encodeResourceToString().replace("''", "'month'"))
      }

    val resourcesSlot = mutableListOf<Resource>()
    val booleanSlot = slot<Boolean>()
    coEvery { defaultRepository.create(capture(booleanSlot), capture(resourcesSlot)) } returns
      emptyList()
    coEvery { fhirEngine.get<StructureMap>("131373") } returns structureMap
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf()

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        patient,
        Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = patient })
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { carePlan ->
        Assert.assertNotNull(UUID.fromString(carePlan.id))
        Assert.assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)
        Assert.assertEquals(CarePlan.CarePlanIntent.PLAN, carePlan.intent)
        Assert.assertEquals("Child Routine visit Plan", carePlan.title)
        Assert.assertEquals(
          "This defines the schedule of care for patients under 5 years old",
          carePlan.description
        )
        Assert.assertEquals(patient.logicalId, carePlan.subject.extractId())
        Assert.assertEquals(
          DateTimeType.now().value.makeItReadable(),
          carePlan.created.makeItReadable()
        )
        Assert.assertEquals(
          patient.generalPractitionerFirstRep.extractId(),
          carePlan.author.extractId()
        )
        Assert.assertEquals(
          DateTimeType.now().value.makeItReadable(),
          carePlan.period.start.makeItReadable()
        )
        Assert.assertEquals(
          patient.birthDate.plusYears(5).makeItReadable(),
          carePlan.period.end.makeItReadable()
        )
        // 60 - 2  = 58 TODO Fix issue with number of tasks updating relative to today's date
        Assert.assertTrue(carePlan.activityFirstRep.outcomeReference.isNotEmpty())

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { list -> Assert.assertTrue(list.isNotEmpty()) }
          .all { task ->
            // TODO
            task.status == TaskStatus.REQUESTED &&
              LocalDate.parse(task.executionPeriod.end.asYyyyMmDd()).let { localDate ->
                localDate.dayOfMonth == localDate.lengthOfMonth()
              }
          }

        val task1 = resourcesSlot[1] as Task
        Assert.assertEquals(TaskStatus.REQUESTED, task1.status)
        // TODO Fix issue with task start date updating relative to today's date
        Assert.assertTrue(task1.executionPeriod.start.makeItReadable().isNotEmpty())
        // Assert.assertEquals("01-Apr-2022", task1.executionPeriod.start.makeItReadable())
        // Assert.assertEquals("30-Apr-2022", task1.executionPeriod.end.makeItReadable())
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testGenerateCarePlanForGroup() = runTest {
    val planDefinition =
      "plans/household-routine-visit/plandefinition.json"
        .readFile()
        .decodeResourceFromString<PlanDefinition>()

    val group =
      "plans/household-routine-visit/sample/group.json".readFile().decodeResourceFromString<Group>()

    val structureMapScript = "plans/household-routine-visit/structure-map.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(structureMapScript, "HHRoutineCarePlan").also {
        // TODO: IMP - The parser does not recognize the time unit i.e. months and prints as ''
        //  so use only months and that would have the unit replaced with 'months'
        println(it.encodeResourceToString().replace("''", "'month'"))
      }

    val resourcesSlot = mutableListOf<Resource>()
    val booleanSlot = slot<Boolean>()
    coEvery { defaultRepository.create(capture(booleanSlot), capture(resourcesSlot)) } returns
      emptyList()
    coEvery { fhirEngine.get<StructureMap>("hh") } returns structureMap
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf()

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        group,
        Bundle()
          .addEntry(
            Bundle.BundleEntryComponent().apply {
              resource = Encounter().apply { status = Encounter.EncounterStatus.FINISHED }
            }
          )
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { carePlan ->
        Assert.assertNotNull(UUID.fromString(carePlan.id))
        Assert.assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)
        Assert.assertEquals(CarePlan.CarePlanIntent.PLAN, carePlan.intent)
        Assert.assertEquals("HH Routine visit Plan", carePlan.title)
        Assert.assertEquals("sample plan", carePlan.description)
        Assert.assertEquals(group.logicalId, carePlan.subject.extractId())
        Assert.assertEquals(
          DateTimeType.now().value.makeItReadable(),
          carePlan.created.makeItReadable()
        )
        Assert.assertNotNull(carePlan.period.start)
        Assert.assertTrue(carePlan.activityFirstRep.outcomeReference.isNotEmpty())

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { list -> Assert.assertTrue(list.isNotEmpty()) }
          .all { task ->
            task.status == TaskStatus.REQUESTED &&
              LocalDate.parse(task.executionPeriod.end.asYyyyMmDd()).let { localDate ->
                localDate.dayOfMonth == localDate.lengthOfMonth()
              }
          }

        val task1 = resourcesSlot[1] as Task
        Assert.assertEquals(TaskStatus.REQUESTED, task1.status)
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testGenerateCarePlanForHouseHold() = runTest {
    val planDefinition =
      "plans/household-wash-check-routine-visit/plandefinition.json"
        .readFile()
        .decodeResourceFromString<PlanDefinition>()

    val group =
      "plans/household-wash-check-routine-visit/sample/group.json"
        .readFile()
        .decodeResourceFromString<Group>()

    val structureMapScript = "plans/household-wash-check-routine-visit/structure-map.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(structureMapScript, "HHRoutineCarePlan").also {
        // The parser does not recognize the time unit i.e. months and prints as '',
        // so use only months and that would have the unit replaced with 'months'
        it.encodeResourceToString().replace("''", "'month'")
      }

    val resourcesSlot = mutableListOf<Resource>()
    val booleanSlot = slot<Boolean>()
    coEvery { defaultRepository.create(capture(booleanSlot), capture(resourcesSlot)) } returns
      emptyList()
    coEvery { fhirEngine.get<StructureMap>("hh") } returns structureMap
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf()

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        group,
        Bundle()
          .addEntry(
            Bundle.BundleEntryComponent().apply {
              resource = Encounter().apply { status = Encounter.EncounterStatus.FINISHED }
            }
          )
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { carePlan ->
        Assert.assertNotNull(UUID.fromString(carePlan.id))
        Assert.assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)
        Assert.assertEquals(CarePlan.CarePlanIntent.PLAN, carePlan.intent)
        Assert.assertEquals("Household Routine WASH Check Plan", carePlan.title)
        Assert.assertEquals(
          "This defines the schedule of service for WASH Check on households",
          carePlan.description
        )
        Assert.assertEquals(group.logicalId, carePlan.subject.extractId())
        Assert.assertEquals(
          DateTimeType.now().value.makeItReadable(),
          carePlan.created.makeItReadable()
        )
        Assert.assertNotNull(carePlan.period.start)
        Assert.assertTrue(carePlan.activityFirstRep.outcomeReference.isNotEmpty())

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { list -> Assert.assertTrue(list.isNotEmpty() && list.size > 59 && list.size < 62) }
          .all { task ->
            task.status == TaskStatus.REQUESTED &&
              LocalDate.parse(task.executionPeriod.end.asYyyyMmDd()).let { localDate ->
                localDate.dayOfMonth == localDate.lengthOfMonth()
              }
          }

        val task1 = resourcesSlot[1] as Task
        Assert.assertEquals(TaskStatus.REQUESTED, task1.status)
      }
  }

  @Test
  fun testGenerateCarePlanForSickChildOver2m() = runTest {
    val planDefinitionResources =
      loadPlanDefinitionResources("sick-child-visit", listOf("register-over2m"))
    val planDefinition = planDefinitionResources.planDefinition
    val patient = planDefinitionResources.patient.apply { this.birthDate = Date().plusMonths(-3) }
    val questionnaireResponses = planDefinitionResources.questionnaireResponses
    val resourcesSlot = planDefinitionResources.resourcesSlot

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        patient,
        Bundle()
          .addEntry(
            Bundle.BundleEntryComponent().apply { resource = questionnaireResponses.first() }
          )
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { carePlan ->
        assertCarePlan(carePlan, planDefinition, patient, Date(), Date().plusDays(7), 3)

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { assertEquals(4, it.size) } // 4 tasks generated, 3 followup 1 referral
          .also {
            assertTrue(it.all { it.status == TaskStatus.READY })
            assertTrue(it.all { it.`for`.reference == patient.asReference().reference })
          }
          .also {
            it.last().let { task ->
              assertTrue(task.reasonReference.reference == "Questionnaire/132049")
              assertTrue(task.executionPeriod.end.asYyyyMmDd() == Date().plusMonths(1).asYyyyMmDd())
            }
          }
          .take(3)
          .run {
            assertTrue(this.all { it.reasonReference.reference == "Questionnaire/131898" })
            assertTrue(
              this.all { task ->
                task.executionPeriod.end.asYyyyMmDd() == Date().plusDays(7).asYyyyMmDd()
              }
            )
            assertTrue(
              this.all { it.basedOn.first().reference == carePlan.asReference().reference }
            )
            assertTrue(
              this.elementAt(0).executionPeriod.start.asYyyyMmDd() ==
                Date().plusDays(1).asYyyyMmDd()
            )
            assertTrue(
              this.elementAt(1).executionPeriod.start.asYyyyMmDd() ==
                Date().plusDays(2).asYyyyMmDd()
            )
            assertTrue(
              this.elementAt(2).executionPeriod.start.asYyyyMmDd() ==
                Date().plusDays(3).asYyyyMmDd()
            )
          }
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testGenerateCarePlanForSickChildUnder2m() = runTest {
    val planDefinitionResources =
      loadPlanDefinitionResources("sick-child-visit", listOf("register-under2m"))
    val planDefinition = planDefinitionResources.planDefinition
    val patient = planDefinitionResources.patient.apply { this.birthDate = Date().plusMonths(-1) }
    val questionnaireResponses = planDefinitionResources.questionnaireResponses
    val resourcesSlot = planDefinitionResources.resourcesSlot

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        patient,
        Bundle()
          .addEntry(
            Bundle.BundleEntryComponent().apply { resource = questionnaireResponses.first() }
          )
      )
      .also { carePlan ->
        Assert.assertNull(carePlan)

        resourcesSlot.forEach { println(it.encodeResourceToString()) }

        resourcesSlot.map { it as Task }.also { Assert.assertEquals(1, it.size) }.first().let {
          Assert.assertTrue(it.status == TaskStatus.READY)
          Assert.assertTrue(it.basedOn.first().reference == planDefinition.asReference().reference)
          Assert.assertTrue(it.`for`.reference == patient.asReference().reference)
          Assert.assertTrue(it.executionPeriod.start.asYyyyMmDd() == Date().asYyyyMmDd())
        }
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testCompleteCarePlanForSickChildFollowup() = runTest {
    val plandefinition =
      "plans/sick-child-visit/plandefinition.json"
        .readFile()
        .decodeResourceFromString<PlanDefinition>()

    val patient =
      "plans/sick-child-visit/sample/patient.json".readFile().decodeResourceFromString<Patient>()
    val questionnaireResponse =
      "plans/sick-child-visit/sample/questionnaire-response-followup.json"
        .readFile()
        .decodeResourceFromString<QuestionnaireResponse>()

    val structureMapReferral =
      structureMapUtilities.parse("plans/structure-map-referral.txt".readFile(), "ReferralTask")
        .also { println(it.encodeResourceToString()) }

    val createdTasksSlot = mutableListOf<Resource>()
    val updatedTasksSlot = mutableListOf<Resource>()
    val booleanSlot = slot<Boolean>()
    coEvery { defaultRepository.create(capture(booleanSlot), capture(createdTasksSlot)) } returns
      emptyList()
    coEvery { defaultRepository.addOrUpdate(any(), capture(updatedTasksSlot)) } just Runs
    coEvery { fhirEngine.update(any()) } just runs
    coEvery { fhirEngine.get<StructureMap>("528a8603-2e43-4a2e-a33d-1ec2563ffd3e") } returns
      structureMapReferral

    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns
      listOf(
        CarePlan().apply {
          instantiatesCanonical = listOf(CanonicalType(plandefinition.asReference().reference))
          addActivity().apply { this.addOutcomeReference().apply { this.reference = "Task/1111" } }
        }
      )
    coEvery { fhirEngine.get<Task>(any()) } returns
      Task().apply {
        id = "1111"
        status = TaskStatus.READY
      }

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        plandefinition,
        patient,
        Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = questionnaireResponse })
      )!!
      .also { carePlan: CarePlan ->
        Assert.assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)

        createdTasksSlot.forEach { resource -> println(resource.encodeResourceToString()) }

        createdTasksSlot
          .also { list -> Assert.assertTrue(list.size == 2) }
          .filter { resource -> resource.resourceType == ResourceType.Task }
          .map { resource -> resource as Task }
          .also { tasks ->
            tasks.first().let { task ->
              Assert.assertTrue(task.status == TaskStatus.READY)
              Assert.assertTrue(
                task.basedOn.first().reference == plandefinition.asReference().reference
              )
              Assert.assertTrue(task.`for`.reference == patient.asReference().reference)
              Assert.assertTrue(task.executionPeriod.start.asYyyyMmDd() == Date().asYyyyMmDd())
            }
          }
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testUpdateCarePlanForSickChildReferral() = runTest {
    val plandefinition =
      "plans/sick-child-visit/plandefinition.json"
        .readFile()
        .decodeResourceFromString<PlanDefinition>()

    val patient =
      "plans/sick-child-visit/sample/patient.json"
        .readFile()
        .decodeResourceFromString<Patient>()
        .apply { this.birthDate = Date().plusMonths(-1).plusDays(-15) }

    val questionnaireResponse =
      "plans/sick-child-visit/sample/questionnaire-response-register-under2m.json"
        .readFile()
        .decodeResourceFromString<QuestionnaireResponse>()

    val structureMapScript = "plans/structure-map-referral.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(structureMapScript, "ReferralTask").also {
        println(it.encodeResourceToString())
      }

    val resourcesSlot = mutableListOf<Resource>()
    val booleanSlot = slot<Boolean>()
    coEvery { defaultRepository.create(capture(booleanSlot), capture(resourcesSlot)) } returns
      emptyList()
    coEvery { fhirEngine.get<StructureMap>("528a8603-2e43-4a2e-a33d-1ec2563ffd3e") } returns
      structureMap

    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf()

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        plandefinition,
        patient,
        Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = questionnaireResponse })
      )
      .also { _ ->
        resourcesSlot.forEach { println(it.encodeResourceToString()) }

        resourcesSlot
          .filter { it.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { list -> Assert.assertTrue(list.size == 1) }
          .also {
            it.first().let {
              Assert.assertTrue(it.status == TaskStatus.READY)
              Assert.assertTrue(
                it.basedOn.first().reference == plandefinition.asReference().reference
              )
              Assert.assertTrue(it.`for`.reference == patient.asReference().reference)
              Assert.assertTrue(it.executionPeriod.start.asYyyyMmDd() == Date().asYyyyMmDd())
            }
          }
      }
  }

  @Test
  fun `generateOrUpdateCarePlan should generate full careplan for 8 visits when lmp is today`() =
      runTest {
    val planDefinitionResources = loadPlanDefinitionResources("anc-visit", listOf("register"))
    val planDefinition = planDefinitionResources.planDefinition
    val patient = planDefinitionResources.patient
    val questionnaireResponses = planDefinitionResources.questionnaireResponses
    val resourcesSlot = planDefinitionResources.resourcesSlot

    // start of plan is lmp date | 8 tasks to be generated for each month ahead i.e. lmp + 9m
    val lmp = Date()

    questionnaireResponses.first().find("245679f2-6172-456e-8ff3-425f5cea3243")!!.answer.first()
      .value = DateType(lmp)

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        patient,
        Bundle()
          .addEntry(
            Bundle.BundleEntryComponent().apply { resource = questionnaireResponses.first() }
          )
      )!!
      .also { println(it.encodeResourceToString()) }
      .also {
        val carePlan = it
        assertCarePlan(
          carePlan,
          planDefinition,
          patient,
          lmp,
          lmp.plusMonths(9),
          8
        ) // 8 visits for each month of ANC

        resourcesSlot.forEach { println(it.encodeResourceToString()) }

        assertTrue(resourcesSlot.first() is CarePlan)

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { assertEquals(9, it.size) } // 8 for visit, 1 for referral
          .also {
            assertTrue(it.all { it.status == TaskStatus.READY })
            assertTrue(it.all { it.`for`.reference == patient.asReference().reference })
          }
          // last task is referral
          .also {
            it.last().let { task ->
              assertTrue(task.reasonReference.reference == "Questionnaire/132049")
              assertTrue(task.executionPeriod.end.asYyyyMmDd() == Date().plusMonths(1).asYyyyMmDd())
            }
          }
          // first 8 tasks are anc visit for each month start
          .take(8)
          .run {
            assertTrue(this.all { it.reasonReference.reference == "Questionnaire/132155" })
            assertTrue(
              this.all { it.basedOn.first().reference == carePlan.asReference().reference }
            )

            // first visit is lmp plus 1 month and subsequent visit are every month after
            // that until
            // delivery
            var pregnancyStart: BaseDateTimeType = DateTimeType(lmp.clone() as Date)
            this.forEach { task ->
              fhirPathEngine
                .evaluate(pregnancyStart, "\$this + 1 'month'")
                .firstOrNull()
                ?.dateTimeValue()
                ?.let { result -> pregnancyStart = result }
              assertEquals(
                pregnancyStart.valueToString(),
                DateTimeType(task.executionPeriod.start).valueToString()
              )
            }
          }
      }
  }

  @Test
  fun `generateOrUpdateCarePlan should generate careplan for 5 visits when lmp has passed 3 months`() =
      runTest {
    val planDefinitionResources = loadPlanDefinitionResources("anc-visit", listOf("register"))
    val planDefinition = planDefinitionResources.planDefinition
    val patient = planDefinitionResources.patient
    val questionnaireResponses = planDefinitionResources.questionnaireResponses
    val resourcesSlot = planDefinitionResources.resourcesSlot

    // start of plan is lmp date | 8 tasks to be generated for each month ahead i.e. lmp + 9m
    // anc registered late so skip the tasks which passed due date
    val lmp = DateType(Date()).apply { add(Calendar.MONTH, -4) }

    questionnaireResponses.first().find("245679f2-6172-456e-8ff3-425f5cea3243")!!.answer.first()
      .value = lmp

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        patient,
        Bundle()
          .addEntry(
            Bundle.BundleEntryComponent().apply { resource = questionnaireResponses.first() }
          )
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { resourcesSlot.forEach { println(it.encodeResourceToString()) } }
      .also { carePlan ->
        assertCarePlan(
          carePlan,
          planDefinition,
          patient,
          lmp.value,
          lmp.value.plusMonths(9),
          5
        ) // 5 visits for each month of ANC

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { assertEquals(6, it.size) } // 5 for visit, 1 for referral
          .also {
            assertTrue(it.all { it.status == TaskStatus.READY })
            assertTrue(it.all { it.`for`.reference == patient.asReference().reference })
          }
          // first 5 tasks are anc visit for each month of pregnancy
          .take(5)
          .run {
            assertTrue(this.all { it.reasonReference.reference == "Questionnaire/132155" })
            assertTrue(
              this.all { it.basedOn.first().reference == carePlan.asReference().reference }
            )

            // first visit is lmp plus 1 month and subsequent visit are every month after
            // that until
            // delivery
            // skip tasks for past 3 months of late registration
            val ancStart = lmp.value.plusMonths(3).clone() as Date
            this.forEachIndexed { index, task ->
              assertEquals(
                ancStart.plusMonths(index + 1).asYyyyMmDd(),
                task.executionPeriod.start.asYyyyMmDd()
              )
            }
          }
      }
  }

  @Test
  fun `generateOrUpdateCarePlan should generate careplan for next visit when ondemand task is required`() =
      runTest {
    val planDefinitionResources =
      loadPlanDefinitionResources("anc-visit-ondemand", listOf("register"))
    val planDefinition = planDefinitionResources.planDefinition
    val patient = planDefinitionResources.patient
    val questionnaireResponses = planDefinitionResources.questionnaireResponses
    val resourcesSlot = planDefinitionResources.resourcesSlot

    // start of plan is lmp date | 8 tasks to be generated for each month ahead i.e. lmp + 9m
    // anc registered late so skip the tasks which passed due date
    val lmp = Date().plusMonths(-4)

    questionnaireResponses.first().find("245679f2-6172-456e-8ff3-425f5cea3243")!!.answer.first()
      .value = DateType(lmp)

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        patient,
        Bundle()
          .addEntry(
            Bundle.BundleEntryComponent().apply { resource = questionnaireResponses.first() }
          )
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { carePlan ->
        assertCarePlan(
          carePlan,
          planDefinition,
          patient,
          lmp,
          lmp.plusMonths(9),
          1
        ) // 1 visits for next month of ANC

        resourcesSlot.forEach { println(it.encodeResourceToString()) }
        assertEquals(1, carePlan.activityFirstRep.outcomeReference.size)

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { assertEquals(2, it.size) } // 1 for visit, 1 for referral
          .also { tasks ->
            assertTrue(tasks.all { it.status == TaskStatus.READY })
            assertTrue(tasks.all { it.`for`.reference == patient.asReference().reference })
          }
          // first 5 tasks are anc visit for each month of pregnancy
          .take(1)
          .run {
            assertTrue(this.all { it.reasonReference.reference == "Questionnaire/132155" })
            assertTrue(
              this.all { it.basedOn.first().reference == carePlan.asReference().reference }
            )

            this.forEachIndexed { _, task ->
              assertEquals(
                Date().plusMonths(1).asYyyyMmDd(),
                task.executionPeriod.start.asYyyyMmDd()
              )
            }
          }
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun `Generate CarePlan should generate child immunization schedule`() = runTest {
    val planDefinitionResources =
      loadPlanDefinitionResources("child-immunization-schedule", listOf("register-temp"))
    val planDefinition = planDefinitionResources.planDefinition
    val patient = planDefinitionResources.patient
    val questionnaireResponses = planDefinitionResources.questionnaireResponses
    val resourcesSlot = planDefinitionResources.resourcesSlot

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        patient,
        Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = patient })
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { carePlan ->
        assertCarePlan(
          carePlan,
          planDefinition,
          patient,
          patient.birthDate,
          patient.birthDate.plusDays(4017),
          20
        )

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map {
            println(it.encodeResourceToString())
            it as Task
          }
          .also { tasks ->
            assertTrue(tasks.all { it.status == TaskStatus.REQUESTED })
            assertTrue(
              tasks.all {
                it.reasonReference.reference == "Questionnaire/9b1aa23b-577c-4fb2-84e3-591e6facaf82"
              }
            )
            assertTrue(
              tasks.all {
                it.code.codingFirstRep.display ==
                  "Administration of vaccine to produce active immunity (procedure)" &&
                  it.code.codingFirstRep.code == "33879002"
              }
            )
            assertTrue(tasks.all { it.description.contains(it.reasonCode.text, true) })
            assertTrue(
              tasks.all { it.`for`.reference == questionnaireResponses.first().subject.reference }
            )
            assertTrue(
              tasks.all { it.basedOnFirstRep.reference == carePlan.asReference().reference }
            )
          }
          .also { tasks ->
            val vaccines =
              mutableMapOf<String, Date>(
                "BCG" to patient.birthDate,
                "OPV 0" to patient.birthDate,
                "PENTA 1" to patient.birthDate.plusDays(42),
                "OPV 1" to patient.birthDate.plusDays(42),
                "PCV 1" to patient.birthDate.plusDays(42),
                "ROTA 1" to patient.birthDate.plusDays(42),
                "PENTA 2" to patient.birthDate.plusDays(70),
                "OPV 2" to patient.birthDate.plusDays(70),
                "PCV 2" to patient.birthDate.plusDays(70),
                "ROTA 2" to patient.birthDate.plusDays(70),
                "PENTA 3" to patient.birthDate.plusDays(98),
                "OPV 3" to patient.birthDate.plusDays(98),
                "PCV 3" to patient.birthDate.plusDays(98),
                "IPV" to patient.birthDate.plusDays(98),
                "MEASLES 1" to patient.birthDate.plusMonths(9),
                "MEASLES 2" to patient.birthDate.plusMonths(15),
                "YELLOW FEVER" to patient.birthDate.plusMonths(9),
                "TYPHOID" to patient.birthDate.plusMonths(9),
                "HPV 1" to patient.birthDate.plusDays(3285),
                "HPV 2" to patient.birthDate.plusDays(3467),
              )
            vaccines.forEach { vaccine ->
              println(vaccine)

              val task = tasks.find { it.description.startsWith(vaccine.key) }
              assertNotNull(task)
              assertTrue(task!!.executionPeriod.start.asYyyyMmDd() == vaccine.value.asYyyyMmDd())
            }
          }
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun `test generateOrUpdateCarePlan returns success even when evaluatedValue is null`() =
      runBlocking {
    val planDefinitionResources =
      loadPlanDefinitionResources("child-immunization-schedule", listOf("register-temp"))
    val planDefinition = planDefinitionResources.planDefinition
    val patient = planDefinitionResources.patient
    val data = Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = patient })

    val dynamicValue = planDefinition.action.first().dynamicValue
    val expressionValue = dynamicValue.find { it.expression.expression == "%rootResource.title" }

    // Update the value of the expression
    expressionValue?.let { it.expression = Expression().apply { expression = "dummyExpression" } }

    // call the method under test and get the result
    val result = fhirCarePlanGenerator.generateOrUpdateCarePlan(planDefinition, patient, data)

    // assert that the result is not null
    assertNotNull(result)
  }

  @Test
  @ExperimentalCoroutinesApi
  fun `Generate CarePlan should generate child immunization schedule with correct groupIdentifier value`() =
      runTest {
    val planDefinitionResources =
      loadPlanDefinitionResources("child-immunization-schedule", listOf("register-temp"))
    val planDefinition = planDefinitionResources.planDefinition
    val patient = planDefinitionResources.patient
    val questionnaireResponses = planDefinitionResources.questionnaireResponses
    val resourcesSlot = planDefinitionResources.resourcesSlot

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        planDefinition,
        patient,
        Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = patient })
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { carePlan ->
        assertCarePlan(
          carePlan,
          planDefinition,
          patient,
          patient.birthDate,
          patient.birthDate.plusDays(4017),
          20
        )

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map {
            println(it.encodeResourceToString())
            it as Task
          }
          .also { tasks ->
            assertTrue(tasks.all { it.status == TaskStatus.REQUESTED })
            assertTrue(
              tasks.all {
                it.reasonReference.reference == "Questionnaire/9b1aa23b-577c-4fb2-84e3-591e6facaf82"
              }
            )
            assertTrue(
              tasks.all {
                it.code.codingFirstRep.display ==
                  "Administration of vaccine to produce active immunity (procedure)" &&
                  it.code.codingFirstRep.code == "33879002"
              }
            )
            assertTrue(tasks.all { it.description.contains(it.reasonCode.text, true) })
            assertTrue(
              tasks.all { it.`for`.reference == questionnaireResponses.first().subject.reference }
            )
            assertTrue(
              tasks.all { it.basedOnFirstRep.reference == carePlan.asReference().reference }
            )
          }
          .also { tasks ->
            val vaccines =
              mutableMapOf<String, Date>(
                "BCG" to patient.birthDate,
                "OPV 0" to patient.birthDate,
                "PENTA 1" to patient.birthDate.plusDays(42),
                "OPV 1" to patient.birthDate.plusDays(42),
                "PCV 1" to patient.birthDate.plusDays(42),
                "ROTA 1" to patient.birthDate.plusDays(42),
                "PENTA 2" to patient.birthDate.plusDays(70),
                "OPV 2" to patient.birthDate.plusDays(70),
                "PCV 2" to patient.birthDate.plusDays(70),
                "ROTA 2" to patient.birthDate.plusDays(70),
                "PENTA 3" to patient.birthDate.plusDays(98),
                "OPV 3" to patient.birthDate.plusDays(98),
                "PCV 3" to patient.birthDate.plusDays(98),
                "IPV" to patient.birthDate.plusDays(98),
                "MEASLES 1" to patient.birthDate.plusMonths(9),
                "MEASLES 2" to patient.birthDate.plusMonths(15),
                "YELLOW FEVER" to patient.birthDate.plusMonths(9),
                "TYPHOID" to patient.birthDate.plusMonths(9),
                "HPV 1" to patient.birthDate.plusDays(3285),
                "HPV 2" to patient.birthDate.plusDays(3467),
              )
            vaccines.forEach { vaccine ->
              println(vaccine)

              val task = tasks.find { it.description.startsWith(vaccine.key) }
              assertNotNull(task)
              when (vaccine.key) {
                "BCG" -> assertTrue(task!!.groupIdentifier.value == "0_d")
                "OPV 0" -> assertTrue(task!!.groupIdentifier.value == "0_d")
                "PENTA 1" -> assertTrue(task!!.groupIdentifier.value == "6_wk")
                "OPV 1" -> assertTrue(task!!.groupIdentifier.value == "6_wk")
                "PCV 1" -> assertTrue(task!!.groupIdentifier.value == "6_wk")
                "ROTA 1" -> assertTrue(task!!.groupIdentifier.value == "6_wk")
                "PENTA 2" -> assertTrue(task!!.groupIdentifier.value == "10_wk")
                "OPV 2" -> assertTrue(task!!.groupIdentifier.value == "10_wk")
                "PCV 2" -> assertTrue(task!!.groupIdentifier.value == "10_wk")
                "ROTA 2" -> assertTrue(task!!.groupIdentifier.value == "10_wk")
                "PENTA 3" -> assertTrue(task!!.groupIdentifier.value == "14_wk")
                "OPV 3" -> assertTrue(task!!.groupIdentifier.value == "14_wk")
                "PCV 3" -> assertTrue(task!!.groupIdentifier.value == "14_wk")
                "IPV" -> assertTrue(task!!.groupIdentifier.value == "14_wk")
                "MEASLES 1" -> assertTrue(task!!.groupIdentifier.value == "9_mo")
                "MEASLES 2" -> assertTrue(task!!.groupIdentifier.value == "15_mo")
                "YELLOW FEVER" -> assertTrue(task!!.groupIdentifier.value == "9_mo")
                "TYPHOID" -> assertTrue(task!!.groupIdentifier.value == "9_mo")
                "HPV 1" -> assertTrue(task!!.groupIdentifier.value == "3285_d")
                "HPV 2" -> assertTrue(task!!.groupIdentifier.value == "3467_d")
              }
              assertTrue(task!!.executionPeriod.start.asYyyyMmDd() == vaccine.value.asYyyyMmDd())
            }
          }
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun `Generate CarePlan should generate disease followup schedule`() = runTest {
    val plandefinition =
      "plans/disease-followup/plan-definition.json"
        .readFile()
        .decodeResourceFromString<PlanDefinition>()

    val patient =
      "plans/disease-followup/patient.json".readFile().decodeResourceFromString<Patient>()
    val diseaseFollowUpQuestionnaireResponseString =
      "plans/disease-followup/questionnaire-response.json"
        .readFile()
        .decodeResourceFromString<QuestionnaireResponse>()

    val structureMapScript = "plans/disease-followup/structure-map.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(structureMapScript, "eCBIS Child Immunization").also {
        println(it.encodeResourceToString())
      }

    val resourcesSlot = mutableListOf<Resource>()
    val booleanSlot = slot<Boolean>()
    coEvery { defaultRepository.create(capture(booleanSlot), capture(resourcesSlot)) } returns
      emptyList()
    coEvery { fhirEngine.get<StructureMap>("63752b18-9f0e-48a7-9a21-d3714be6309a") } returns
      structureMap
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf()
    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        plandefinition,
        patient,
        Bundle()
          .addEntry(Bundle.BundleEntryComponent().apply { resource = patient })
          .addEntry(
            Bundle.BundleEntryComponent().apply {
              resource = diseaseFollowUpQuestionnaireResponseString
            }
          )
      )!!
      .also { println(it.encodeResourceToString()) }
      .also { carePlan ->
        Assert.assertNotNull(UUID.fromString(carePlan.id))
        Assert.assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)
        Assert.assertEquals(CarePlan.CarePlanIntent.PLAN, carePlan.intent)

        Assert.assertEquals("Disease Follow Up", carePlan.title)
        Assert.assertEquals(
          "This is a follow up for patient's marked with the following diseases HIV, TB, Mental Health & CM-NTD",
          carePlan.description
        )
        Assert.assertEquals(patient.logicalId, carePlan.subject.extractId())
        Assert.assertEquals(
          DateTimeType.now().value.makeItReadable(),
          carePlan.created.makeItReadable()
        )
        Assert.assertEquals(
          patient.generalPractitionerFirstRep.extractId(),
          carePlan.author.extractId()
        )
        Assert.assertTrue(carePlan.activityFirstRep.outcomeReference.isNotEmpty())
        coEvery { defaultRepository.create(capture(booleanSlot), capture(resourcesSlot)) }
        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map { it as Task }
          .also { list -> Assert.assertTrue(list.isNotEmpty()) }
          .also { println(it.last().encodeResourceToString()) }
          .all { task ->
            task.status == TaskStatus.INPROGRESS &&
              LocalDate.parse(task.executionPeriod.end.asYyyyMmDd()).let { localDate ->
                localDate.dayOfMonth == localDate.lengthOfMonth()
              }
          }
      }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun `transitionTaskTo should update task status`() = runTest {
    coEvery { fhirEngine.get(ResourceType.Task, "12345") } returns Task().apply { id = "12345" }
    coEvery { defaultRepository.addOrUpdate(any(), any()) } just Runs

    fhirCarePlanGenerator.updateTaskDetailsByResourceId("12345", TaskStatus.COMPLETED)

    val task = slot<Task>()
    coVerify { defaultRepository.addOrUpdate(any(), capture(task)) }

    Assert.assertEquals(TaskStatus.COMPLETED, task.captured.status)
  }

  @Test
  fun testConditionallyUpdateCarePlanStatusTerminatesWhenNoCarePlanIsFound() {
    val planDefinitions = listOf("plandef-1")
    val carePlanConfig = CarePlanConfig(fhirPathExpression = "Patient.active")
    val questionnaireConfig: QuestionnaireConfig =
      QuestionnaireConfig(
        id = "id-1",
        planDefinitions = planDefinitions,
        carePlanConfigs = listOf(carePlanConfig)
      )
    val patient =
      Patient().apply {
        id = "patient-1"
        active = true
      }
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf()

    runBlocking {
      fhirCarePlanGenerator.conditionallyUpdateCarePlanStatus(
        questionnaireConfig = questionnaireConfig,
        subject = patient
      )
    }

    coVerify(exactly = 0) { fhirEngine.get(any(), any()) }
    coVerify(exactly = 0) { fhirEngine.update(any()) }
  }

  @Test
  fun testConditionallyUpdateCarePlanStatusRevokesCarePlanWhenCarePlanStatusIsClosed() {
    val planDefinitions = listOf("plandef-1")
    val carePlanConfig = CarePlanConfig(fhirPathExpression = "Patient.active")
    val questionnaireConfig: QuestionnaireConfig =
      QuestionnaireConfig(
        id = "id-1",
        planDefinitions = planDefinitions,
        carePlanConfigs = listOf(carePlanConfig)
      )
    val patient =
      Patient().apply {
        id = "patient-1"
        active = true
      }
    val carePlan =
      CarePlan().apply {
        id = "careplan-1"
        status = CarePlan.CarePlanStatus.ACTIVE
      }
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf(carePlan)
    coEvery { fhirEngine.update(any()) } just runs

    runBlocking {
      fhirCarePlanGenerator.conditionallyUpdateCarePlanStatus(
        questionnaireConfig = questionnaireConfig,
        subject = patient
      )
    }

    val carePlanSlot = slot<CarePlan>()
    coVerify { fhirEngine.update(capture(carePlanSlot)) }
    assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlanSlot.captured.status)
  }

  @Test
  fun testConditionallyUpdateCarePlanStatusFetchesResourceUsingFhirPathResourceId() {
    val planDefinitions = listOf("plandef-1")
    val carePlanConfig =
      CarePlanConfig(
        fhirPathExpression = "Group.active",
        fhirPathResource = "Group",
        fhirPathResourceId = "4595a221-7ad3-4219-97f0-1920a02f8882"
      )
    val questionnaireConfig: QuestionnaireConfig =
      QuestionnaireConfig(
        id = "id-1",
        planDefinitions = planDefinitions,
        carePlanConfigs = listOf(carePlanConfig)
      )
    val patient =
      Patient().apply {
        id = "patient-1"
        active = true
      }
    val carePlan =
      CarePlan().apply {
        id = "careplan-1"
        status = CarePlan.CarePlanStatus.ACTIVE
      }
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf(carePlan)
    coEvery { fhirEngine.get(any(), any()) } returns Group().apply { active = true }
    coEvery { fhirEngine.update(any()) } just runs

    runBlocking {
      fhirCarePlanGenerator.conditionallyUpdateCarePlanStatus(
        questionnaireConfig = questionnaireConfig,
        subject = patient
      )
    }

    coVerify { fhirEngine.get(ResourceType.Group, carePlanConfig.fhirPathResourceId!!) }

    val carePlanSlot = slot<CarePlan>()
    coVerify { fhirEngine.update(capture(carePlanSlot)) }
    assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlanSlot.captured.status)
  }

  @Test
  fun testConditionallyUpdateCarePlanStatusDoesNotUpdateCarePlanWhenFhirPathResourceExpressionFails() {
    val planDefinitions = listOf("plandef-1")
    val carePlanConfig =
      CarePlanConfig(
        fhirPathExpression = "Group.active",
        fhirPathResource = "Group",
        fhirPathResourceId = "4595a221-7ad3-4219-97f0-1920a02f8882"
      )
    val questionnaireConfig: QuestionnaireConfig =
      QuestionnaireConfig(
        id = "id-1",
        planDefinitions = planDefinitions,
        carePlanConfigs = listOf(carePlanConfig)
      )
    val patient =
      Patient().apply {
        id = "patient-1"
        active = true
      }
    val carePlan =
      CarePlan().apply {
        id = "careplan-1"
        status = CarePlan.CarePlanStatus.ACTIVE
      }
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf(carePlan)
    coEvery { fhirEngine.get(any(), any()) } returns Group().apply { active = false }
    coEvery { fhirEngine.update(any()) } just runs

    runBlocking {
      fhirCarePlanGenerator.conditionallyUpdateCarePlanStatus(
        questionnaireConfig = questionnaireConfig,
        subject = patient
      )
    }

    coVerify { fhirEngine.get(ResourceType.Group, carePlanConfig.fhirPathResourceId!!) }
    coVerify(exactly = 0) { fhirEngine.update(any()) }
  }

  @Test
  fun testConditionallyUpdateCarePlanStatusCancelsTasks() {
    val planDefinitions = listOf("plandef-1")
    val carePlanConfig =
      CarePlanConfig(
        fhirPathExpression = "Group.active",
        fhirPathResource = "Group",
        fhirPathResourceId = "4595a221-7ad3-4219-97f0-1920a02f8882"
      )
    val questionnaireConfig: QuestionnaireConfig =
      QuestionnaireConfig(
        id = "id-1",
        planDefinitions = planDefinitions,
        carePlanConfigs = listOf(carePlanConfig)
      )
    val patient =
      Patient().apply {
        id = "patient-1"
        active = true
      }
    val carePlan =
      CarePlan().apply {
        id = "careplan-1"
        status = CarePlan.CarePlanStatus.ACTIVE
        activity =
          listOf(
            CarePlanActivityComponent().apply {
              outcomeReference = listOf(Reference("Task/f10eec84-ef78-4bd1-bac4-6e68c7548f4c"))
            }
          )
      }
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf(carePlan)
    coEvery { fhirEngine.get(any(), any()) } returns Group().apply { active = true }
    coEvery { fhirEngine.update(any()) } just runs
    val task =
      Task().apply {
        id = "uuid"
        status = TaskStatus.READY
      }
    coEvery { fhirCarePlanGenerator.getTask(any()) } returns task
    coEvery { defaultRepository.addOrUpdate(any(), any()) } just runs

    runBlocking {
      fhirCarePlanGenerator.conditionallyUpdateCarePlanStatus(
        questionnaireConfig = questionnaireConfig,
        subject = patient
      )
    }

    coVerify { fhirEngine.get(ResourceType.Group, carePlanConfig.fhirPathResourceId!!) }

    val carePlanSlot = slot<CarePlan>()
    coVerify { fhirEngine.update(capture(carePlanSlot)) }
    assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlanSlot.captured.status)

    val taskSlot = slot<Task>()
    coVerify { defaultRepository.addOrUpdate(true, capture(taskSlot)) }
    assertEquals(task.id, taskSlot.captured.id)
    assertEquals(task.status, taskSlot.captured.status)
  }

  @Test
  fun testConditionallyUpdateCarePlanStatusDoesNotCancelTasksWhenStatusIsCompleted() {
    val planDefinitions = listOf("plandef-1")
    val carePlanConfig =
      CarePlanConfig(
        fhirPathExpression = "Group.active",
        fhirPathResource = "Group",
        fhirPathResourceId = "4595a221-7ad3-4219-97f0-1920a02f8882"
      )
    val questionnaireConfig: QuestionnaireConfig =
      QuestionnaireConfig(
        id = "id-1",
        planDefinitions = planDefinitions,
        carePlanConfigs = listOf(carePlanConfig)
      )
    val patient =
      Patient().apply {
        id = "patient-1"
        active = true
      }
    val carePlan =
      CarePlan().apply {
        id = "careplan-1"
        status = CarePlan.CarePlanStatus.ACTIVE
        activity =
          listOf(
            CarePlanActivityComponent().apply {
              outcomeReference = listOf(Reference("Task/f10eec84-ef78-4bd1-bac4-6e68c7548f4c"))
            }
          )
      }
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf(carePlan)
    coEvery { fhirEngine.get(any(), any()) } returns Group().apply { active = true }
    coEvery { fhirEngine.update(any()) } just runs
    val task =
      Task().apply {
        id = "uuid"
        status = TaskStatus.COMPLETED
      }
    coEvery { fhirCarePlanGenerator.getTask(any()) } returns task
    coEvery { defaultRepository.addOrUpdate(any(), any()) } just runs

    runBlocking {
      fhirCarePlanGenerator.conditionallyUpdateCarePlanStatus(
        questionnaireConfig = questionnaireConfig,
        subject = patient
      )
    }

    coVerify { fhirEngine.get(ResourceType.Group, carePlanConfig.fhirPathResourceId!!) }

    val carePlanSlot = slot<CarePlan>()
    coVerify { fhirEngine.update(capture(carePlanSlot)) }
    assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlanSlot.captured.status)

    coVerify(exactly = 0) { defaultRepository.addOrUpdate(any(), any()) }
  }

  data class PlanDefinitionResources(
    val planDefinition: PlanDefinition,
    val patient: Patient,
    val questionnaireResponses: List<QuestionnaireResponse>,
    val structureMap: StructureMap,
    val structureMapReferral: StructureMap,
    val resourcesSlot: MutableList<Resource>
  )

  fun loadPlanDefinitionResources(
    planName: String,
    questionnaireResponseTags: List<String> = emptyList()
  ): PlanDefinitionResources {
    val planDefinition =
      "plans/$planName/plandefinition.json".readFile().decodeResourceFromString<PlanDefinition>()

    val patient =
      "plans/$planName/sample/patient.json".readFile().decodeResourceFromString<Patient>()

    val questionnaireResponses =
      questionnaireResponseTags.map {
        "plans/$planName/sample/questionnaire-response-$it.json"
          .readFile()
          .decodeResourceFromString<QuestionnaireResponse>()
      }

    val structureMapRegister =
      structureMapUtilities.parse(
          "plans/$planName/structure-map-register.txt".readFile(),
          "${planName.uppercase().replace("-", "").replace(" ", "")}CarePlan"
        )
        .also { println(it.encodeResourceToString()) }

    val structureMapReferral =
      structureMapUtilities.parse("plans/structure-map-referral.txt".readFile(), "ReferralTask")
        .also { println(it.encodeResourceToString()) }

    val resourcesSlot = mutableListOf<Resource>()
    val booleanSlot = slot<Boolean>()
    coEvery { defaultRepository.create(capture(booleanSlot), capture(resourcesSlot)) } returns
      emptyList()
    coEvery { fhirEngine.search<CarePlan>(Search(ResourceType.CarePlan)) } returns listOf()
    coEvery { fhirEngine.get<StructureMap>(structureMapRegister.logicalId) } returns
      structureMapRegister
    coEvery { fhirEngine.get<StructureMap>("528a8603-2e43-4a2e-a33d-1ec2563ffd3e") } returns
      structureMapReferral

    return PlanDefinitionResources(
      planDefinition,
      patient,
      questionnaireResponses,
      structureMapRegister,
      structureMapReferral,
      resourcesSlot
    )
  }

  fun assertCarePlan(
    carePlan: CarePlan,
    planDefinition: PlanDefinition,
    patient: Patient,
    referenceDate: Date,
    endDate: Date,
    visitTasks: Int
  ) {
    assertNotNull(UUID.fromString(carePlan.id))
    assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)
    assertEquals(CarePlan.CarePlanIntent.PLAN, carePlan.intent)
    assertEquals(planDefinition.title, carePlan.title)
    assertEquals(planDefinition.description, carePlan.description)
    assertEquals(patient.logicalId, carePlan.subject.extractId())
    assertEquals(DateTimeType.now().value.makeItReadable(), carePlan.created.makeItReadable())
    assertEquals(patient.generalPractitionerFirstRep.extractId(), carePlan.author.extractId())

    assertEquals(referenceDate.makeItReadable(), carePlan.period.start.makeItReadable())
    assertEquals(endDate.makeItReadable(), carePlan.period.end.makeItReadable())

    assertTrue(carePlan.activityFirstRep.outcomeReference.isNotEmpty())
    assertEquals(visitTasks, carePlan.activityFirstRep.outcomeReference.size)
  }

  @Test
  fun `updateDependentTaskDueDate - no dependent tasks`() = runBlocking {
    groupTask.apply {
      status = TaskStatus.REQUESTED
      partOf = emptyList()
    }
    // when
    val updatedTask = groupTask.updateDependentTaskDueDate(defaultRepository)
    // then
    assertEquals("650203d2-f327-4eb4-a9fd-741e0ce29c3f", dependentTask.logicalId)
    assertEquals(groupTask, updatedTask)
  }

  @Test
  fun `test updateDependentTaskDueDate - dependent task without output`() {
    coEvery { fhirEngine.get(ResourceType.Task, "650203d2-f327-4eb4-a9fd-741e0ce29c3f") } returns
      groupTask.apply {
        status = TaskStatus.READY
        partOf = listOf(Reference("Task/650203d2-f327-4eb4-a9fd-741e0ce29c3f"))
      }
    dependentTask.apply {
      output = emptyList()
      executionPeriod =
        Period().apply {
          start = Date()
          end = Date()
        }
    }

    coEvery {
      defaultRepository.loadResource(Reference(groupTask.partOf.first().reference))
    } returns dependentTask
    val updatedTask = runBlocking { groupTask.updateDependentTaskDueDate(defaultRepository) }
    assertEquals(groupTask, updatedTask)
  }

  @Test
  fun `test updateDependentTaskDueDate with dependent task , with output but no execution period start date`() {
    coEvery { fhirEngine.get(ResourceType.Task, "650203d2-f327-4eb4-a9fd-741e0ce29c3f") } returns
      groupTask.apply { status = TaskStatus.INPROGRESS }
    dependentTask.apply { dependentTask.executionPeriod.start = null }
    coEvery {
      defaultRepository.loadResource(Reference(groupTask.partOf.first().reference))
    } returns dependentTask
    val updatedTask = runBlocking { groupTask.updateDependentTaskDueDate(defaultRepository) }
    assertEquals(groupTask, updatedTask)
  }

  @Test
  fun `test updateDependentTaskDueDate with dependent task with output, execution period start date, and encounter part of reference that is null`() {

    coEvery { fhirEngine.get(ResourceType.Task, "650203d2-f327-4eb4-a9fd-741e0ce29c3f") } returns
      dependentTask.apply {
        status = TaskStatus.INPROGRESS
        output =
          listOf(
            Task.TaskOutputComponent(
              CodeableConcept(),
              StringType(
                "{\n" +
                  "          \"reference\": \"Encounter/14e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
                  "        }"
              )
            )
          )
      }
    coEvery {
      defaultRepository.loadResource(
        Reference(
          Json.decodeFromString<JsonObject>(dependentTask.output.first().value.toString())[
              REFERENCE]
            ?.jsonPrimitive
            ?.content
        )
      )
    } returns encounter.apply { partOf = null }
    coEvery { defaultRepository.loadResource(Reference(ArgumentMatchers.anyString())) } returns
      Immunization()
    val updatedTask = runBlocking { groupTask.updateDependentTaskDueDate(defaultRepository) }
    assertEquals(groupTask, updatedTask)
  }

  @Test
  fun `test updateDependentTaskDueDate with dependent task with output, execution period start date, and encounter part of reference that is not an Immunization`() {
    coEvery { fhirEngine.get(ResourceType.Task, "650203d2-f327-4eb4-a9fd-741e0ce29c3f") } returns
      dependentTask.apply {
        status = TaskStatus.INPROGRESS
        output =
          listOf(
            Task.TaskOutputComponent(
              CodeableConcept(),
              StringType(
                "{\n" +
                  "          \"reference\": \"Encounter/14e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
                  "        }"
              )
            )
          )
        input = listOf(Task.ParameterComponent(CodeableConcept(), StringType("9")))
      }
    coEvery {
      fhirEngine.get(ResourceType.Encounter, "14e2ae52-32fc-4507-8736-1177cdaafe90")
    } returns encounter
    coEvery {
      fhirEngine.get(ResourceType.Immunization, "15e2ae52-32fc-4507-8736-1177cdaafe90")
    } returns Task()

    coEvery {
      defaultRepository.loadResource(
        Reference(
          Json.decodeFromString<JsonObject>(dependentTask.output.first().value.toString())[
              REFERENCE]
            ?.jsonPrimitive
            ?.content
        )
      )
    } returns encounter

    coEvery { defaultRepository.loadResource(Reference(encounter.partOf.reference)) } returns
      immunizationResource

    val updatedTask = runBlocking { groupTask.updateDependentTaskDueDate(defaultRepository) }
    assertEquals(groupTask, updatedTask)
  }

  @Test
  fun `updateDependentTaskDueDate with Task input value equal or greater than difference between administration date and depedentTask executionPeriod start`() {
    coEvery { fhirEngine.get(ResourceType.Task, "650203d2-f327-4eb4-a9fd-741e0ce29c3f") } returns
      dependentTask.apply {
        status = TaskStatus.INPROGRESS
        output =
          listOf(
            Task.TaskOutputComponent(
              CodeableConcept(),
              StringType(
                "{\n" +
                  "          \"reference\": \"Encounter/14e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
                  "        }"
              )
            )
          )
        input = listOf(Task.ParameterComponent(CodeableConcept(), StringType("9")))
      }
    coEvery {
      fhirEngine.get(ResourceType.Encounter, "14e2ae52-32fc-4507-8736-1177cdaafe90")
    } returns encounter
    coEvery {
      fhirEngine.get(ResourceType.Immunization, "15e2ae52-32fc-4507-8736-1177cdaafe90")
    } returns immunizationResource

    coEvery {
      defaultRepository.loadResource(
        Reference(
          Json.decodeFromString<JsonObject>(dependentTask.output.first().value.toString())[
              REFERENCE]
            ?.jsonPrimitive
            ?.content
        )
      )
    } returns encounter

    coEvery { defaultRepository.loadResource(Reference(encounter.partOf.reference)) } returns
      immunizationResource

    val updatedTask = runBlocking { groupTask.updateDependentTaskDueDate(defaultRepository) }
    assertEquals(groupTask, updatedTask)
  }
  @Test
  fun `updateDependentTaskDueDate sets executionPeriod start correctly`() {
    coEvery { fhirEngine.get(ResourceType.Task, "650203d2-f327-4eb4-a9fd-741e0ce29c3f") } returns
      dependentTask.apply {
        status = TaskStatus.INPROGRESS
        output =
          listOf(
            Task.TaskOutputComponent(
              CodeableConcept(),
              StringType(
                "{\n" +
                  "          \"reference\": \"Encounter/14e2ae52-32fc-4507-8736-1177cdaafe90\"\n" +
                  "        }"
              )
            )
          )
        input = listOf(Task.ParameterComponent(CodeableConcept(), StringType("28")))
      }
    coEvery {
      fhirEngine.get(ResourceType.Encounter, "14e2ae52-32fc-4507-8736-1177cdaafe90")
    } returns encounter
    coEvery {
      fhirEngine.get(ResourceType.Immunization, "15e2ae52-32fc-4507-8736-1177cdaafe90")
    } returns immunizationResource

    coEvery {
      defaultRepository.loadResource(
        Reference(
          Json.decodeFromString<JsonObject>(dependentTask.output.first().value.toString())[
              REFERENCE]
            ?.jsonPrimitive
            ?.content
        )
      )
    } returns encounter

    coEvery { defaultRepository.loadResource(Reference(encounter.partOf.reference)) } returns
      immunizationResource

    coEvery { defaultRepository.addOrUpdate(addMandatoryTags = true, dependentTask) } just runs
    runBlocking { groupTask.updateDependentTaskDueDate(defaultRepository) }
    assertEquals(
      Date.from(Instant.parse("2021-11-07T00:00:00Z")),
      dependentTask.executionPeriod.start
    )
    coVerify { defaultRepository.addOrUpdate(addMandatoryTags = true, dependentTask) }
  }
}

private fun Date.asYyyyMmDd(): String = this.formatDate(SDF_YYYY_MM_DD)
