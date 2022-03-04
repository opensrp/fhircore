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

package org.smartregister.fhircore.engine.app.fakes

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest.Companion.readFile
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString

object Faker {
  fun loadTestConfigurationRegistryData(
    defaultRepository: DefaultRepository,
    configurationRegistry: ConfigurationRegistry
  ) {
    coEvery { defaultRepository.searchCompositionByIdentifier(any()) } returns
      "/configs/config_composition.json".readFile().decodeResourceFromString() as Composition

    coEvery { defaultRepository.getBinary(any()) } answers
      {
        val idArg = this.args.first().toString().replace("b_", "")
        Binary().apply {
          content = "/configs/${idArg}_configurations.json".readFile().toByteArray()
        }
      }

    runBlocking { configurationRegistry.loadConfigurations(appId = "appId") {} }
  }

  fun buildTestConfigurationRegistry(defaultRepository: DefaultRepository): ConfigurationRegistry {
    val configurationRegistry = spyk(ConfigurationRegistry(mockk(), mockk(), defaultRepository))

    loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    return configurationRegistry
  }
}
