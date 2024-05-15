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

package org.smartregister.fhircore.engine.worker

import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.ParamMap
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.sync.AppTimeStampContext
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import java.util.LinkedList

class CompositionConfigSyncWorkerTest : RobolectricTest() {
  @Test
  fun `should create sync worker with expected properties`() {
    val workerParams = mockk<WorkerParameters>()
    val syncParams = emptyMap<ResourceType, ParamMap>()
    val syncListenerManager = mockk<SyncListenerManager>()
    val fhirEngine = mockk<FhirEngine>()
    val taskExecutor = mockk<TaskExecutor>()
    val timeContext = mockk<AppTimeStampContext>()
    val syncParamSource = mockk<SyncParamSource>()

    val compositionListParamPairsReq = mutableListOf<Pair<ResourceType, Map<String, String>>>()
    compositionListParamPairsReq.add(
      Pair(
        ResourceType.List,
        mapOf(ConfigurationRegistry.ID to "test-resourceId"),
      ),
    )
    compositionListParamPairsReq.add(
      Pair(
        ResourceType.fromCode(ResourceType.List.name),
        mapOf("_count" to "200"),
      ),
    )

    val expectedRequest = mapOf(*compositionListParamPairsReq.toTypedArray())
    val expectedCompositionListRequestQue = LinkedList<(Map<ResourceType, Map<String, String>>)>()
    expectedCompositionListRequestQue.push(expectedRequest)

    every { taskExecutor.serialTaskExecutor } returns mockk()
    every { workerParams.taskExecutor } returns taskExecutor
    every { syncParamSource.compositionConfigRequestQue } returns expectedCompositionListRequestQue
    every { syncListenerManager.loadSyncParams() } returns syncParams

    val testSyncWorker =
      CompositionConfigSyncWorker(mockk(), workerParams, fhirEngine, timeContext, syncParamSource)

    Assert.assertEquals(AcceptLocalConflictResolver, testSyncWorker.getConflictResolver())
    Assert.assertEquals(fhirEngine, testSyncWorker.getFhirEngine())

    runTest {
      Assert.assertEquals(expectedRequest, testSyncWorker.loadCompositionConfigParams())
    }

  }
}
