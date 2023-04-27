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

package org.smartregister.fhircore.engine.sync

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class AppTimeStampContextTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  @Inject lateinit var sharedPreference: SharedPreferencesHelper
  private lateinit var appTimeStampContext: AppTimeStampContext

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    appTimeStampContext = AppTimeStampContext(sharedPreference)
  }

  @Test
  fun testGetLastUpdateTimestampShouldReturnTimestamp() {
    runTest {
      // No timestamp saved for ResourceType Patient
      Assert.assertNull(appTimeStampContext.getLasUpdateTimestamp(ResourceType.Patient))

      val currentTimestamp = OffsetDateTime.now().toString()
      appTimeStampContext.saveLastUpdatedTimestamp(ResourceType.Patient, currentTimestamp)
      Assert.assertEquals(
        currentTimestamp,
        appTimeStampContext.getLasUpdateTimestamp(ResourceType.Patient)
      )
    }
  }

  @Test
  fun saveLastUpdatedTimestampShouldSaveTimestampForResource() {
    runTest {
      val currentTimestamp = OffsetDateTime.now().toString()
      appTimeStampContext.saveLastUpdatedTimestamp(ResourceType.CarePlan, currentTimestamp)
      Assert.assertEquals(
        currentTimestamp,
        sharedPreference.read("CAREPLAN_LAST_SYNC_TIMESTAMP", null)
      )
    }
  }
}
