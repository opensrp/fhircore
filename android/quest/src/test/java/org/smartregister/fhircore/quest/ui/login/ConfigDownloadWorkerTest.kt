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

package org.smartregister.fhircore.quest.ui.login

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.data.DataMigration
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class ConfigDownloadWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var defaultRepository: DefaultRepository
  private val configurationRegistry = mockk<ConfigurationRegistry>()
  private lateinit var configDownloadWorker: ConfigDownloadWorker
  private lateinit var dataMigration: DataMigration

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    dataMigration = mockk()
    configDownloadWorker =
      TestListenableWorkerBuilder<ConfigDownloadWorker>(ApplicationProvider.getApplicationContext())
        .setWorkerFactory(ConfigDownloadWorkerFactory())
        .build()
    val config: Configuration =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    // Initialize WorkManager for instrumentation tests.
    WorkManagerTestInitHelper.initializeTestWorkManager(
      ApplicationProvider.getApplicationContext(),
      config,
    )
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testThatDoWorkCallsDownloadNonWorkflowConfigs() {
    coEvery { configurationRegistry.fetchNonWorkflowConfigResources() } just runs
    coEvery { dataMigration.migrate() } just runs
    runTest {
      val result = configDownloadWorker.doWork()
      coVerify { configurationRegistry.fetchNonWorkflowConfigResources() }
      Assert.assertEquals(ListenableWorker.Result.success(), result)
    }
  }

  inner class ConfigDownloadWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters,
    ): ListenableWorker {
      return ConfigDownloadWorker(
        appContext = appContext,
        workerParams = workerParameters,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = dispatcherProvider,
        defaultRepository = defaultRepository,
        dataMigration = dataMigration,
      )
    }
  }
}
