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

package org.smartregister.fhircore.anc.app.fakes

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.smartregister.fhircore.anc.robolectric.RobolectricTest.Companion.readFile
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.extractId

object Faker {

  fun loadTestConfigurationRegistryData(
    appId: String,
    defaultRepository: DefaultRepository,
    configurationRegistry: ConfigurationRegistry
  ) {
    val composition =
      "/configs/$appId/config_composition.json".readFile().decodeResourceFromString() as Composition
    coEvery { defaultRepository.searchCompositionByIdentifier(appId) } returns composition

    coEvery { defaultRepository.getBinary(any()) } answers
      {
        val section =
          composition.section.first { it.focus.extractId() == this.args.first().toString() }
        Binary().apply {
          content =
            "/configs/$appId/config_${section.focus.identifier.value}.json".readFile().toByteArray()
        }
      }

    runBlocking { configurationRegistry.loadConfigurations(appId) {} }
  }

  fun buildTestConfigurationRegistry(
    appId: String,
    defaultRepository: DefaultRepository
  ): ConfigurationRegistry {
    val configurationRegistry = spyk(ConfigurationRegistry(mockk(), mockk(), defaultRepository))

    loadTestConfigurationRegistryData(appId, defaultRepository, configurationRegistry)

    return configurationRegistry
  }
}
