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

package org.smartregister.fhircore.quest.integration

import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.Bundle
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService

object Faker {

  private const val APP_DEBUG = "app/debug"

  fun buildTestConfigurationRegistry(): ConfigurationRegistry {
    val fhirResourceService = mockk<FhirResourceService>()
    val fhirResourceDataSource = spyk(FhirResourceDataSource(fhirResourceService))
    coEvery { fhirResourceService.getResource(any()) } returns Bundle()

    val json = Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
      isLenient = true
      useAlternativeNames = true
    }

    val configurationRegistry =
      spyk(
        ConfigurationRegistry(
          fhirEngine = mockk(),
          fhirResourceDataSource = fhirResourceDataSource,
          sharedPreferencesHelper = mockk(),
          dispatcherProvider = mockk(),
          configService = mockk(),
          json = json,
        ),
      )

    coEvery { configurationRegistry.addOrUpdate(any()) } just runs

    runBlocking {
      configurationRegistry.loadConfigurations(
        appId = APP_DEBUG,
        context = InstrumentationRegistry.getInstrumentation().targetContext,
      ) {}
    }

    return configurationRegistry
  }
}
