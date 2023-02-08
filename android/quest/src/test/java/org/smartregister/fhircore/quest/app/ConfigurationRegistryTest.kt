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

package org.smartregister.fhircore.quest.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StructureMap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class ConfigurationRegistryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) var coroutinesTestRule = CoroutineTestRule()
  @Inject lateinit var gson: Gson
  private lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var fhirEngine: FhirEngine
  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private val secureSharedPreference = mockk<SecureSharedPreference>()
  private val configService = mockk<ConfigService>()
  private val application: Context = ApplicationProvider.getApplicationContext()
  private val fhirResourceService = mockk<FhirResourceService>()
  private val fhirResourceDataSource = spyk(FhirResourceDataSource(fhirResourceService))

  @Before
  fun setUp() {
    hiltRule.inject()
    sharedPreferencesHelper = mockk()
    fhirEngine = mockk()

    configurationRegistry =
      ConfigurationRegistry(
        fhirEngine = fhirEngine,
        fhirResourceDataSource = fhirResourceDataSource,
        sharedPreferencesHelper = sharedPreferencesHelper,
        dispatcherProvider = coroutineTestRule.testDispatcherProvider,
        configService = configService,
        json = Faker.json
      )
    coEvery { fhirEngine.create(any()) } answers { listOf() }
    runBlocking { configurationRegistry.loadConfigurations("app/debug", application) }
  }

  @Test
  fun testFetchNonWorkflowConfigurations() = runBlocking {
    val composition =
      Composition().apply {
        addSection().apply {
          this.focus =
            Reference().apply {
              reference = "StructureMap/123456"
              identifier = Identifier().apply { value = "012345" }
            }
        }
      }

    val bundle =
      org.hl7.fhir.r4.model.Bundle().apply {
        addEntry().apply {
          this.resource = StructureMap().apply { StructureMap@ this.id = "123456" }
        }
      }

    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"
    coEvery { fhirEngine.search<Composition>(any()) } returns listOf(composition)
    coEvery { fhirEngine.get(any(), any()) } throws ResourceNotFoundException("Exce", "Exce")

    coEvery { configurationRegistry.fhirResourceDataSource.getResource(any()) } returns bundle
    every { sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null) } returns "demo"

    configurationRegistry.fetchNonWorkflowConfigResources()
    coVerify { configurationRegistry.fhirResourceDataSource.getResource(any()) }
    coVerify { configurationRegistry.create(any()) }
  }

  @Test
  fun testFetchListResource() = runBlocking {
    val composition =
      Composition().apply {
        addSection().apply {
          this.focus =
            Reference().apply {
              reference = "List/123456"
              identifier = Identifier().apply { value = "012345" }
            }
        }
      }

    val bundle =
      org.hl7.fhir.r4.model.Bundle().apply {
        addEntry().apply {
          this.resource = ListResource().apply { ListResource@ this.id = "123456" }
        }
      }

    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"
    coEvery { fhirEngine.search<Composition>(any()) } returns listOf(composition)
    coEvery { fhirEngine.get(any(), any()) } throws ResourceNotFoundException("Exce", "Exce")

    coEvery { configurationRegistry.fhirResourceDataSource.getResource(any()) } returns bundle
    every { sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null) } returns "demo"

    configurationRegistry.fetchNonWorkflowConfigResources()
    coVerify { configurationRegistry.fhirResourceDataSource.getResource(any()) }
    coVerify { configurationRegistry.create(any()) }
  }
}
