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
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.AppDataStore

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AppSyncWorkerTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var fhirEngine: FhirEngine
  @BindValue val dataStore: AppDataStore = mockk()
  @BindValue val syncParamsManager: SyncParametersManager = mockk()
  private lateinit var appSyncWorker: AppSyncWorker

  @Before
  fun setUp() {
    hiltRule.inject()

    val appContext: Context = ApplicationProvider.getApplicationContext()
    appSyncWorker =
      TestListenableWorkerBuilder<AppSyncWorker>(appContext)
        .setWorkerFactory(
          object : WorkerFactory() {
            override fun createWorker(
              appContext: Context,
              workerClassName: String,
              workerParameters: WorkerParameters
            ): ListenableWorker =
              AppSyncWorker(appContext, workerParameters, syncParamsManager, fhirEngine, dataStore)
          }
        )
        .build()
  }

  @Test
  fun getConflictResolverReturnsAcceptLocalConflictResolver() {
    Assert.assertEquals(AcceptLocalConflictResolver, appSyncWorker.getConflictResolver())
  }

  @Test
  fun getDownloadWorkManagerCallsSyncParameterManagerParams() {
    every { syncParamsManager.getSyncParams() } returns emptyMap()
    val downloadManager = appSyncWorker.getDownloadWorkManager()
    Assert.assertNotNull(downloadManager)
    Assert.assertTrue(downloadManager is ResourceParamsBasedDownloadWorkManager)
    verify(exactly = 1) { syncParamsManager.getSyncParams() }
  }

  @Test
  fun getDownloadWorkManagerContextGetsAndSavesTimestampToDataStore() = runTest {
    every { syncParamsManager.getSyncParams() } returns emptyMap()

    val oldTimestamp = "2023-04-20T07:24:47.111Z"
    val newTimestamp = "2023-04-20T10:17:18.111Z"
    var lastUpdatedTimestamp = oldTimestamp

    coEvery { dataStore.getLastUpdateTimestamp(ResourceType.Patient) } answers
      {
        lastUpdatedTimestamp
      }
    coEvery { dataStore.saveLastUpdatedTimestamp(ResourceType.Patient, newTimestamp) } answers
      {
        lastUpdatedTimestamp = this.secondArg() as String
      }
    val downloadManager =
      appSyncWorker.getDownloadWorkManager() as ResourceParamsBasedDownloadWorkManager
    val timestampContext = downloadManager.context
    Assert.assertEquals(oldTimestamp, timestampContext.getLasUpdateTimestamp(ResourceType.Patient))
    timestampContext.saveLastUpdatedTimestamp(ResourceType.Patient, newTimestamp)
    Assert.assertEquals(newTimestamp, timestampContext.getLasUpdateTimestamp(ResourceType.Patient))
  }

  @Test
  fun getFhirEngine() {
    Assert.assertEquals(fhirEngine, appSyncWorker.getFhirEngine())
  }
}
