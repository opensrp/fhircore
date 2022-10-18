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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.impl.WorkManagerImpl
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import org.hl7.fhir.r4.model.Coding
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers.setStaticField
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.sync.SyncStrategyTag
import org.smartregister.fhircore.engine.task.FhirTaskPlanWorker
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class ConfigServiceTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var gson: Gson

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private val configService = spyk(AppConfigService(ApplicationProvider.getApplicationContext()))

  @Before
  fun setUp() {
    hiltRule.inject()
    sharedPreferencesHelper = SharedPreferencesHelper(application, gson)
  }

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

  @Test
  fun testProvideMandatorySyncTags() {

    val practitionerId = "practitioner-id"
    sharedPreferencesHelper.write(SharedPreferenceKey.PRACTITIONER_ID.name, practitionerId)
    every { configService.provideSyncStrategies() } returns
      listOf(SharedPreferenceKey.PRACTITIONER_ID.name)
    every { configService.provideSyncStrategyTags() } returns
      listOf(
        SyncStrategyTag(
          type = SharedPreferenceKey.PRACTITIONER_ID.name,
          tag =
            Coding().apply {
              system = "http://fake.tag.com/Practitioner#system"
              display = "Practitioner "
            }
        )
      )

    val mandatorySyncTags = configService.provideMandatorySyncTags(sharedPreferencesHelper)
    Assert.assertEquals(practitionerId, mandatorySyncTags[0].code)
  }

  @Test
  fun testProvideMandatorySyncTagsForLocationSyncStrategy() {
    val locationId = "location-id1"
    sharedPreferencesHelper.write("Location", listOf(locationId))

    val mandatorySyncTags = configService.provideMandatorySyncTags(sharedPreferencesHelper)
    Assert.assertEquals(locationId, mandatorySyncTags[0].code)
  }
}
