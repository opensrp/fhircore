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

package org.smartregister.fhircore.quest.ui.appsetting

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.util.JsonUtil
import com.google.gson.GsonBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.second
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import retrofit2.HttpException
import retrofit2.Response

@HiltAndroidTest
class AppSettingViewModelTest : RobolectricTest() {

  private val defaultRepository = mockk<DefaultRepository>()
  private val fhirResourceDataSource = mockk<FhirResourceDataSource>()
  private val sharedPreferencesHelper =
    SharedPreferencesHelper(
      ApplicationProvider.getApplicationContext(),
      GsonBuilder().setLenient().create(),
    )

  private val configService = mockk<ConfigService>()

  @kotlinx.coroutines.ExperimentalCoroutinesApi
  private val appSettingViewModel =
    spyk(
      AppSettingViewModel(
        fhirResourceDataSource = fhirResourceDataSource,
        defaultRepository = defaultRepository,
        sharedPreferencesHelper = sharedPreferencesHelper,
        configService = configService,
        configurationRegistry = Faker.buildTestConfigurationRegistry(),
        dispatcherProvider = this.coroutineTestRule.testDispatcherProvider,
      ),
    )
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testOnApplicationIdChanged() {
    appSettingViewModel.onApplicationIdChanged("appId")
    Assert.assertNotNull(appSettingViewModel.appId.value)
    Assert.assertEquals("appId", appSettingViewModel.appId.value)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurations() = runTest {
    coEvery { appSettingViewModel.fhirResourceDataSource.getResource(any()) } returns
      Bundle().apply { addEntry().resource = Composition() }
    coEvery { appSettingViewModel.defaultRepository.create(any()) } returns emptyList()

    val appId = "app/debug"
    appSettingViewModel.appId.value = appId
    appSettingViewModel.loadConfigurations(context)
    Assert.assertNotNull(appSettingViewModel.showProgressBar.value)
    Assert.assertFalse(appSettingViewModel.showProgressBar.value!!)
    Assert.assertEquals(appId, sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null))
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchConfigurations() = runTest {
    val appId = "test_app_id"
    appSettingViewModel.onApplicationIdChanged(appId)

    coEvery { fhirResourceDataSource.getResource(any()) } returns
      Bundle().apply {
        addEntry().resource =
          Composition().apply {
            addSection().apply { this.focus = Reference().apply { reference = "Binary/123" } }
          }
      }
    coEvery { appSettingViewModel.defaultRepository.createRemote(any(), any()) } just runs

    appSettingViewModel.fetchConfigurations(context)

    coVerify { fhirResourceDataSource.getResource(any()) }
    coVerify { appSettingViewModel.defaultRepository.createRemote(any(), any()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun `fetchConfigurations() should save shared preferences for patient related resource types`() =
    runTest {
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
      coEvery { fhirResourceDataSource.getResource(any()) } returns
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
                          fhirResource =
                            FhirResourceConfig(
                              baseResource = ResourceConfig(resource = ResourceType.Patient),
                              relatedResources =
                                listOf(
                                  ResourceConfig(resource = ResourceType.Encounter),
                                  ResourceConfig(resource = ResourceType.Task),
                                ),
                            ),
                        ),
                      )
                      .encodeToByteArray(),
                  )
            }
        }
      coEvery { defaultRepository.createRemote(any(), any()) } just runs
      coEvery { appSettingViewModel.saveSyncSharedPreferences(any()) } just runs

      appSettingViewModel.run {
        onApplicationIdChanged("app")
        fetchConfigurations(context)
      }

      val slot = slot<List<ResourceType>>()

      coVerify { appSettingViewModel.fetchComposition(any(), any()) }
      coVerify { fhirResourceDataSource.getResource(any()) }
      coVerify { defaultRepository.createRemote(any(), any()) }
      coVerify { appSettingViewModel.saveSyncSharedPreferences(capture(slot)) }

