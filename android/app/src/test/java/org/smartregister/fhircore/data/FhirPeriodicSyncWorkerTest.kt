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

package org.smartregister.fhircore.data

import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class FhirPeriodicSyncWorkerTest : RobolectricTest() {

  private lateinit var fhirPeriodicSyncWorker: FhirPeriodicSyncWorker

  @Before
  fun setUp() {

    val workerParam = mockk<WorkerParameters>()
    every { workerParam.taskExecutor } returns WorkManagerTaskExecutor(mockk())

    fhirPeriodicSyncWorker = FhirPeriodicSyncWorker(FhirApplication.getContext(), workerParam)
  }

  @Test
  fun testGetFhirEngineShouldReturnNonNullFhirEngine() {
    Assert.assertNotNull(fhirPeriodicSyncWorker.getFhirEngine())
  }
}
