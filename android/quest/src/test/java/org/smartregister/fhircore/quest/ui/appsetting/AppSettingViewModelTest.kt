/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
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
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
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
import org.junit.Before
import org.junit.Rule
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
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.getPayload
import org.smartregister.fhircore.engine.util.extension.second
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import retrofit2.HttpException
import retrofit2.Response

@HiltAndroidTest
@kotlinx.coroutines.ExperimentalCoroutinesApi
class AppSettingViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var dispatcherProvider: DispatcherProvider
  private val defaultRepository = mockk<DefaultRepository>()
  private val fhirResourceDataSource = mockk<FhirResourceDataSource>()
  private val configService = mockk<ConfigService>()
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
  private lateinit var appSettingViewModel: AppSettingViewModel

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    every { defaultRepository.fhirEngine } returns fhirEngine

    appSettingViewModel =
      spyk(
        AppSettingViewModel(
          fhirResourceDataSource = fhirResourceDataSource,
          defaultRepository = defaultRepository,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configService = configService,
          configurationRegistry = Faker.buildTestConfigurationRegistry(),
          dispatcherProvider = dispatcherProvider,
        ),
      )
  }

  @Test
  fun testOnApplicationIdChanged() {
    appSettingViewModel.onApplicationIdChanged("appId")
    Assert.assertNotNull(appSettingViewModel.appId.value)
    Assert.assertEquals("appId", appSettingViewModel.appId.value)
  }

  @Test
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
  fun testFetchConfigurations() =
    runTest(timeout = 90.seconds) {
      fhirEngine.create(Composition().apply { id = "sampleComposition" })
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
  fun `fetchConfigurations() should call configurationRegistry#processResultBundleBinaries with correct values`() =
    runTest {
      coEvery { appSettingViewModel.configurationRegistry.fetchRemoteComposition(any()) } returns
        Composition().apply {
          addSection().apply {
            this.focus =
              Reference().apply {
                reference = "Binary/123"
                identifier = Identifier().apply { value = "register-test" }
              }
          }
        }

      val expectedBundle =
        Bundle().apply {
          addEntry().resource =
            Binary().apply {
              id = "binary-id-1"
              data =
                Base64.getEncoder()
                  .encode(
                    JsonUtil.serialize(
                        RegisterConfiguration(
                          id = "1",
                          appId = "a",
                          fhirResource =
                            FhirResourceConfig(
                              baseResource =
                                ResourceConfig(
                                  resource = ResourceType.Patient,
                                ),
                              relatedResources =
                                listOf(
                                  ResourceConfig(
                                    resource = ResourceType.Encounter,
                                  ),
                                  ResourceConfig(
                                    resource = ResourceType.Task,
                                  ),
                                ),
                            ),
                        ),
                      )
                      .encodeToByteArray(),
                  )
            }
        }
      coEvery { fhirResourceDataSource.post(any(), any()) } returns expectedBundle

      coEvery { defaultRepository.createRemote(any(), any()) } just runs
      coEvery { appSettingViewModel.configurationRegistry.saveSyncSharedPreferences(any()) } just
        runs
      coEvery { appSettingViewModel.loadConfigurations(any()) } just runs
      coEvery { appSettingViewModel.isNonProxy() } returns false
      coEvery {
        appSettingViewModel.configurationRegistry.processResultBundleBinaries(any(), any())
      } just runs

      appSettingViewModel.run {
        onApplicationIdChanged("app")
        fetchConfigurations(context)
      }

      val binarySlot = slot<Binary>()

      coVerify { appSettingViewModel.configurationRegistry.fetchRemoteComposition(any()) }
      coVerify { fhirResourceDataSource.post(any(), any()) }
      coVerify { defaultRepository.createRemote(any(), any()) }
      coVerify {
        appSettingViewModel.configurationRegistry.processResultBundleBinaries(
          capture(binarySlot),
          any(),
        )
      }

      assertEquals(expectedBundle.entry[0].resource.id, binarySlot.captured.id)
      assertEquals((expectedBundle.entry[0].resource as Binary).data, binarySlot.captured.data)
    }

  @Test
  fun `fetchConfigurations() should decode profile configuration`() =
    runTest(timeout = 90.seconds) {
      fhirEngine.create(
        Composition().apply { id = "sampleComposition" },
      ) // Create sample Composition

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
      coEvery { fhirResourceDataSource.post(any(), any()) } returns
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
                                  ResourceConfig(
                                    resource = ResourceType.Encounter,
                                  ),
                                  ResourceConfig(
                                    resource = ResourceType.Task,
                                  ),
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
      coEvery { appSettingViewModel.configurationRegistry.saveSyncSharedPreferences(any()) } just
        runs
      coEvery { appSettingViewModel.isNonProxy() } returns false

      appSettingViewModel.run {
        onApplicationIdChanged("app")
        fetchConfigurations(context)
      }

      val slot = slot<List<ResourceType>>()

      coVerify { appSettingViewModel.fetchComposition(any(), any()) }
      coVerify { fhirResourceDataSource.post(any(), any()) }
      coVerify { defaultRepository.createRemote(any(), any()) }
      coVerify {
        appSettingViewModel.configurationRegistry.saveSyncSharedPreferences(capture(slot))
      }

      Assert.assertEquals(
        listOf(ResourceType.Patient, ResourceType.Encounter, ResourceType.Task),
        slot.captured,
      )
    }

  @Test(expected = HttpException::class)
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
  fun testFetchConfigurationsThrowsHttpExceptionWithStatusCodeOutside400And503() {
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

    runBlocking { fhirResourceDataSource.getResource(anyString()) }
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
  fun testHasDebugSuffix_withSuffix_shouldReturn_true() {
    coEvery { appSettingViewModel.isDebugVariant() } returns true
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
    appSettingViewModel.appId.value = null
    Assert.assertFalse(appSettingViewModel.hasDebugSuffix())
  }

  @Test
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

    Assert.assertEquals(21, compositionSections.size)

    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section = compositionSections
      }

    coEvery { appSettingViewModel.loadConfigurations(any()) } just runs
    coEvery { appSettingViewModel.isNonProxy() } returns false
    coEvery { appSettingViewModel.appId } returns MutableLiveData(appId)
    coEvery { appSettingViewModel.configurationRegistry.fetchRemoteComposition(appId) } returns
      composition
    coEvery { appSettingViewModel.defaultRepository.createRemote(any(), any()) } just runs
    coEvery { appSettingViewModel.configurationRegistry.saveSyncSharedPreferences(any()) } just runs
    coEvery {
      appSettingViewModel.configurationRegistry.processResultBundleBinaries(any(), any())
    } just runs
    coEvery { fhirResourceDataSource.post(any(), any()) } returns
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

    val urlArgumentSlot = mutableListOf<String>()
    val requestPathPostArgumentSlot = mutableListOf<RequestBody>()

    coVerify(exactly = 2) {
      fhirResourceDataSource.post(capture(urlArgumentSlot), capture(requestPathPostArgumentSlot))
    }

    Assert.assertEquals(2, requestPathPostArgumentSlot.size)
    Assert.assertEquals(
      "{\"resourceType\":\"Bundle\",\"type\":\"batch\",\"entry\":[{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-1\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-2\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-3\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-4\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-5\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-6\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-7\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-8\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-9\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-10\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-11\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-12\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-13\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-14\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-15\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-16\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-17\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-18\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-19\"}},{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-20\"}}]}",
      requestPathPostArgumentSlot.first().getPayload(),
    )
    Assert.assertEquals(
      "{\"resourceType\":\"Bundle\",\"type\":\"batch\",\"entry\":[{\"request\":{\"method\":\"GET\",\"url\":\"Binary/id-21\"}}]}",
      requestPathPostArgumentSlot.last().getPayload(),
    )
  }

  @Test
  fun `fetchConfigurations() should decode profile configuration Non Proxy`() =
    runTest(timeout = 90.seconds) {
      fhirEngine.create(Composition().apply { id = "sampleComposition" })

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
                                  ResourceConfig(
                                    resource = ResourceType.Encounter,
                                  ),
                                  ResourceConfig(
                                    resource = ResourceType.Task,
                                  ),
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
      coEvery { appSettingViewModel.configurationRegistry.saveSyncSharedPreferences(any()) } just
        runs
      coEvery { appSettingViewModel.isNonProxy() } returns true

      appSettingViewModel.run {
        onApplicationIdChanged("app")
        fetchConfigurations(context)
      }

      val slot = slot<List<ResourceType>>()

      coVerify { appSettingViewModel.fetchComposition(any(), any()) }
      coVerify { fhirResourceDataSource.getResource(any()) }
      coVerify { defaultRepository.createRemote(any(), any()) }
      coVerify {
        appSettingViewModel.configurationRegistry.saveSyncSharedPreferences(capture(slot))
      }

      Assert.assertEquals(
        listOf(ResourceType.Patient, ResourceType.Encounter, ResourceType.Task),
        slot.captured,
      )
    }
}
