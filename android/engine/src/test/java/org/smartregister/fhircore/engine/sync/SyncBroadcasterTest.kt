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
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.fhir.OffsetDateTimeTypeAdapter
import com.google.android.fhir.sync.SyncJobStatus
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.AppConfigService
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.trace.FakePerformanceReporter
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
class SyncBroadcasterTest : RobolectricTest() {

  private lateinit var syncBroadcaster: SyncBroadcaster
  private lateinit var workManager: WorkManager
  private lateinit var tokenAuthenticator: TokenAuthenticator
  private val sharedSyncStatus: MutableSharedFlow<SyncJobStatus> = MutableSharedFlow()
  private val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  private lateinit var tracer: PerformanceReporter

  @Before
  fun setUp() {
    val appContext = ApplicationProvider.getApplicationContext<HiltTestApplication>()
    mockkStatic(WorkManager::class)
    workManager = mockk()
    tracer = mockk()
    tokenAuthenticator = mockk()

    every { tokenAuthenticator.sessionActive() } returns true

    val mutableLiveData = MutableLiveData(listOf<WorkInfo>())
    every { WorkManager.getInstance(appContext) } returns workManager
    every { workManager.getWorkInfosForUniqueWorkLiveData(any()) } returns mutableLiveData
    val workRequestSlot = slot<OneTimeWorkRequest>()
    every { workManager.enqueueUniqueWork(any(), any(), capture(workRequestSlot)) } answers
      {
        mockk()
      }
    every { WorkManager.getInstance(any()) } returns workManager

    every {
      sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)
    } returns null

    every { tracer.startTrace(any()) } returns Unit
    every { tracer.putAttribute(any(), any(), any()) } just runs
    every { tracer.incrementMetric(any(), any(), any()) } just runs
    every { tracer.stopTrace(any()) } returns Unit

