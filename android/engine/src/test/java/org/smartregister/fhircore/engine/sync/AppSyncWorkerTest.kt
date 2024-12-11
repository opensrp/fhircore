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

import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.ParamMap
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class AppSyncWorkerTest : RobolectricTest() {
  @Test
  fun `should create sync worker with expected properties`() {
    val workerParams = mockk<WorkerParameters>()
    val syncParams = emptyMap<ResourceType, ParamMap>()
    val syncListenerManager = mockk<SyncListenerManager>()
    val fhirEngine = mockk<FhirEngine>()
    val taskExecutor = mockk<TaskExecutor>()
    val timeContext = mockk<AppTimeStampContext>()
    val configService = mockk<ConfigService>()

    every { taskExecutor.serialTaskExecutor } returns mockk()
    every { workerParams.taskExecutor } returns taskExecutor
    coEvery { syncListenerManager.loadResourceSearchParams() } returns syncParams
    every { syncListenerManager.registerSyncListener(any()) } just runs

    val appSyncWorker =
      AppSyncWorker(
        RuntimeEnvironment.getApplication(),
        workerParams,
        syncListenerManager,
        fhirEngine,
        timeContext,
        configService,
      )

    appSyncWorker.getDownloadWorkManager()
    coVerify { syncListenerManager.loadResourceSearchParams() }

    Assert.assertEquals(AcceptLocalConflictResolver, appSyncWorker.getConflictResolver())
    Assert.assertEquals(fhirEngine, appSyncWorker.getFhirEngine())
  }
}
