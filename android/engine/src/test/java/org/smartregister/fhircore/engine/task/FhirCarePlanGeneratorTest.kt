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

package org.smartregister.fhircore.engine.task

import androidx.work.WorkManager
import androidx.work.WorkRequest
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
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
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
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

@HiltAndroidTest
class FhirCarePlanGeneratorTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  val fhirEngine: FhirEngine = mockk()
  val now = DateTimeType("2022-03-14") // 3 months ahead patient birthdate in sample

  lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator

  @Inject lateinit var transformSupportServices: TransformSupportServices

  @Inject lateinit var fhirPathEngine: FHIRPathEngine

  lateinit var structureMapUtilities: StructureMapUtilities

  private val defaultRepository: DefaultRepository = mockk()

  @Before
  fun setup() {
    hiltRule.inject()

    structureMapUtilities = StructureMapUtilities(transformSupportServices.simpleWorkerContext)
    val workManager = mockk<WorkManager>()

    fhirCarePlanGenerator =
      FhirCarePlanGenerator(
        fhirEngine = fhirEngine,
        transformSupportServices = transformSupportServices,
        fhirPathEngine = fhirPathEngine,
        defaultRepository = defaultRepository,
        workManager = workManager
      )

    mockkStatic(DateTimeType::class)
    every { DateTimeType.now() } returns now
    every { workManager.enqueue(any<WorkRequest>()) } returns mockk()
  }

  @After
  fun cleanup() {
    unmockkStatic(DateTimeType::class)
  }

  @Test
  fun testGenerateCarePlanForPatient() = runTest {
    val plandefinition =
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
        plandefinition,
        patient,
        Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = patient })
      )!!
      .also { println(it.encodeResourceToString()) }
      .also {
        val carePlan = it
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
            task.status == Task.TaskStatus.REQUESTED &&
              LocalDate.parse(task.executionPeriod.end.asYyyyMmDd()).let { localDate ->
                localDate.dayOfMonth == localDate.lengthOfMonth()
              }
          }

        val task1 = resourcesSlot[1] as Task
        Assert.assertEquals(Task.TaskStatus.REQUESTED, task1.status)
        // TODO Fix issue with task start date updating relative to today's date
        Assert.assertTrue(task1.executionPeriod.start.makeItReadable().isNotEmpty())
        // Assert.assertEquals("01-Apr-2022", task1.executionPeriod.start.makeItReadable())
        // Assert.assertEquals("30-Apr-2022", task1.executionPeriod.end.makeItReadable())
      }
  }

  @Test
  fun testGenerateCarePlanForGroup() = runTest {
    val plandefinition =
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
        plandefinition,
        group,
        Bundle()
          .addEntry(
            Bundle.BundleEntryComponent().apply {
              resource = Encounter().apply { status = Encounter.EncounterStatus.FINISHED }
            }
          )
      )!!
      .also { println(it.encodeResourceToString()) }
      .also {
        val carePlan = it
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
            task.status == Task.TaskStatus.REQUESTED &&
              LocalDate.parse(task.executionPeriod.end.asYyyyMmDd()).let { localDate ->
                localDate.dayOfMonth == localDate.lengthOfMonth()
              }
          }

        val task1 = resourcesSlot[1] as Task
        Assert.assertEquals(Task.TaskStatus.REQUESTED, task1.status)
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
      .also {
        Assert.assertNull(it)

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

    coEvery { fhirEngine.search<CarePlan>(any()) } returns
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
        val carePlan = carePlan
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

    coEvery { fhirEngine.search<CarePlan>(any()) } returns listOf()

    fhirCarePlanGenerator.generateOrUpdateCarePlan(
        plandefinition,
        patient,
        Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = questionnaireResponse })
      )
      .also {
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

            // first visit is lmp plus 1 month and subsequent visit are every month after that until
            // delivery
            val pregnancyStart = lmp.clone() as Date
            this.forEachIndexed { index, task ->
              assertEquals(
                pregnancyStart.plusMonths(index + 1).asYyyyMmDd(),
                task.executionPeriod.start.asYyyyMmDd()
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
          5
        ) // 5 visits for each month of ANC

        resourcesSlot.forEach { println(it.encodeResourceToString()) }
        // 5 visits and tasks
        assertEquals(5, carePlan.activityFirstRep.outcomeReference.size)

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

            // first visit is lmp plus 1 month and subsequent visit are every month after that until
            // delivery
            // skip tasks for past 3 months of late registration
            val ancStart = lmp.plusMonths(3).clone() as Date
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
          .also {
            assertTrue(it.all { it.status == TaskStatus.READY })
            assertTrue(it.all { it.`for`.reference == patient.asReference().reference })
          }
          // first 5 tasks are anc visit for each month of pregnancy
          .take(1)
          .run {
            assertTrue(this.all { it.reasonReference.reference == "Questionnaire/132155" })
            assertTrue(
              this.all { it.basedOn.first().reference == carePlan.asReference().reference }
            )

            this.forEachIndexed { index, task ->
              assertEquals(
                Date().plusMonths(1).asYyyyMmDd(),
                task.executionPeriod.start.asYyyyMmDd()
              )
            }
          }
      }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
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
        ) // TODO????

        resourcesSlot
          .filter { res -> res.resourceType == ResourceType.Task }
          .map {
            println(it.encodeResourceToString())
            it as Task
          }
          .also {
            // TODO validate whole schedule
            //            it.last().let { task ->
            //              Assert.assertTrue(task.description == "HPV(2) at 9.5 years Vaccine")
            //              Assert.assertTrue(
            //                task.reasonCode.text ==
            //                  "Administration of vaccine to produce active immunity (procedure)"
            //              )
            //              Assert.assertTrue(task.reasonCode.hasCoding())
            //              Assert.assertTrue(task.reasonCode.coding.get(0).code == "33879002")
            //            }
          }
          .also {
            Assert.assertTrue(
              it.all {
                it.reasonReference.reference == "Questionnaire/9b1aa23b-577c-4fb2-84e3-591e6facaf82"
              }
            )
          }
          .all { task -> task.status == Task.TaskStatus.REQUESTED /*&&
              LocalDate.parse(task.executionPeriod.end.asYyyyMmDd()).let { localDate ->
                localDate.dayOfMonth == localDate.lengthOfMonth()
              }*/ }

        /* val task1 = resourcesSlot[1] as Task
        Assert.assertEquals(Task.TaskStatus.REQUESTED, task1.status)
        Assert.assertTrue(task1.executionPeriod.start.makeItReadable().isNotEmpty())
        Assert.assertTrue(task1.description.isNotEmpty())
        Assert.assertTrue(task1.description == "OPV at Birth Vaccine")*/
      }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
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
      .also {
        val carePlan = it
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
  fun `transitionTaskTo should update task status`() = runTest {
    coEvery { fhirEngine.get(ResourceType.Task, "12345") } returns Task().apply { id = "12345" }
    coEvery { defaultRepository.addOrUpdate(any(), any()) } just Runs

    fhirCarePlanGenerator.transitionTaskTo("12345", TaskStatus.COMPLETED)

    val task = slot<Task>()
    coVerify { defaultRepository.addOrUpdate(any(), capture(task)) }

    Assert.assertEquals(TaskStatus.COMPLETED, task.captured.status)
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
}

private fun Date.asYyyyMmDd(): String = this.formatDate(SDF_YYYY_MM_DD)
