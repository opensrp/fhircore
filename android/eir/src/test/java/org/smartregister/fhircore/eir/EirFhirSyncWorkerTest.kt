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

package org.smartregister.fhircore.eir

import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow

@Config(shadows = [EirApplicationShadow::class])
@Ignore("Fails automated execution but works locally") // TODO fix
class EirFhirSyncWorkerTest : RobolectricTest() {

  private lateinit var eirFhirSyncWorker: EirFhirSyncWorker

  @Before
  fun setUp() {

    val workerParam = mockk<WorkerParameters>()
    every { workerParam.taskExecutor } returns WorkManagerTaskExecutor(mockk())

    eirFhirSyncWorker = EirFhirSyncWorker(EirApplication.getContext(), workerParam)
  }

  @Test
  fun testGetFhirEngineShouldReturnNonNullFhirEngine() {
    Assert.assertNotNull(eirFhirSyncWorker.getFhirEngine())
  }

  @Test
  fun testGetSyncDataReturnMapOfConfiguredSyncItems() {
    val data = eirFhirSyncWorker.getSyncData()
    Assert.assertEquals(data.size, 5)
    Assert.assertTrue(data.containsKey(ResourceType.Patient))
    Assert.assertTrue(data.containsKey(ResourceType.Immunization))
    Assert.assertTrue(data.containsKey(ResourceType.StructureMap))
    Assert.assertTrue(data.containsKey(ResourceType.RelatedPerson))
  }

  @Test
  fun testGetDataSourceReturnsDataSource() {
    Assert.assertNotNull(eirFhirSyncWorker.getDataSource())
  }
}
