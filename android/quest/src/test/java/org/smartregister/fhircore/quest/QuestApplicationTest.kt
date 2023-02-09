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

package org.smartregister.fhircore.quest

import androidx.hilt.work.HiltWorkerFactory
import com.google.android.fhir.datacapture.DataCaptureConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestApplicationTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var workerFactory: HiltWorkerFactory

  private lateinit var application: QuestApplication
  @Before
  fun setUp() {
    hiltRule.inject()
    application = spyk(QuestApplication())
  }

  @Test
  fun testGetDataCaptureConfig() {
    every { application.getDataCaptureConfig() } returns DataCaptureConfig()

    val config = application.getDataCaptureConfig()
    verify { application.getDataCaptureConfig() }
    assertNotNull(config)
  }

  @Test
  fun testGetWorkManagerConfiguration() {
    every { application.workerFactory } returns workerFactory
    val config = application.workManagerConfiguration
    verify { application.workManagerConfiguration }
    assertNotNull(config)
  }
}