    syncBroadcaster =
      SyncBroadcaster(
        configurationRegistry = mockk(),
        configService = mockk(),
        fhirEngine = mockk(),
        sharedSyncStatus = sharedSyncStatus,
        dispatcherProvider = CoroutineTestRule().testDispatcherProvider,
        tracer = tracer,
        tokenAuthenticator = tokenAuthenticator,
        sharedPreferencesHelper = sharedPreferencesHelper,
        appContext = mockk(relaxed = true)
      )
  }

  @After
  fun tearDown() {
    // Clean up mocks
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun `runSync should initiate one-time sync when network is available`() = runTest {
    val networkState: (Context) -> Boolean = { true }
    val gson: Gson =
      GsonBuilder()
        .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeTypeAdapter().nullSafe())
        .create()

    val workInfo =
      WorkInfo(
        UUID(0, 0),
        WorkInfo.State.SUCCEEDED,
        Data.EMPTY,
        listOf(),
        workDataOf(
          "StateType" to SyncJobStatus.Started::class.java.name,
          "State" to gson.toJson(SyncJobStatus.Started())
        ),
        0,
        0
      )
    val inProgressInfo =
      WorkInfo(
        UUID(0, 0),
        WorkInfo.State.SUCCEEDED,
        Data.EMPTY,
        listOf(),
        workDataOf(
          "StateType" to SyncJobStatus.Finished::class.java.name,
          "State" to gson.toJson(SyncJobStatus.Finished())
        ),
        0,
        0
      )

    every { workManager.getWorkInfosForUniqueWorkLiveData(any()) } answers
      {
        val uniqueWorkName = firstArg<String>()
        MutableLiveData(
          when {
            uniqueWorkName.startsWith(AppSyncWorker::class.java.name) ->
              listOf(workInfo, inProgressInfo)
            else -> emptyList()
          }
        )
      }

    val collectedSyncStatusList = mutableListOf<SyncJobStatus>()
    val job =
      launch(UnconfinedTestDispatcher(testScheduler)) {
        sharedSyncStatus.toList(collectedSyncStatusList)
      }

    syncBroadcaster.runSync(networkState)

    Assert.assertTrue(collectedSyncStatusList.first() is SyncJobStatus.Started)
    Assert.assertTrue(collectedSyncStatusList.last() is SyncJobStatus.Finished)

    verify { tracer.startTrace(any()) }
    verify { tracer.stopTrace(any()) }

    job.cancel()
  }

  @Test
  fun `runSync should emit failed status when network is unavailable`() = runTest {
    val networkState: (Context) -> Boolean = { false }

    val collectedSyncStatusList = mutableListOf<SyncJobStatus>()
    val job =
      launch(UnconfinedTestDispatcher(testScheduler)) {
        sharedSyncStatus.toList(collectedSyncStatusList)
      }

    syncBroadcaster.runSync(networkState)

    Assert.assertTrue(collectedSyncStatusList.first() is SyncJobStatus.Failed)
    job.cancel()
  }

  @Test
  fun runSyncWhenNetworkStateFalseEmitsSyncFailed() = runTest {
    val configurationRegistry = Faker.buildTestConfigurationRegistry()
    val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
    val configService = AppConfigService(context = context)
    val sharedSyncStatus: MutableSharedFlow<SyncJobStatus> = MutableSharedFlow()
    syncBroadcaster =
      SyncBroadcaster(
        configurationRegistry,
        configService,
        fhirEngine = mockk(),
        sharedSyncStatus,
        dispatcherProvider = CoroutineTestRule().testDispatcherProvider,
        appContext = context,
        tracer = FakePerformanceReporter(),
        tokenAuthenticator = tokenAuthenticator,
        sharedPreferencesHelper = sharedPreferencesHelper
      )
    val collectedSyncStatusList = mutableListOf<SyncJobStatus>()
    val job =
      launch(UnconfinedTestDispatcher(testScheduler)) {
        sharedSyncStatus.toList(collectedSyncStatusList)
      }
    syncBroadcaster.runSync { false }
    val syncStatus = collectedSyncStatusList.first()
    Assert.assertTrue(syncStatus is SyncJobStatus.Failed)
    syncStatus as SyncJobStatus.Failed
    Assert.assertEquals(
      context.getString(R.string.unable_to_sync),
      syncStatus.exceptions.first().exception.message
    )
    job.cancel()
  }

  @Test
  fun shouldEmitSyncFailedWhenNetworkAndTokenSessionIsNotActive() = runTest {
    val configurationRegistry = Faker.buildTestConfigurationRegistry()
    val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
    val configService = AppConfigService(context = context)
    val sharedSyncStatus: MutableSharedFlow<SyncJobStatus> = MutableSharedFlow()
    val tokenAuthenticatorAlt = mockk<TokenAuthenticator>()
    every { tokenAuthenticatorAlt.sessionActive() } returns false
    syncBroadcaster =
      SyncBroadcaster(
        configurationRegistry,
        configService,
        fhirEngine = mockk(),
        sharedSyncStatus,
        dispatcherProvider = CoroutineTestRule().testDispatcherProvider,
        appContext = context,
        tracer = FakePerformanceReporter(),
        tokenAuthenticator = tokenAuthenticatorAlt,
        sharedPreferencesHelper = sharedPreferencesHelper
      )
    val collectedSyncStatusList = mutableListOf<SyncJobStatus>()
    val job =
      launch(UnconfinedTestDispatcher(testScheduler)) {
        sharedSyncStatus.toList(collectedSyncStatusList)
      }
    syncBroadcaster.runSync { true }
    val syncStatus = collectedSyncStatusList.first()
    Assert.assertTrue(syncStatus is SyncJobStatus.Failed)
    syncStatus as SyncJobStatus.Failed
    Assert.assertEquals(
      context.getString(R.string.sync_authentication_error),
      syncStatus.exceptions.first().exception.message
    )
    job.cancel()
  }
}
