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
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 24-11-2022. */
@HiltAndroidTest
class FhirTaskExpireWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue var fhirTaskExpireUtil: FhirTaskExpireUtil = mockk()
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var fhirEngine: FhirEngine
  private lateinit var fhirTaskExpireWorker: FhirTaskExpireWorker
  lateinit var context: Context

  @Before
  fun setup() {
    hiltRule.inject()

    context = ApplicationProvider.getApplicationContext()
    initializeWorkManager()
    fhirTaskExpireWorker =
      TestListenableWorkerBuilder<FhirTaskExpireWorker>(context)
        .setWorkerFactory(FhirTaskExpireJobWorkerFactory())
        .build()
  }

  @Test
  fun doWorkShouldFetchTasksAndMarkAsExpired() {
    val date1 = DateTime().minusDays(4).toDate()
    val date2 = Date()
    val firstBatchTasks = mutableListOf(Task())
    val secondBatchTasks = mutableListOf(Task())

    coEvery { fhirTaskExpireUtil.fetchOverdueTasks() } returns Pair(date1, firstBatchTasks)
    coEvery { fhirTaskExpireUtil.fetchOverdueTasks(from = date1) } returns
      Pair(date2, secondBatchTasks)
    coEvery { fhirTaskExpireUtil.fetchOverdueTasks(from = date2) } returns
      Pair(null, mutableListOf())
    coEvery { fhirTaskExpireUtil.markTaskExpired(any()) } just runs

    val result = runBlocking { fhirTaskExpireWorker.doWork() }

    assertEquals(ListenableWorker.Result.success(), result)

    coVerify { fhirTaskExpireUtil.fetchOverdueTasks() }
    coVerify { fhirTaskExpireUtil.fetchOverdueTasks(from = date1) }
    coVerify { fhirTaskExpireUtil.fetchOverdueTasks(from = date2) }

    coVerify { fhirTaskExpireUtil.markTaskExpired(firstBatchTasks) }
    coVerify { fhirTaskExpireUtil.markTaskExpired(secondBatchTasks) }
  }

  private fun initializeWorkManager() {
    val config: Configuration =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    // Initialize WorkManager for instrumentation tests.
    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
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
