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

import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.isIn

@HiltAndroidTest
@Ignore("TODO investigate, fails on CI")
class ConfigServiceTest : RobolectricTest() {
  val configService = AppConfigService(ApplicationProvider.getApplicationContext())
  val configurationRegistry = Faker.buildTestConfigurationRegistry(mockk())

  @Test
  fun testLoadSyncParamsShouldLoadFromConfiguration() {
    val syncParam =
      configService.loadRegistrySyncParams(configurationRegistry, UserInfo("samplep", "sampleo"))

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
}
