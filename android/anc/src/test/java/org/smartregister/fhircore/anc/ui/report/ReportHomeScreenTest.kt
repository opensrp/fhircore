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

package org.smartregister.fhircore.anc.ui.report

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow

@Config(shadows = [AncApplicationShadow::class])
class ReportHomeScreenTest : RobolectricTest() {

  private val app = ApplicationProvider.getApplicationContext<Application>()
  private lateinit var repository: ReportRepository
  private lateinit var viewModel: ReportViewModel
  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @Before
  fun setUp() {
    repository = mockk()
    viewModel =
      spyk(
        objToCopy =
          ReportViewModel(
            ApplicationProvider.getApplicationContext(),
            coroutinesTestRule.testDispatcherProvider
          )
      )
  }

  @Test
  @Ignore("composeRule.setContent is failing")
  fun testReportHomeScreenComponents() {
    // toolbar should have valid title and icon

  }
}
