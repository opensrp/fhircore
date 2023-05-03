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

package org.smartregister.fhircore.engine.rulesengine

import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.ListResource
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class RulesExecutorTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  @get:Rule(order = 1)
  val coroutineRule = CoroutineTestRule()
  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var rulesFactory: RulesFactory
  private lateinit var rulesExecutor: RulesExecutor

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltAndroidRule.inject()
    rulesFactory =
      spyk(
        RulesFactory(
          context = ApplicationProvider.getApplicationContext(),
          configurationRegistry = configurationRegistry,
          fhirPathDataExtractor = fhirPathDataExtractor,
          dispatcherProvider = coroutineRule.testDispatcherProvider
        )
      )
    rulesExecutor = RulesExecutor(rulesFactory)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun processResourceData() {
    val patientId = "patient id"
    val baseResource = Faker.buildPatient(id = patientId)
    val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<RepositoryResourceData>>()
    val ruleConfig =
      RuleConfig(
        name = "patientName",
        description = "Retrieve patient name",
        actions = listOf("data.put('familyName', fhirPath.extractValue(Group, 'Group.name'))")
      )
    val ruleConfigs = listOf(ruleConfig)

    runBlocking(Dispatchers.Default) {
      val resourceData =
        rulesExecutor.processResourceData(
          baseResourceRulesId = null,
          baseResource = baseResource,
          relatedResourcesMap = relatedRepositoryResourceData,
          secondaryRepositoryResourceData = null,
          ruleConfigs = ruleConfigs,
          params = emptyMap()
        )

      Assert.assertEquals(patientId, resourceData.baseResourceId)
      Assert.assertEquals(ResourceType.Patient, resourceData.baseResourceType)
      Assert.assertNull(resourceData.listResourceDataMap)
      Assert.assertEquals(1, resourceData.computedValuesMap.size)
    }
  }

  @Test
  fun processListResourceData() {
    val registerCard = RegisterCardConfig()
    val viewType = ViewType.CARD
    val listProperties = ListProperties(registerCard = registerCard, viewType = viewType)
    val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<RepositoryResourceData>>()
    val computedValuesMap: Map<String, List<Resource>> = emptyMap()

    runBlocking(Dispatchers.Default) {
      val resourceData =
        rulesExecutor.processListResourceData(
          listProperties = listProperties,
          relatedResourcesMap = relatedRepositoryResourceData,
          computedValuesMap = computedValuesMap
        )

      Assert.assertEquals(0, resourceData.size)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun processListResourceDataWithDataAndNoExpression() {
    val registerCard = RegisterCardConfig()
    val viewType = ViewType.CARD
    val patient = Faker.buildPatient()
    val listResource = ListResource("id", resourceType = ResourceType.Patient)
    val resources = listOf(listResource)
    val listProperties =
      ListProperties(registerCard = registerCard, viewType = viewType, resources = resources)
    val repositoryResourceData = RepositoryResourceData.Search(resource = patient)
    val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<RepositoryResourceData>>()
    val computedValuesMap: Map<String, List<Resource>> = emptyMap()

    relatedRepositoryResourceData[ResourceType.Patient.name] =
      LinkedList<RepositoryResourceData>().apply { add(repositoryResourceData) }

    runBlocking(Dispatchers.Default) {
      val resourceData =
        rulesExecutor.processListResourceData(
          listProperties,
          relatedRepositoryResourceData,
          computedValuesMap
        )

      Assert.assertEquals(1, resourceData.size)
      Assert.assertEquals(patient.id, resourceData.first().baseResourceId)
      Assert.assertEquals(patient.resourceType, resourceData.first().baseResourceType)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun processListResourceDataWithDataAndExpression() {
    val registerCard = RegisterCardConfig()
    val viewType = ViewType.CARD
    val patient = Faker.buildPatient()
    val listResource =
      ListResource(
        "id",
        resourceType = ResourceType.Patient,
        conditionalFhirPathExpression = "Patient.active"
      )
    val resources = listOf(listResource)
    val listProperties =
      ListProperties(registerCard = registerCard, viewType = viewType, resources = resources)
    val repositoryResourceData = RepositoryResourceData.Search(resource = patient)

    val relatedRepositoryResourceData = mutableMapOf<String, LinkedList<RepositoryResourceData>>()
    val computedValuesMap: Map<String, List<Resource>> = emptyMap()

    relatedRepositoryResourceData[ResourceType.Patient.name] =
      LinkedList<RepositoryResourceData>().apply { add(repositoryResourceData) }

    runBlocking(Dispatchers.Default) {
      val resourceData =
        rulesExecutor.processListResourceData(
          listProperties,
          relatedRepositoryResourceData,
          computedValuesMap
        )

      Assert.assertEquals(resourceData.size, 1)
      Assert.assertEquals(patient.id, resourceData.first().baseResourceId)
      Assert.assertEquals(patient.resourceType, resourceData.first().baseResourceType)
    }
  }
  @Test
  fun getRulesFactory() {
    Assert.assertEquals(rulesExecutor.rulesFactory, rulesFactory)
  }
}
