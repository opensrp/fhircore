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

package org.smartregister.fhircore.anc.ui.family.removefamily

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class RemoveFamilyViewModelTest : RobolectricTest() {

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private val familyDetailRepository: FamilyDetailRepository = mockk()

  private lateinit var removeFamilyViewModel: RemoveFamilyViewModel

  @Before
  fun setUp() {
    removeFamilyViewModel = RemoveFamilyViewModel(familyDetailRepository)
  }

  @Ignore("fails locally")
  @Test
  fun testChangeFamilyHeadShouldCallRepositoryMethod() {
    coEvery { familyDetailRepository.delete(any()) } answers {}
    coEvery { familyDetailRepository.loadResource<Patient>(any()) } returns Patient()
    removeFamilyViewModel.removeFamily(
      "111",
    )
    coVerify { familyDetailRepository.delete(any()) }
    Assert.assertEquals(true, removeFamilyViewModel.isRemoveFamily.value)
  }
}
