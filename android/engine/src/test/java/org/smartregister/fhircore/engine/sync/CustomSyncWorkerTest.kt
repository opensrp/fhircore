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

package org.smartregister.fhircore.engine.sync

import android.content.Context
import android.util.Log
import androidx.compose.ui.state.ToggleableState
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import java.util.UUID
import javax.inject.Inject
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.datastore.syncLocationIdsProtoStore
import org.smartregister.fhircore.engine.domain.model.SyncLocationToggleableState
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class CustomSyncWorkerTest : RobolectricTest() {

  @kotlinx.coroutines.ExperimentalCoroutinesApi
  private val resourceService: FhirResourceService = mockk()

  @OptIn(ExperimentalCoroutinesApi::class)
  private var fhirResourceDataSource: FhirResourceDataSource =
    spyk(FhirResourceDataSource(resourceService))

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var customSyncWorker: CustomSyncWorker

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()
    initializeWorkManager()
  }

  @Test
  fun `should create sync worker with expected properties`() {
    sharedPreferencesHelper =
      SharedPreferencesHelper(ApplicationProvider.getApplicationContext(), Gson())
    configurationRegistry = Faker.buildTestConfigurationRegistry(sharedPreferencesHelper)

    customSyncWorker =
      TestListenableWorkerBuilder<CustomSyncWorker>(
          ApplicationProvider.getApplicationContext(),
        )
        .setWorkerFactory(CustomSyncWorkerFactory())
        .build()

    val expected = runBlocking { customSyncWorker.doWork() }

    Assert.assertEquals(ListenableWorker.Result.success(), expected)
  }

  @Test
  fun `should create sync worker with organization`() = runBlocking {
    sharedPreferencesHelper =
      SharedPreferencesHelper(ApplicationProvider.getApplicationContext(), Gson())

    val organizationId1 = "organization-id1"
    val organizationId2 = "organization-id2"
    sharedPreferencesHelper.write(
      ResourceType.Organization.name,
      listOf(organizationId1, organizationId2),
    )
    sharedPreferencesHelper.context.syncLocationIdsProtoStore.updateData {
      listOf(
        SyncLocationToggleableState(UUID.randomUUID().toString(), ToggleableState.On),
      )
    }
    configurationRegistry = Faker.buildTestConfigurationRegistry(sharedPreferencesHelper)

    customSyncWorker =
      TestListenableWorkerBuilder<CustomSyncWorker>(
          ApplicationProvider.getApplicationContext(),
        )
        .setWorkerFactory(CustomSyncWorkerFactory())
        .build()

    val expected = runBlocking { customSyncWorker.doWork() }

    Assert.assertEquals(ListenableWorker.Result.success(), expected)
  }

  private fun writePrefs(key: String, value: String) {
    if (::sharedPreferencesHelper.isInitialized) {
      sharedPreferencesHelper.write(key, value)
    }
  }

  @Test
  fun `should create sync worker with failure results`() {
    configurationRegistry = Faker.buildTestConfigurationRegistry()

    customSyncWorker =
      TestListenableWorkerBuilder<CustomSyncWorker>(
          ApplicationProvider.getApplicationContext(),
        )
        .setWorkerFactory(CustomSyncWorkerFactory())
        .build()

    val expected = runBlocking { customSyncWorker.doWork() }

    Assert.assertEquals(ListenableWorker.Result.failure(), expected)
  }

  private fun initializeWorkManager() {
    val config: Configuration =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    // Initialize WorkManager for instrumentation tests.
    WorkManagerTestInitHelper.initializeTestWorkManager(
      ApplicationProvider.getApplicationContext(),
      config,
    )
  }

  inner class CustomSyncWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters,
    ): ListenableWorker {
      return CustomSyncWorker(
        appContext = appContext,
        workerParams = workerParameters,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = dispatcherProvider,
        fhirResourceDataSource = fhirResourceDataSource,
      )
    }
  }
}
