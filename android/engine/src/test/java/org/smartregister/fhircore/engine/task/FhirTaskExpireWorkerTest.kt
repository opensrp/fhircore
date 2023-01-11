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

package org.smartregister.fhircore.engine.task

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
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 24-11-2022. */
@HiltAndroidTest
class FhirTaskExpireWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @BindValue var fhirTaskExpireUtil: FhirTaskExpireUtil = mockk()
  private val fhirEngine: FhirEngine = mockk(relaxed = true)
  private lateinit var fhirTaskExpireWorker: FhirTaskExpireWorker

  @Before
  fun setup() {
    hiltRule.inject()
    initializeWorkManager()
    fhirTaskExpireWorker =
      TestListenableWorkerBuilder<FhirTaskExpireWorker>(ApplicationProvider.getApplicationContext())
        .setWorkerFactory(FhirTaskExpireJobWorkerFactory())
        .build()
  }

  @Test
  fun doWorkShouldFetchTasksAndMarkAsExpired() {

    coEvery { fhirTaskExpireUtil.expireOverdueTasks() } returns emptyList()

    val result = runBlocking { fhirTaskExpireWorker.doWork() }

    assertEquals(ListenableWorker.Result.success(), result)
  }

  private fun initializeWorkManager() {
    val config: Configuration =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    // Initialize WorkManager for instrumentation tests.
    WorkManagerTestInitHelper.initializeTestWorkManager(
      ApplicationProvider.getApplicationContext(),
      config
    )
  }

  inner class FhirTaskExpireJobWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters
    ): ListenableWorker {
      return FhirTaskExpireWorker(appContext, workerParameters, fhirEngine, fhirTaskExpireUtil)
    }
  }
}
