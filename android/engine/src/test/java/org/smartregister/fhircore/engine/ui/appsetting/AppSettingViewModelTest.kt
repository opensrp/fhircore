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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.GsonBuilder
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import java.net.UnknownHostException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.showToast
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingViewModelTest : RobolectricTest() {
  @ExperimentalCoroutinesApi @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()
  private val defaultRepository = mockk<DefaultRepository>()
  private val fhirResourceDataSource = mockk<FhirResourceDataSource>()
  private val sharedPreferencesHelper =
    SharedPreferencesHelper(
      ApplicationProvider.getApplicationContext(),
      GsonBuilder().setLenient().create()
    )

  private val configService = mockk<ConfigService>()
  @ExperimentalCoroutinesApi
  private val appSettingViewModel =
    spyk(
      AppSettingViewModel(
        fhirResourceDataSource = fhirResourceDataSource,
        defaultRepository = defaultRepository,
        sharedPreferencesHelper = sharedPreferencesHelper,
        configurationRegistry = Faker.buildTestConfigurationRegistry(),
        dispatcherProvider = this.coroutineTestRule.testDispatcherProvider
      )
    )
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  @Test
  fun testOnApplicationIdChanged() {
    appSettingViewModel.onApplicationIdChanged("appId")
    Assert.assertNotNull(appSettingViewModel.appId.value)
    Assert.assertEquals("appId", appSettingViewModel.appId.value)
  }

  @Test
  @ExperimentalCoroutinesApi
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
  @Ignore("Fix failing test")
  @ExperimentalCoroutinesApi
  fun testFetchConfigurations() = runTest {
    coEvery { appSettingViewModel.fhirResourceDataSource.getResource(any()) } returns
      Bundle().apply {
        addEntry().resource =
          Composition().apply {
            addSection().apply { this.focus = Reference().apply { reference = "Binary/123" } }
          }
      }
    coEvery { appSettingViewModel.defaultRepository.create(any()) } returns emptyList()

    appSettingViewModel.fetchConfigurations(context)

    coVerify { appSettingViewModel.fhirResourceDataSource.getResource(any()) }
    coVerify { appSettingViewModel.defaultRepository.create(any()) }
  }

  @Test(expected = HttpException::class)
  @ExperimentalCoroutinesApi
  fun testFetchConfigurationsThrowsHttpExceptionWithStatusCodeBetween400And503() = runTest {
    val appId = "app_id"
    appSettingViewModel.onApplicationIdChanged(appId)
    val context = mockk<Context>(relaxed = true)
    val fhirResourceDataSource = FhirResourceDataSource(mockk())
    coEvery { fhirResourceDataSource.getResource(ArgumentMatchers.anyString()) } throws
      HttpException(
        Response.error<ResponseBody>(
          500,
          "Internal Server Error".toResponseBody("application/json".toMediaTypeOrNull())
        )
      )
    fhirResourceDataSource.getResource(ArgumentMatchers.anyString())
    verify { context.showToast(context.getString(R.string.error_loading_config_http_error)) }
    coVerify { fhirResourceDataSource.getResource(ArgumentMatchers.anyString()) }
    coVerify { appSettingViewModel.fetchConfigurations(context) }
    verify { context.showToast(context.getString(R.string.error_loading_config_http_error)) }
    Assert.assertEquals(
      context.getString(R.string.error_loading_config_http_error),
      appSettingViewModel.error.value
    )
    Assert.assertEquals(false, appSettingViewModel.showProgressBar.value)
  }

  @Test(expected = UnknownHostException::class)
  @ExperimentalCoroutinesApi
  fun testFetchConfigurationsThrowsUnknownHostException() = runTest {
    val appId = "app_id"
    appSettingViewModel.onApplicationIdChanged(appId)
    val fhirResourceDataSource = FhirResourceDataSource(mockk())
    coEvery { fhirResourceDataSource.getResource(ArgumentMatchers.anyString()) } throws
      UnknownHostException(context.getString(R.string.error_loading_config_no_internet))
    fhirResourceDataSource.getResource(ArgumentMatchers.anyString())
    coVerify { appSettingViewModel.fetchConfigurations(context) }
    verify { context.showToast(context.getString(R.string.error_loading_config_no_internet)) }
    Assert.assertEquals(
      context.getString(R.string.error_loading_config_no_internet),
      appSettingViewModel.error.value
    )
    Assert.assertEquals(false, appSettingViewModel.showProgressBar.value)
  }

  @Test(expected = Exception::class)
  @ExperimentalCoroutinesApi
  fun testFetchConfigurationsThrowsException() = runTest {
    val context = mockk<Context>(relaxed = true)
    val appId = "app_id"
    appSettingViewModel.onApplicationIdChanged(appId)
    val fhirResourceDataSource = FhirResourceDataSource(mockk())
    coEvery { fhirResourceDataSource.getResource(ArgumentMatchers.anyString()) } throws
      Exception(context.getString(R.string.error_loading_config_general))
    coEvery { appSettingViewModel.fetchConfigurations(context) } just runs
    appSettingViewModel.fetchConfigurations(ArgumentMatchers.any(Context::class.java))
    every { context.getString(R.string.error_loading_config_general) }
    Assert.assertEquals(
      context.getString(R.string.error_loading_config_no_internet),
      appSettingViewModel.error.value
    )
    Assert.assertEquals(false, appSettingViewModel.showProgressBar.value)
  }

  @Test
  fun testHasDebugSuffix_withSuffix_shouldReturn_true() {
    appSettingViewModel.appId.value = "default/debug"
    Assert.assertTrue(appSettingViewModel.hasDebugSuffix()!!)
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testHasDebugSuffix_noSuffix_shouldReturn_false() {
    appSettingViewModel.appId.value = "app"
    Assert.assertFalse(appSettingViewModel.hasDebugSuffix())
  }

  @Test
  @ExperimentalCoroutinesApi
  fun testHasDebugSuffix_emptyAppId_shouldReturn_null() {
    appSettingViewModel.appId.value = null
    Assert.assertFalse(appSettingViewModel.hasDebugSuffix())
  }
}
