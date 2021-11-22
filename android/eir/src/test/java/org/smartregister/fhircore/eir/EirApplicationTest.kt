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

import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJob
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.configuration.app.ConfigService

class EirApplicationTest : RobolectricTest() {

  @Test
  fun testConstructFhirEngineShouldReturnNonNull() {
    Assert.assertNotNull(EirApplication.getContext().fhirEngine)
  }
  @Test
  fun testThatApplicationIsInstanceOfConfigurableApplication() {
    Assert.assertTrue(
      ApplicationProvider.getApplicationContext<EirApplication>() is ConfigService
    )
  }

  @Test
  fun testSyncJobShouldReturnNonNull() {
    Assert.assertNotNull(EirApplication.getContext().syncJob)
  }

  @Test
  @Ignore("Fix null pointer exception and test verification")
  fun testRunSyncShouldCallSyncJobRun() = runBlockingTest {
    mockkObject(Sync)

    val application = spyk<EirApplication>(ApplicationProvider.getApplicationContext())

    val syncJob: SyncJob = spyk()
    every { Sync.basicSyncJob(any()) } returns syncJob

    val sharedSyncStatus: MutableSharedFlow<State> = spyk()
    application.runOneTimeSync(sharedSyncStatus = sharedSyncStatus)

    coVerify {
      syncJob.run(application.fhirEngine, any(), application.resourceSyncParams, sharedSyncStatus)
    }

    unmockkObject(Sync)
  }
}
