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

package org.smartregister.fhircore.anc.ui.anccare.encounters

import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.anc.data.EncounterRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@Ignore("Passing on local but failing CI")
class EncounterListViewModelTest : RobolectricTest() {

  private lateinit var repository: EncounterRepository
  private lateinit var viewModel: EncounterListViewModel

  @Before
  fun setUp() {
    repository = mockk()
    viewModel = EncounterListViewModel(mockk())
  }

  @Test
  fun testShouldVerifyBackClickListener() {
    var count = 0

    /*viewModel.setAppBackClickListener { ++count }
    viewModel.getAppBackClickListener().invoke()*/

    Assert.assertEquals(1, count)
  }
}
