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

package org.smartregister.fhircore.engine.app

import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.impl.WorkManagerImpl
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.robolectric.util.ReflectionHelpers.setStaticField
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.task.FhirTaskPlanWorker

@HiltAndroidTest
class ConfigServiceTest : RobolectricTest() {

  private val configService = AppConfigService(ApplicationProvider.getApplicationContext())

  @Test
  fun testSchedulePlanShouldEnqueueUniquePeriodicWork() {
    val workManager = mockk<WorkManagerImpl>()
    setStaticField(WorkManagerImpl::class.java, "sDelegatedInstance", workManager)

    every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } returns mockk()
    configService.scheduleFhirTaskPlanWorker(mockk())

    verify {
      workManager.enqueueUniquePeriodicWork(
        eq(FhirTaskPlanWorker.WORK_ID),
        eq(ExistingPeriodicWorkPolicy.REPLACE),
        any()
      )
    }
  }
}
