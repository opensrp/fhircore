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

package org.smartregister.fhircore.engine.app

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.impl.WorkManagerImpl
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.SyncJob
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers.setStaticField
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.sync.SyncStrategy
import org.smartregister.fhircore.engine.task.FhirTaskPlanWorker
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.isIn

@HiltAndroidTest
class ConfigServiceTest : RobolectricTest() {

  @get:Rule var hiltRule = HiltAndroidRule(this)

  private var application: Context = ApplicationProvider.getApplicationContext()
  private val configService = AppConfigService(ApplicationProvider.getApplicationContext())
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Before
  fun setUp() {
    hiltRule.inject()
    runBlocking { configurationRegistry.loadConfigurations("app/debug", application) }

    val careTeamIds = listOf("948", "372")
    sharedPreferencesHelper.write(SyncStrategy.CARETEAM.value, careTeamIds)
    val organizationIds = listOf("400", "105")
    sharedPreferencesHelper.write(SyncStrategy.ORGANIZATION.value, organizationIds)
    val locationIds = listOf("728", "899")
    sharedPreferencesHelper.write(SyncStrategy.LOCATION.value, locationIds)
  }

  @Test
  fun testLoadSyncParamsShouldLoadFromConfiguration() {
    val syncParam =
      configService.loadRegistrySyncParams(configurationRegistry, sharedPreferencesHelper)

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
    val syncJob = mockk<SyncJob>()
    val configurationRegistry = mockk<ConfigurationRegistry>()
    val syncBroadcaster = mockk<SyncBroadcaster>()

    every { syncJob.stateFlow() } returns mockk()
    every { syncJob.poll(any(), FhirSyncWorker::class.java) } returns mockk()
    coEvery { syncBroadcaster.sharedSyncStatus } returns mockk()

    configService.schedulePeriodicSync(syncJob, configurationRegistry, syncBroadcaster)
    shadowOf(Looper.getMainLooper()).idle()

    verify { syncJob.poll(any(), eq(FhirSyncWorker::class.java)) }
  }

  @Test
  fun testSchedulePlanShouldEnqueueUniquePeriodicWork() {
    val workManager = mockk<WorkManagerImpl>()
    setStaticField(WorkManagerImpl::class.java, "sDelegatedInstance", workManager)

    every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } returns mockk()
    configService.schedulePlan(mockk())

    verify {
      workManager.enqueueUniquePeriodicWork(
        eq(FhirTaskPlanWorker.WORK_ID),
        eq(ExistingPeriodicWorkPolicy.REPLACE),
        any()
      )
    }
  }

  @Test
  fun testUnschedulePlanShouldCancelUniqueWork() {
    val workManager = mockk<WorkManagerImpl>()
    setStaticField(WorkManagerImpl::class.java, "sDelegatedInstance", workManager)

    every { workManager.cancelUniqueWork(any()) } returns mockk()
    configService.unschedulePlan(mockk())

    verify { workManager.cancelUniqueWork(eq(FhirTaskPlanWorker.WORK_ID)) }
  }
}
