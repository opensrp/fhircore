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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
@HiltAndroidTest
class RemoveFamilyViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) var coroutineRule = CoroutineTestRule()

  private val familyDetailRepository: FamilyDetailRepository = mockk(relaxed = true)

  private lateinit var removeFamilyViewModel: RemoveFamilyViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    removeFamilyViewModel = RemoveFamilyViewModel(familyDetailRepository)

    coEvery { familyDetailRepository.fetchDemographics("111") } returns getPatient()
  }

  private fun getPatient(): Patient {
    val patient =
      Patient().apply {
        id = "111"
        name =
          listOf(
            HumanName().apply {
              given = listOf(StringType("john"))
              family = "doe"
            }
          )
      }
    return patient
  }

  @Test
  fun testDiscardRemoveFamily() {
    removeFamilyViewModel.discardRemovingFamily()
    Assert.assertEquals(true, removeFamilyViewModel.discardRemoving.value)
  }

  @Test
  fun testRemoveFamilyShouldCallRepositoryMethod() {
    removeFamilyViewModel.removeFamily("111")
    Assert.assertEquals(true, removeFamilyViewModel.isRemoveFamily.value)
  }

  @Test
  fun testRemoveFamilyShouldCallRepositoryMethodWithError() {
    removeFamilyViewModel.removeFamily("111")
    Assert.assertEquals(false, removeFamilyViewModel.discardRemoving.value)
  }

  @Test
  fun testFetchFamilyName() {
    removeFamilyViewModel.fetchFamilyName("1234")
    Assert.assertNotNull(removeFamilyViewModel.demographics.value)
  }
}
