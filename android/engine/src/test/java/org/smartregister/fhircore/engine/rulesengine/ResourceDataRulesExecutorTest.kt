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

package org.smartregister.fhircore.engine.rulesengine

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.ListResourceConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.SortConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.rulesengine.services.LocationService
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class ResourceDataRulesExecutorTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @kotlinx.coroutines.ExperimentalCoroutinesApi
  @get:Rule(order = 1)
  val coroutineRule = CoroutineTestRule()

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var locationService: LocationService
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var rulesFactory: RulesFactory
  private lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor

  @Inject lateinit var fhirContext: FhirContext
  private lateinit var defaultRepository: DefaultRepository

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltAndroidRule.inject()
    defaultRepository = mockk(relaxed = true)
    rulesFactory =
      spyk(
        RulesFactory(
          context = ApplicationProvider.getApplicationContext(),
          configurationRegistry = configurationRegistry,
          fhirPathDataExtractor = fhirPathDataExtractor,
          dispatcherProvider = dispatcherProvider,
          locationService = locationService,
          fhirContext = fhirContext,
          defaultRepository = defaultRepository,
        ),
      )
    resourceDataRulesExecutor = ResourceDataRulesExecutor(rulesFactory)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun processResourceData() {
    val patientId = "patient id"
    val baseResource = Faker.buildPatient(id = patientId)
    val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<Resource>>()
    val ruleConfig =
      RuleConfig(
        name = "patientName",
        description = "Retrieve patient name",
        actions = listOf("data.put('familyName', fhirPath.extractValue(Group, 'Group.name'))"),
      )
    val ruleConfigs = listOf(ruleConfig)

    runBlocking(Dispatchers.Default) {
      val resourceData =
        resourceDataRulesExecutor.processResourceData(
          repositoryResourceData =
            RepositoryResourceData(
              resourceRulesEngineFactId = null,
              resource = baseResource,
              relatedResourcesMap = relatedRepositoryResourceData,
            ),
          ruleConfigs = ruleConfigs,
          params = emptyMap(),
        )

      Assert.assertEquals(patientId, resourceData.baseResourceId)
      Assert.assertEquals(ResourceType.Patient, resourceData.baseResourceType)
      Assert.assertNull(resourceData.listResourceDataMap)
      Assert.assertEquals(1, resourceData.computedValuesMap.size)
    }
  }

  @Test
  fun processListResourceData() {
    runTest {
      val registerCard = RegisterCardConfig()
      val viewType = ViewType.CARD
      val listProperties = ListProperties(registerCard = registerCard, viewType = viewType)
      val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<Resource>>()
      val computedValuesMap: Map<String, List<Resource>> = emptyMap()
      val listResourceDataStateMap = mutableStateMapOf<String, SnapshotStateList<ResourceData>>()
      resourceDataRulesExecutor.processListResourceData(
        listProperties = listProperties,
        relatedResourcesMap = relatedRepositoryResourceData,
        computedValuesMap = computedValuesMap,
        listResourceDataStateMap = listResourceDataStateMap,
      )
      Assert.assertEquals(0, listResourceDataStateMap.size)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun processListResourceDataWithDataAndNoExpression() {
    runTest {
      val registerCard = RegisterCardConfig()
      val viewType = ViewType.CARD
      val patient = Faker.buildPatient(id = "patientId", given = "Betty", family = "Jones")
      val anotherPatient =
        Faker.buildPatient(id = "anotherPatient", given = "Abel", family = "Mandela")
      val listResource =
        ListResourceConfig(
          "id",
          resourceType = ResourceType.Patient,
          sortConfig =
            SortConfig(
              dataType = Enumerations.DataType.STRING,
              fhirPathExpression = "Patient.name.given",
            ),
        )
      val listProperties =
        ListProperties(
          registerCard = registerCard,
          viewType = viewType,
          resources = listOf(listResource),
        )
      val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<Resource>>()
      val computedValuesMap: Map<String, List<Resource>> = emptyMap()

      relatedRepositoryResourceData[ResourceType.Patient.name] =
        LinkedList<Resource>().apply {
          add(patient)
          add(anotherPatient)
        }
      val listResourceDataStateMap = mutableStateMapOf<String, SnapshotStateList<ResourceData>>()
      resourceDataRulesExecutor.processListResourceData(
        listProperties = listProperties,
        relatedResourcesMap = relatedRepositoryResourceData,
        computedValuesMap = computedValuesMap,
        listResourceDataStateMap = listResourceDataStateMap,
      )
      val snapshotStateList = listResourceDataStateMap[listProperties.id]
      Assert.assertNotNull(snapshotStateList)
      Assert.assertEquals(2, snapshotStateList?.size)

      // List data sorted by Patient name
      val firstResourceData = snapshotStateList?.first()
      Assert.assertEquals(anotherPatient.id, firstResourceData?.baseResourceId)
      Assert.assertEquals(anotherPatient.resourceType, firstResourceData?.baseResourceType)

      val lastResourceData = snapshotStateList?.last()
      Assert.assertEquals(patient.id, lastResourceData?.baseResourceId)
      Assert.assertEquals(patient.resourceType, lastResourceData?.baseResourceType)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun processListResourceDataWithDataAndExpression() {
    runTest {
      val registerCard = RegisterCardConfig()
      val viewType = ViewType.CARD
      val patient = Faker.buildPatient()
      val listResource =
        ListResourceConfig(
          "id",
          resourceType = ResourceType.Patient,
          conditionalFhirPathExpression = "Patient.active",
        )
      val resources = listOf(listResource)
      val listProperties =
        ListProperties(registerCard = registerCard, viewType = viewType, resources = resources)

      val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<Resource>>()
      val computedValuesMap: Map<String, List<Resource>> = emptyMap()

      relatedRepositoryResourceData[ResourceType.Patient.name] =
        LinkedList<Resource>().apply { add(patient) }

      val listResourceDataStateMap = mutableStateMapOf<String, SnapshotStateList<ResourceData>>()

      resourceDataRulesExecutor.processListResourceData(
        listProperties = listProperties,
        relatedResourcesMap = relatedRepositoryResourceData,
        computedValuesMap = computedValuesMap,
        listResourceDataStateMap = listResourceDataStateMap,
      )

      val snapshotStateList = listResourceDataStateMap[listProperties.id]
      Assert.assertNotNull(snapshotStateList)
      Assert.assertEquals(snapshotStateList?.size, 1)

      val resourceData = snapshotStateList?.first()!!
      Assert.assertEquals(patient.id, resourceData.baseResourceId)
      Assert.assertEquals(patient.resourceType, resourceData.baseResourceType)
      Assert.assertEquals(0, resourceData.computedValuesMap.size)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testProcessListResourceDataWithRelatedResourcesAndFhirPathExpressionShouldReturnEmptyComputedValuesMap() {
    runTest {
      val registerCard = RegisterCardConfig()
      val viewType = ViewType.CARD
      val patient = Faker.buildPatient()
      val listResource =
        ListResourceConfig(
          "id",
          resourceType = ResourceType.Patient,
          conditionalFhirPathExpression = "Patient.active",
          relatedResources =
            listOf(
              ListResourceConfig(
                null,
                resourceType = ResourceType.Task,
                fhirPathExpression = "Task.for.reference",
              ),
            ),
        )
      val listProperties =
        ListProperties(
          registerCard = registerCard,
          viewType = viewType,
          resources = listOf(listResource),
        )

      val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<Resource>>()

      relatedRepositoryResourceData[ResourceType.Patient.name] =
        LinkedList<Resource>().apply { add(patient) }

      val listResourceDataStateMap = mutableStateMapOf<String, SnapshotStateList<ResourceData>>()

      resourceDataRulesExecutor.processListResourceData(
        listProperties = listProperties,
        relatedResourcesMap = relatedRepositoryResourceData,
        computedValuesMap = emptyMap(),
        listResourceDataStateMap = listResourceDataStateMap,
      )

      val snapshotStateList = listResourceDataStateMap[listProperties.id]
      val resourceData = snapshotStateList?.first()
      Assert.assertNotNull(resourceData)
      Assert.assertEquals(0, resourceData?.computedValuesMap?.size)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testProcessListResourceDataRelatedRepositoryResourceDataWithRelatedResourcesAndFhirPathExpressionShouldPopulatesComputedValuesMap() {
    val allTasks = "allTasks"
    val patientReadyTasks = "patientReadyTasks"
    runTest {
      // Provide register card with rules to be executed for LIST view.
      // The LIST uses Patient as its base resource and loads Patient's Tasks as related resource
      val registerCard =
        RegisterCardConfig(
          rules =
            listOf(
              RuleConfig(
                name = "taskId",
                actions =
                  listOf(
                    "data.put('taskId', fhirPath.extractValue(patientReadyTasks.get(0), 'Task.id'))",
                  ),
              ),
            ),
        )

      val patient = Faker.buildPatient()
      val anotherPatient = Faker.buildPatient("anotherId")
      val listResource =
        ListResourceConfig(
          resourceType = ResourceType.Patient,
          conditionalFhirPathExpression = "Patient.active",
          relatedResources =
            listOf(
              ListResourceConfig(
                id = patientReadyTasks,
                resourceType = ResourceType.Task,
                conditionalFhirPathExpression = "Task.status = 'ready'",
                fhirPathExpression = "Task.for.reference",
                relatedResourceId = allTasks,
              ),
            ),
        )
      val listProperties =
        ListProperties(
          id = "listId",
          registerCard = registerCard,
          viewType = ViewType.LIST,
          resources = listOf(listResource),
        )

      val tasks =
        listOf(
          Task().apply {
            id = "task1"
            `for` = patient.id.asReference(ResourceType.Patient)
            status = Task.TaskStatus.COMPLETED
          },
          Task().apply {
            id = "task2"
            `for` = patient.id.asReference(ResourceType.Patient)
            status = Task.TaskStatus.READY
          },
          Task().apply {
            id = "task3"
            `for` = anotherPatient.id.asReference(ResourceType.Patient)
            status = Task.TaskStatus.READY
          },
        )
      val relatedRepositoryResourceData = mutableMapOf<String, List<Resource>>()
      relatedRepositoryResourceData.apply {
        put(ResourceType.Patient.name, listOf(patient))
        put(allTasks, tasks)
      }

      val listResourceDataStateMap = mutableStateMapOf<String, SnapshotStateList<ResourceData>>()

      resourceDataRulesExecutor.processListResourceData(
        listProperties = listProperties,
        relatedResourcesMap = relatedRepositoryResourceData,
        computedValuesMap = emptyMap(),
        listResourceDataStateMap = listResourceDataStateMap,
      )

      val snapshotStateList = listResourceDataStateMap[listProperties.id]
      Assert.assertNotNull(snapshotStateList)

      // List should contain one resource data with Patient as base resource
      val resourceData = snapshotStateList?.first()
      Assert.assertEquals(patient.id, resourceData?.baseResourceId)
      Assert.assertEquals(patient.resourceType, resourceData?.baseResourceType)

      // Only ready Tasks FOR the patient should be filtered; expecting just one
      val resourceDataComputedValuesMap = resourceData?.computedValuesMap
      val taskId = resourceDataComputedValuesMap?.get("taskId")
      Assert.assertEquals("task2", taskId)
    }
  }

  @Test
  fun getRulesFactory() {
    Assert.assertEquals(resourceDataRulesExecutor.rulesFactory, rulesFactory)
  }
}
