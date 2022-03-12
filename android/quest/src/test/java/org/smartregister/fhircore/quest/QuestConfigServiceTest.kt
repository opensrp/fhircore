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

package org.smartregister.fhircore.quest

import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestConfigServiceTest : RobolectricTest() {

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val repository: DefaultRepository = mockk()

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private lateinit var configService: QuestConfigService

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { repository.searchCompositionByIdentifier(any()) } returns
      "/configs/quest/config_composition.json".parseSampleResourceFromFile() as Composition

    coEvery { repository.getBinary(any()) } returns Binary()
    coEvery { repository.getBinary(any()) } answers
      {
        val idArg = this.args.first().toString()
        val valueArg =
          when (idArg) {
            "62938" -> "application"
            "62940" -> "login"
            "62952" -> "patient_register"
            "87021" -> "patient_task_register"
            "63003" -> "pin"
            "63011" -> "patient_details_view"
            "63007" -> "result_details_navigation"
            "56181" -> "sync"
            else -> null
          }
        Binary().apply { content = "/configs/quest/config_$valueArg.json".readFile().toByteArray() }
      }

    runBlocking { configurationRegistry.loadConfigurations("quest", {}) }

    configService =
      QuestConfigService(
        context = ApplicationProvider.getApplicationContext(),
        sharedPreferencesHelper = sharedPreferencesHelper,
        configurationRegistry = configurationRegistry
      )
  }

  @Test
  fun testResourceSyncParam_shouldHaveResourceTypes() {
    every { sharedPreferencesHelper.read(any(), null) } returns
      UserInfo("ONA-Systems", "105", "Nairobi").encodeJson()

    val syncParam = configService.resourceSyncParams
    Assert.assertTrue(syncParam.isNotEmpty())

    val resourceTypes =
      arrayOf(
          ResourceType.Library,
          ResourceType.StructureMap,
          ResourceType.MedicationRequest,
          ResourceType.QuestionnaireResponse,
          ResourceType.Questionnaire,
          ResourceType.Patient,
          ResourceType.Condition,
          ResourceType.Observation,
          ResourceType.Encounter,
          ResourceType.Task
        )
        .sorted()

    Assert.assertEquals(resourceTypes, syncParam.keys.toTypedArray().sorted())

    syncParam.keys.filter { it.isIn(ResourceType.Binary, ResourceType.StructureMap) }.forEach {
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }

    syncParam.keys.filter { it.isIn(ResourceType.Library) }.forEach {
      Assert.assertTrue(syncParam[it]!!.containsKey("_id"))
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
          ResourceType.Task
        )
      }
      .forEach {
        Assert.assertTrue(syncParam[it]!!.containsKey("subject.organization"))
        Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
      }

    syncParam.keys
      .filter { it.isIn(ResourceType.Observation, ResourceType.QuestionnaireResponse) }
      .forEach {
        Assert.assertTrue(syncParam[it]!!.containsKey("_filter"))
        Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
      }

    syncParam.keys.filter { it.isIn(ResourceType.Questionnaire) }.forEach {
      Assert.assertTrue(syncParam[it]!!.containsKey("publisher"))
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }
  }

  @Test
  fun testResourceSyncParam_allExpressionNull_shouldHaveResourceTypes() {
    every { sharedPreferencesHelper.read(any(), null) } returns
      UserInfo(null, null, null).encodeJson()

    val syncParam = configService.resourceSyncParams
    val resourceTypes =
      arrayOf(
          ResourceType.Library,
          ResourceType.StructureMap,
          ResourceType.MedicationRequest,
          ResourceType.QuestionnaireResponse,
          ResourceType.Questionnaire,
          ResourceType.Patient,
          ResourceType.Condition,
          ResourceType.Observation,
          ResourceType.Encounter,
          ResourceType.Task
        )
        .sorted()

    Assert.assertEquals(resourceTypes, syncParam.keys.toTypedArray().sorted())

    syncParam.keys.filter { it.isIn(ResourceType.Binary, ResourceType.StructureMap) }.forEach {
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }

    syncParam.keys.filter { it.isIn(ResourceType.Library) }.forEach {
      Assert.assertTrue(syncParam[it]!!.containsKey("_id"))
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }

    syncParam.keys.filter { it.isIn(ResourceType.Patient) }.forEach {
      Assert.assertTrue(!syncParam[it]!!.containsKey("organization"))
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }

    syncParam.keys
      .filter {
        it.isIn(
          ResourceType.Encounter,
          ResourceType.Condition,
          ResourceType.MedicationRequest,
          ResourceType.Task
        )
      }
      .forEach {
        Assert.assertTrue(!syncParam[it]!!.containsKey("subject.organization"))
        Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
      }

    syncParam.keys
      .filter { it.isIn(ResourceType.Observation, ResourceType.QuestionnaireResponse) }
      .forEach {
        Assert.assertTrue(!syncParam[it]!!.containsKey("_filter"))
        Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
      }

    syncParam.keys.filter { it.isIn(ResourceType.Questionnaire) }.forEach {
      Assert.assertTrue(!syncParam[it]!!.containsKey("publisher"))
      Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
    }
  }
}
