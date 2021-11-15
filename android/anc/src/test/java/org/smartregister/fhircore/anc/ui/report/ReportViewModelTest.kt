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

import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class ReportViewModelTest : RobolectricTest() {

  private lateinit var repository: ReportRepository
  private lateinit var viewModel: ReportViewModel

  @Before
  fun setUp() {
    repository = mockk()
    viewModel =
      ReportViewModel.get(
        Robolectric.buildActivity(ReportHomeActivity::class.java).get(),
        ApplicationProvider.getApplicationContext(),
        repository
      )
  }

  @Test
  fun testShouldVerifyBackClickListener() {
    viewModel.onBackPress()
    Assert.assertEquals(true, viewModel.backPress.value)
  }
}
