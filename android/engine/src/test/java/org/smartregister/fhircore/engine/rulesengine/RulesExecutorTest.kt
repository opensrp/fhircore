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
import org.hl7.fhir.r4.model.Patient
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
  @get:Rule(order = 1) val coroutineRule = CoroutineTestRule()
  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var rulesFactory: RulesFactory
  private lateinit var rulesExecutor: RulesExecutor

  @Before
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
  fun processResourceData() {
    val patientId = "patient id"
    val baseResource = Faker.buildPatient(id = patientId)
    val relatedRepositoryResourceData: LinkedList<RepositoryResourceData> =
      LinkedList<RepositoryResourceData>()
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
          baseResource,
          relatedRepositoryResourceData,
          ruleConfigs,
          "",
          emptyMap()
        )

      Assert.assertEquals(resourceData.baseResourceId, patientId)
      Assert.assertEquals(resourceData.baseResourceType, ResourceType.Patient)
      Assert.assertNull(resourceData.listResourceDataMap)
      Assert.assertEquals(resourceData.computedValuesMap.size, 1)
    }
  }

  @Test
  fun processListResourceData() {
    val registerCard = RegisterCardConfig()
    val viewType = ViewType.CARD
    val listResource =
      ListResource("id", resourceType = ResourceType.Patient, conditionalFhirPathExpression = "*")
    val resources = listOf(listResource)
    val listProperties =
      ListProperties(registerCard = registerCard, viewType = viewType, resources = resources)
    val repositoryResourceData = RepositoryResourceData(resource = Patient())
    val relatedRepositoryResourceData: LinkedList<RepositoryResourceData> =
      LinkedList<RepositoryResourceData>()
    val computedValuesMap: Map<String, List<Resource>> = emptyMap()

    relatedRepositoryResourceData.add(repositoryResourceData)

    runBlocking(Dispatchers.Default) {
      val resourceData =
        rulesExecutor.processListResourceData(
          listProperties,
          relatedRepositoryResourceData,
          computedValuesMap
        )

      Assert.assertEquals(resourceData.size, 0)
    }
  }

  @Test
  fun getRulesFactory() {
    Assert.assertEquals(rulesExecutor.rulesFactory, rulesFactory)
  }
}
