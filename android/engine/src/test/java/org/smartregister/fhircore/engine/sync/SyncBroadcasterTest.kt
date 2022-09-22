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

package org.smartregister.fhircore.engine.sync

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.SyncJob
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.isIn

@ExperimentalCoroutinesApi
@HiltAndroidTest
class SyncBroadcasterTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var configService: ConfigService

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private val syncJob = mockk<SyncJob>(relaxed = true)

  private val fhirEngine = mockk<FhirEngine>()

  private val syncListenerManager = spyk<SyncListenerManager>()

  private lateinit var syncBroadcaster: SyncBroadcaster

  @Before
  fun setup() {
    hiltAndroidRule.inject()
    MockKAnnotations.init(this)
    syncBroadcaster =
      spyk(
        SyncBroadcaster(
          configurationRegistry = configurationRegistry,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configService = configService,
          syncJob = syncJob,
          fhirEngine = fhirEngine,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
          syncListenerManager = syncListenerManager
        )
      )
  }

  @Test fun testRunSyncWorksAsExpected() = runTest {}

  @Test
  fun testLoadSyncParamsShouldLoadFromConfiguration() {

    val paramsMap =
      mutableMapOf<String, List<String>>().apply {
        put(
          SharedPreferenceKey.PRACTITIONER_DETAILS_ORGANIZATION_IDS.name,
          listOf("Organization/105")
        )
      }
    val syncParam = syncBroadcaster.loadSyncParams(paramsMap)

    Assert.assertTrue(syncParam.isNotEmpty())

    val resourceTypes =
      arrayOf(
          ResourceType.CarePlan,
          ResourceType.Condition,
          ResourceType.Encounter,
          ResourceType.Group,
          ResourceType.Library,
          ResourceType.Measure,
          ResourceType.Observation,
          ResourceType.Patient,
          ResourceType.PlanDefinition,
          ResourceType.Questionnaire,
          ResourceType.QuestionnaireResponse,
          ResourceType.StructureMap,
          ResourceType.Task
        )
        .sorted()

    Assert.assertEquals(resourceTypes, syncParam.keys.toTypedArray().sorted())

    syncParam.keys.filter { it.isIn(ResourceType.Binary, ResourceType.StructureMap) }.forEach {
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }

    syncParam.keys.filter { it.isIn(ResourceType.Patient) }.forEach {
      Assert.assertTrue(syncParam[it]!!.containsKey("organization"))
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }

    syncParam.keys
      .filter {
        it.isIn(
          ResourceType.Encounter,
          ResourceType.Condition,
          ResourceType.MedicationRequest,
          ResourceType.Task,
          ResourceType.QuestionnaireResponse,
          ResourceType.Observation
        )
      }
      .forEach {
        Assert.assertTrue(syncParam[it]!!.containsKey("subject.organization"))
        Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
      }

    syncParam.keys.filter { it.isIn(ResourceType.Questionnaire) }.forEach {
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }
  }

  @Test
  fun testSchedulePeriodicSyncShouldPoll() = runTest {
    syncBroadcaster.schedulePeriodicSync()
    verify { syncJob.poll<FhirSyncWorker>(periodicSyncConfiguration = any(), clazz = any()) }
  }
}
