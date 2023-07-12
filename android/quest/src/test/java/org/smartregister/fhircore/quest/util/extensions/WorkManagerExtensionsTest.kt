/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.util.extensions

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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.task.FhirTaskStatusUpdateWorker
import org.smartregister.fhircore.engine.task.FhirTaskUtil
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class WorkManagerExtensionsTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var defaultRepository: DefaultRepository
  private lateinit var fhirTaskStatusUpdateWorker: FhirTaskStatusUpdateWorker
  private val fhirTaskUtil: FhirTaskUtil = mockk(relaxed = true)

  @Before
  fun setup() {
    hiltRule.inject()
    fhirTaskStatusUpdateWorker =
      TestListenableWorkerBuilder<FhirTaskStatusUpdateWorker>(
          ApplicationProvider.getApplicationContext(),
        )
        .setWorkerFactory(FhirTaskPlanWorkerFactory())
        .build()
  }

  @Test
  fun schedulePeriodicallyShouldEnqueueWork() {
    val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
    workManager.schedulePeriodically<FhirTaskStatusUpdateWorker>(
      workId = FhirTaskStatusUpdateWorker.WORK_ID,
      repeatInterval = 45,
      existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE,
    )

    val workInfo =
      workManager.getWorkInfosForUniqueWork(FhirTaskStatusUpdateWorker.WORK_ID).get()[0]

    Assert.assertNotNull(workInfo.id)
  }

  inner class FhirTaskPlanWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters,
    ): ListenableWorker {
      return FhirTaskStatusUpdateWorker(
        appContext = appContext,
        workerParams = workerParameters,
        fhirTaskUtil = fhirTaskUtil,
        dispatcherProvider =
          this@WorkManagerExtensionsTest.coroutineTestRule.testDispatcherProvider,
      )
    }
  }
}
