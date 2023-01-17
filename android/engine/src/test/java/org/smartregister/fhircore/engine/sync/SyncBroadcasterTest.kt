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

import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.app.AppConfigService
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule

@ExperimentalCoroutinesApi
class SyncBroadcasterTest : RobolectricTest() {

  private lateinit var syncBroadcaster: SyncBroadcaster

  @Test
  fun runSync() = runTest {
    syncBroadcaster = mockk()
    every { syncBroadcaster.runSync() } returns Unit
    syncBroadcaster.runSync()
    verify { syncBroadcaster.runSync() }
  }

  @Test
  fun runSyncWhenNetworkStateFalseEmitsSyncFailed() = runTest {
    val configurationRegistry = Faker.buildTestConfigurationRegistry(mockk())
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
        appContext = context
      )
    val collectedSyncStatusList = mutableListOf<SyncJobStatus>()
    val job =
      launch(UnconfinedTestDispatcher(testScheduler)) {
        sharedSyncStatus.toList(collectedSyncStatusList)
      }
    syncBroadcaster.runSync { false }
    Assert.assertTrue(collectedSyncStatusList.first() is SyncJobStatus.Failed)
    job.cancel()
  }
}
