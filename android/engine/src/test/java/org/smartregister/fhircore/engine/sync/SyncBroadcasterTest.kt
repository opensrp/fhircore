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

package org.smartregister.fhircore.engine.sync

import android.content.Context
import android.content.SharedPreferences
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.Result
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.SyncJob
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
class SyncBroadcasterTest {

  @MockK private lateinit var fhirResourceService: FhirResourceService
  private lateinit var fhirResourceDataSource: FhirResourceDataSource
  @MockK private lateinit var configService: ConfigService
  @MockK private lateinit var context: Context
  @MockK private lateinit var syncJob: SyncJob
  @MockK private lateinit var fhirEngine: FhirEngine
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
  @MockK private lateinit var sharedPreferences: SharedPreferences
  @MockK private lateinit var configurationRegistry: ConfigurationRegistry
  @MockK private lateinit var gson: Gson
  private val sharedSyncStatus: MutableSharedFlow<State> = MutableSharedFlow()

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private lateinit var syncBroadcaster: SyncBroadcaster

  @Before
  fun setup() {
    MockKAnnotations.init(this)
    fhirResourceDataSource = FhirResourceDataSource(fhirResourceService)
    every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
    sharedPreferencesHelper = SharedPreferencesHelper(context = context, gson = gson)
    syncBroadcaster =
      SyncBroadcaster(
        configurationRegistry,
        sharedPreferencesHelper,
        configService,
        syncJob,
        fhirEngine,
        sharedSyncStatus,
        dispatcherProvider
      )
  }

  @Test
  fun `runSync calls syncJob with correct params`() = runTest {
    every { configService.loadRegistrySyncParams(configurationRegistry, any()) } returns mapOf()
    coEvery { syncJob.run(fhirEngine, any(), any(), any()) } returns Result.Success()

    syncBroadcaster.runSync()
    verify { configService.loadRegistrySyncParams(configurationRegistry, sharedPreferencesHelper) }
    coVerify {
      syncJob.run(
        fhirEngine,
        withArg { Assert.assertTrue(it is ResourceParamsBasedDownloadWorkManager) },
        subscribeTo = sharedSyncStatus,
        resolver = AcceptLocalConflictResolver
      )
    }
  }
}
