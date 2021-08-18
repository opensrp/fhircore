package org.smartregister.fhircore.eir

import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow

@Config(shadows = [EirApplicationShadow::class])
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
