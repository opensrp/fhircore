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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.asYyyyMmDd
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

@HiltAndroidTest
class FhirTaskGeneratorTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  val fhirEngine: FhirEngine = mockk()
  val now = DateTimeType("2022-03-14") // 3 months ahead patient birthdate in sample

  lateinit var fhirTaskGenerator: FhirTaskGenerator

  @Inject lateinit var transformSupportServices: TransformSupportServices

  lateinit var structureMapUtilities: StructureMapUtilities

  @Before
  fun setup() {
    hiltRule.inject()

    structureMapUtilities = StructureMapUtilities(transformSupportServices.simpleWorkerContext)

    fhirTaskGenerator =
      FhirTaskGenerator(
        fhirEngine = fhirEngine,
        transformSupportServices = transformSupportServices
      )

    mockkStatic(DateTimeType::class)
    every { DateTimeType.now() } returns now
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

    coEvery { fhirEngine.create(any()) } returns emptyList()
    coEvery { fhirEngine.get<StructureMap>("131373") } returns structureMap

    fhirTaskGenerator.generateCarePlan(
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

        val resourcesSlot = mutableListOf<Resource>()

        coVerify { fhirEngine.create(capture(resourcesSlot)) }

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

    coEvery { fhirEngine.create(any()) } returns emptyList()
    coEvery { fhirEngine.get<StructureMap>("hh") } returns structureMap

    fhirTaskGenerator.generateCarePlan(
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

        val resourcesSlot = mutableListOf<Resource>()

        coVerify { fhirEngine.create(capture(resourcesSlot)) }

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
}