      Assert.assertEquals(
        listOf(ResourceType.Patient, ResourceType.Encounter, ResourceType.Task),
        slot.captured,
      )
    }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun `fetchConfigurations() should decode profile configuration`() = runTest {
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
    coEvery { fhirResourceDataSource.getResource(any()) } returns
      Bundle().apply {
        addEntry().resource =
          Binary().apply {
            data =
              Base64.getEncoder()
                .encode(
                  JsonUtil.serialize(
                      ProfileConfiguration(
                        id = "1",
                        appId = "a",
                        fhirResource =
                          FhirResourceConfig(
                            baseResource = ResourceConfig(resource = ResourceType.Patient),
                            relatedResources =
                              listOf(
                                ResourceConfig(resource = ResourceType.Encounter),
                                ResourceConfig(resource = ResourceType.Task),
                              ),
                          ),
                        profileParams = listOf("1"),
                      ),
                    )
                    .encodeToByteArray(),
                )
          }
      }
    coEvery { defaultRepository.createRemote(any(), any()) } just runs
    coEvery { appSettingViewModel.saveSyncSharedPreferences(any()) } just runs

    appSettingViewModel.run {
      onApplicationIdChanged("app")
      fetchConfigurations(context)
    }

    val slot = slot<List<ResourceType>>()

    coVerify { appSettingViewModel.fetchComposition(any(), any()) }
    coVerify { fhirResourceDataSource.getResource(any()) }
    coVerify { defaultRepository.createRemote(any(), any()) }
    coVerify { appSettingViewModel.saveSyncSharedPreferences(capture(slot)) }

    Assert.assertEquals(
      listOf(ResourceType.Patient, ResourceType.Encounter, ResourceType.Task),
      slot.captured,
    )
  }

  @Test(expected = HttpException::class)
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchConfigurationsThrowsHttpExceptionWithStatusCodeBetween400And503() = runTest {
    val appId = "app_id"
    appSettingViewModel.onApplicationIdChanged(appId)
    val context = mockk<Context>(relaxed = true)
    val fhirResourceDataSource = FhirResourceDataSource(mockk())
    coEvery { fhirResourceDataSource.getResource(anyString()) } throws
      HttpException(
        Response.error<ResponseBody>(
          500,
          "Internal Server Error".toResponseBody("application/json".toMediaTypeOrNull()),
        ),
      )
    fhirResourceDataSource.getResource(anyString())
    verify { context.showToast(context.getString(R.string.error_loading_config_http_error)) }
    coVerify { fhirResourceDataSource.getResource(anyString()) }
    coVerify { appSettingViewModel.fetchConfigurations(context) }
    verify { context.showToast(context.getString(R.string.error_loading_config_http_error)) }
    coVerify { (appSettingViewModel.fetchComposition(appId, any())) }
    Assert.assertEquals(
      context.getString(R.string.error_loading_config_http_error),
      appSettingViewModel.error.value,
    )
    Assert.assertEquals(false, appSettingViewModel.showProgressBar.value)
  }

  @Test(expected = HttpException::class)
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchConfigurationsThrowsHttpExceptionWithStatusCodeOutside400And503() = runTest {
    val appId = "app_id"
    appSettingViewModel.onApplicationIdChanged(appId)
    val context = mockk<Context>(relaxed = true)
    val fhirResourceDataSource = FhirResourceDataSource(mockk())
    coEvery { fhirResourceDataSource.getResource(anyString()) } throws
      HttpException(
        Response.error<ResponseBody>(
          504,
          "Internal Server Error".toResponseBody("application/json".toMediaTypeOrNull()),
        ),
      )
    fhirResourceDataSource.getResource(anyString())
    verify { context.showToast(context.getString(R.string.error_loading_config_http_error)) }
    coVerify { fhirResourceDataSource.getResource(anyString()) }
    coVerify { appSettingViewModel.fetchConfigurations(context) }
    verify { context.showToast(context.getString(R.string.error_loading_config_http_error)) }
    coVerify { (appSettingViewModel.fetchComposition(appId, any())) }
    Assert.assertEquals(
      context.getString(R.string.error_loading_config_http_error),
      appSettingViewModel.error.value,
    )
    Assert.assertEquals(false, appSettingViewModel.showProgressBar.value)
  }

  @Test(expected = UnknownHostException::class)
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchConfigurationsThrowsUnknownHostException() = runTest {
    val appId = "app_id"
    appSettingViewModel.onApplicationIdChanged(appId)
    val fhirResourceDataSource = FhirResourceDataSource(mockk())
    coEvery { fhirResourceDataSource.getResource(anyString()) } throws
      UnknownHostException(context.getString(R.string.error_loading_config_no_internet))
    fhirResourceDataSource.getResource(anyString())
    coVerify { appSettingViewModel.fetchConfigurations(context) }
    verify { context.showToast(context.getString(R.string.error_loading_config_no_internet)) }
    coVerify { (appSettingViewModel.fetchComposition(appId, any())) }
    Assert.assertEquals(
      context.getString(R.string.error_loading_config_no_internet),
      appSettingViewModel.error.value,
    )
    Assert.assertEquals(false, appSettingViewModel.showProgressBar.value)
  }

  @Test(expected = Exception::class)
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchConfigurationsThrowsException() = runTest {
    val context = mockk<Context>(relaxed = true)
    val appId = "app_id"
    appSettingViewModel.onApplicationIdChanged(appId)
    val fhirResourceDataSource = FhirResourceDataSource(mockk())
    coEvery { fhirResourceDataSource.getResource(anyString()) } throws
      Exception(context.getString(R.string.error_loading_config_general))
    coEvery { appSettingViewModel.fetchConfigurations(context) } just runs
    appSettingViewModel.fetchConfigurations(any(Context::class.java))
    every { context.getString(R.string.error_loading_config_general) }
    coVerify { (appSettingViewModel.fetchComposition(appId, any())) }
    Assert.assertEquals(
      context.getString(R.string.error_loading_config_no_internet),
      appSettingViewModel.error.value,
    )
    Assert.assertEquals(false, appSettingViewModel.showProgressBar.value)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun `fetchComposition() should return composition resource`() = runTest {
    coEvery { fhirResourceDataSource.getResource(any()) } returns
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
        ApplicationProvider.getApplicationContext(),
      )

    coVerify { fhirResourceDataSource.getResource(any()) }

    Assert.assertEquals("Binary/123", result!!.sectionFirstRep.focus.reference)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testHasDebugSuffix_withSuffix_shouldReturn_true() {
    appSettingViewModel.appId.value = "app/debug"
    Assert.assertTrue(appSettingViewModel.hasDebugSuffix())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testHasDebugSuffix_noSuffix_shouldReturn_false() {
    appSettingViewModel.appId.value = "app"
    Assert.assertFalse(appSettingViewModel.hasDebugSuffix())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testHasDebugSuffix_emptyAppId_shouldReturn_null() {
    appSettingViewModel.appId.value = null
    Assert.assertFalse(appSettingViewModel.hasDebugSuffix())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testSaveSyncSharedPreferencesShouldVerifyDataSave() {
    val resourceType =
      listOf(ResourceType.Task, ResourceType.Patient, ResourceType.Task, ResourceType.Patient)

    appSettingViewModel.saveSyncSharedPreferences(resourceType)

    val result =
      sharedPreferencesHelper.read<List<ResourceType>>(
        SharedPreferenceKey.REMOTE_SYNC_RESOURCES.name,
      )!!

    Assert.assertEquals(2, result.size)
    Assert.assertEquals(ResourceType.Task.name, result.first())
    Assert.assertEquals(ResourceType.Patient.name, result.last())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchConfigurationsChunking() = runTest {
    val appId = "test_app_id"
    val compositionSections = mutableListOf<Composition.SectionComponent>()

    for (i in
      1..ConfigurationRegistry.MANIFEST_PROCESSOR_BATCH_SIZE +
          1) { // We need more than the MAX batch size
      compositionSections.add(
        Composition.SectionComponent().apply {
          focus.reference = "${ResourceType.Binary.name}/id-$i"
          focus.identifier = Identifier().apply { id = "${ResourceType.Binary.name}/id-$i" }
        },
      )
    }

    Assert.assertEquals(31, compositionSections.size)

    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section = compositionSections
      }

    coEvery { appSettingViewModel.loadConfigurations(any()) } just runs
    coEvery { appSettingViewModel.appId } returns MutableLiveData(appId)
    coEvery {
      fhirResourceDataSource.getResource("Composition?identifier=test_app_id&_count=200")
    } returns Bundle().apply { addEntry().resource = composition }
    coEvery { appSettingViewModel.defaultRepository.createRemote(any(), any()) } just runs
    coEvery {
      fhirResourceDataSource.getResource(
        "Binary?_id=id-1,id-2,id-3,id-4,id-5,id-6,id-7,id-8,id-9,id-10,id-11,id-12,id-13,id-14,id-15,id-16,id-17,id-18,id-19,id-20,id-21,id-22,id-23,id-24,id-25,id-26,id-27,id-28,id-29,id-30&_count=200",
      )
    } returns
      Bundle().apply {
        entry =
          listOf(
            Bundle.BundleEntryComponent().apply {
              resource =
                Binary().apply {
                  data =
                    RegisterConfiguration(id = "1", appId = appId, fhirResource = mockk())
                      .apply {}
                      .toString()
                      .toByteArray(StandardCharsets.UTF_8)
                }
            },
          )
      }
    coEvery { fhirResourceDataSource.getResource("Binary?_id=id-31&_count=200") } returns
      Bundle().apply {
        entry =
          listOf(
            Bundle.BundleEntryComponent().apply {
              resource =
                Binary().apply {
                  data =
                    ProfileConfiguration(id = "2", appId = appId, fhirResource = mockk())
                      .apply {}
                      .toString()
                      .toByteArray(StandardCharsets.UTF_8)
                }
            },
          )
      }

    appSettingViewModel.fetchConfigurations(context)

    val resourceArgumentSlot = mutableListOf<Resource>()

    coVerify(exactly = 3) {
      appSettingViewModel.defaultRepository.createRemote(any(), capture(resourceArgumentSlot))
    }

    val capturedResources: List<Resource> = resourceArgumentSlot
    Assert.assertEquals(3, capturedResources.size)

    Assert.assertTrue(capturedResources.first() is Binary)
    Assert.assertTrue(capturedResources.second() is Binary)
    Assert.assertTrue(capturedResources.last() is Composition)

    val requestPathArgumentSlot = mutableListOf<String>()

    coVerify { fhirResourceDataSource.getResource(capture(requestPathArgumentSlot)) }

    Assert.assertEquals(3, requestPathArgumentSlot.size)

    Assert.assertEquals(
      "Composition?identifier=test_app_id&_count=200",
      requestPathArgumentSlot.first(),
    )
    Assert.assertEquals(
      "Binary?_id=id-1,id-2,id-3,id-4,id-5,id-6,id-7,id-8,id-9,id-10,id-11,id-12,id-13,id-14,id-15,id-16,id-17,id-18,id-19,id-20,id-21,id-22,id-23,id-24,id-25,id-26,id-27,id-28,id-29,id-30&_count=200",
      requestPathArgumentSlot.second(),
    )
    Assert.assertEquals("Binary?_id=id-31&_count=200", requestPathArgumentSlot.last())
  }
}
