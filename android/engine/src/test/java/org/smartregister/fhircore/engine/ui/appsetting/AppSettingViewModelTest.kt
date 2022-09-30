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

package org.smartregister.fhircore.engine.ui.appsetting

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.util.JsonUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import java.util.Base64
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.register.FhirResourceConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.register.ResourceConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class AppSettingViewModelTest : RobolectricTest() {

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val defaultRepository = mockk<DefaultRepository>()
  private val fhirResourceDataSource = mockk<FhirResourceDataSource>()
  private val sharedPreferencesHelper = mockk<SharedPreferencesHelper>()
  private val appSettingViewModel =
    spyk(AppSettingViewModel(fhirResourceDataSource, defaultRepository, sharedPreferencesHelper))

  @Test
  fun testOnApplicationIdChanged() {
    appSettingViewModel.onApplicationIdChanged("appId")
    Assert.assertNotNull(appSettingViewModel.appId.value)
    Assert.assertEquals("appId", appSettingViewModel.appId.value)
  }

  @Test
  fun testLoadConfigurations() = runBlockingTest {
    coEvery { appSettingViewModel.fhirResourceDataSource.loadData(any()) } returns
      Bundle().apply { addEntry().resource = Composition() }
    coEvery { appSettingViewModel.defaultRepository.create(any()) } returns emptyList()

    appSettingViewModel.loadConfigurations(true)
    Assert.assertNotNull(appSettingViewModel.loadConfigs.value)
    Assert.assertEquals(true, appSettingViewModel.loadConfigs.value)
  }

  @Test
  @Ignore("Fix failing test")
  fun testFetchConfigurations() = runBlockingTest {
    coEvery { appSettingViewModel.fhirResourceDataSource.loadData(any()) } returns
      Bundle().apply {
        addEntry().resource =
          Composition().apply {
            addSection().apply { this.focus = Reference().apply { reference = "Binary/123" } }
          }
      }
    coEvery { appSettingViewModel.defaultRepository.create(any()) } returns emptyList()

    appSettingViewModel.fetchConfigurations("app", ApplicationProvider.getApplicationContext())

    coVerify { appSettingViewModel.fhirResourceDataSource.loadData(any()) }
    coVerify { appSettingViewModel.defaultRepository.create(any()) }
  }

  @Test
  fun `fetchConfigurations() should save shared preferences for patient related resource types`() =
      runBlockingTest {
    coEvery { appSettingViewModel.fetchComposition(any(), any()) } returns
      Composition().apply {
        addSection().apply {
          this.focus =
            Reference().apply {
              reference = "Binary/123"
              identifier = Identifier().apply { value = "register-test" }
            }
        }
      }
    coEvery { fhirResourceDataSource.loadData(any()) } returns
      Bundle().apply {
        addEntry().resource =
          Binary().apply {
            data =
              Base64.getEncoder()
                .encode(
                  JsonUtil.serialize(
                      RegisterConfiguration(
                        id = "1",
                        appId = "a",
                        configType = "register",
                        fhirResource =
                          FhirResourceConfig(
                            baseResource = ResourceConfig(resource = "Patient"),
                            relatedResources =
                              listOf(
                                ResourceConfig(resource = "Encounter"),
                                ResourceConfig(resource = "Task")
                              )
                          )
                      )
                    )
                    .encodeToByteArray()
                )
          }
      }
    coEvery { defaultRepository.create(any()) } returns emptyList()
    coEvery { appSettingViewModel.saveSyncSharedPreferences(any()) } just runs

    appSettingViewModel.fetchConfigurations("app", ApplicationProvider.getApplicationContext())

    val slot = slot<List<ResourceType>>()

    coVerify { appSettingViewModel.fetchComposition(any(), any()) }
    coVerify { fhirResourceDataSource.loadData(any()) }
    coVerify { defaultRepository.create(any()) }
    coVerify { appSettingViewModel.saveSyncSharedPreferences(capture(slot)) }

    assertEquals(
      listOf(ResourceType.Patient, ResourceType.Encounter, ResourceType.Task),
      slot.captured
    )
  }

  @Test
  fun `fetchComposition() should return composition resource`() = runBlockingTest {
    coEvery { fhirResourceDataSource.loadData(any()) } returns
      Bundle().apply {
        addEntry().resource =
          Composition().apply {
            addSection().apply {
              this.focus =
                Reference().apply {
                  reference = "Binary/123"
                  identifier = Identifier().apply { value = "register-test" }
                }
            }
          }
      }

    val result =
      appSettingViewModel.fetchComposition(
        "Composition?identifier=test-app",
        ApplicationProvider.getApplicationContext()
      )

    coVerify { fhirResourceDataSource.loadData(any()) }

    assertEquals("Binary/123", result!!.sectionFirstRep.focus.reference)
  }

  @Test
  fun testHasDebugSuffix_withSuffix_shouldReturn_true() {
    appSettingViewModel.appId.value = "app/debug"
    Assert.assertTrue(appSettingViewModel.hasDebugSuffix())
  }

  @Test
  fun testHasDebugSuffix_noSuffix_shouldReturn_false() {
    appSettingViewModel.appId.value = "app"
    Assert.assertFalse(appSettingViewModel.hasDebugSuffix())
  }

  @Test
  fun testHasDebugSuffix_emptyAppId_shouldReturn_null() {
    appSettingViewModel.appId.value = ""
    Assert.assertFalse(appSettingViewModel.hasDebugSuffix())
  }
}
