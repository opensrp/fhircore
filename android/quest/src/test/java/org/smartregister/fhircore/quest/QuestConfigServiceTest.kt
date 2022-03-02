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

  private lateinit var questConfigService: QuestConfigService

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { repository.searchCompositionByIdentifier(any()) } returns
      "/configs/quest/config_composition_quest.json".parseSampleResourceFromFile() as Composition

    coEvery { repository.getBinary(any()) } returns Binary()
    coEvery { repository.getBinary("56181") } returns
      Binary().apply { content = "/configs/config_sync.json".readFile().toByteArray() }

    runBlocking { configurationRegistry.loadConfigurations("quest", {}) }

    questConfigService =
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

    val syncParam = questConfigService.resourceSyncParams
    Assert.assertTrue(syncParam.isNotEmpty())

    val resourceTypes =
      arrayOf(
          ResourceType.Binary,
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
      Assert.assertTrue(syncParam[it]!!.isEmpty())
    }

    syncParam.keys.filter { !it.isIn(ResourceType.Binary, ResourceType.StructureMap) }.forEach {
      Assert.assertTrue(syncParam[it]!!.isNotEmpty())
    }
  }

  @Test
  fun testResourceSyncParam_organization_and_publisher_Null_shouldHaveEmptyMapForRelevantResources() {
    every { sharedPreferencesHelper.read(any(), null) } returns
      UserInfo(null, null, null).encodeJson()

    val syncParam = questConfigService.resourceSyncParams
    val resourceTypes =
      arrayOf(
          ResourceType.Binary,
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

    syncParam.keys.filter { !it.isIn(ResourceType.Library) }.forEach {
      Assert.assertTrue(syncParam[it]!!.isEmpty())
    }
  }
}
