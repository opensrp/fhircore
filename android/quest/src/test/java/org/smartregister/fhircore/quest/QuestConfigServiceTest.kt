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

@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.smartregister.fhircore.quest

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockkObject
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.SyncStrategy
import org.smartregister.fhircore.engine.util.ApplicationUtil
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestConfigServiceTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  private val application: Application = ApplicationProvider.getApplicationContext()

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private lateinit var configService: ConfigService

  @Before
  fun setUp() {
    hiltRule.inject()
    mockkObject(ApplicationUtil)
    every { ApplicationUtil.application } returns application
    runBlocking {
      configurationRegistry.loadConfigurations(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        appId = APP_DEBUG
      ) {}
    }
    configService =
      QuestConfigService(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
      )

    val careTeamIds = listOf("948", "372")
    sharedPreferencesHelper.write(SyncStrategy.CARETEAM.value, careTeamIds)
    val organizationIds = listOf("400", "105")
    sharedPreferencesHelper.write(SyncStrategy.ORGANIZATION.value, organizationIds)
    val locationIds = listOf("728", "899")
    sharedPreferencesHelper.write(SyncStrategy.LOCATION.value, locationIds)
  }

  @Test
  fun testResourceSyncParam_shouldHaveResourceTypes() {
    val syncParam =
      configService.loadRegistrySyncParams(
        configurationRegistry = configurationRegistry,
        sharedPreferencesHelper = sharedPreferencesHelper
      )
    Assert.assertTrue(syncParam.isNotEmpty())

    val resourceTypes =
      arrayOf(
          ResourceType.CarePlan,
          ResourceType.Condition,
          ResourceType.Encounter,
          ResourceType.Group,
          ResourceType.Library,
          ResourceType.Location,
          ResourceType.Measure,
          ResourceType.Observation,
          ResourceType.Patient,
          ResourceType.PlanDefinition,
          ResourceType.Questionnaire,
          ResourceType.QuestionnaireResponse,
          ResourceType.StructureMap,
          ResourceType.Task,
          ResourceType.Practitioner
        )
        .sorted()

    Assert.assertTrue(resourceTypes.containsAll(syncParam.keys.toTypedArray().sorted()))

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
}
