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

package org.smartregister.fhircore.anc.ui.details.careplan

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
@HiltAndroidTest
class CarePlanDetailsViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule(order = 2) val coroutinesTestRule = CoroutineTestRule()

  private lateinit var fhirEngine: FhirEngine
  private lateinit var patientDetailsViewModel: CarePlanDetailsViewModel
  private lateinit var patientRepository: PatientRepository
  private val patientId = "samplePatientId"

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()

    patientDetailsViewModel =
      spyk(CarePlanDetailsViewModel(patientRepository, coroutinesTestRule.testDispatcherProvider))

    patientDetailsViewModel.patientId = patientId
  }

  @Test
  fun testFetchCarePlanShouldReturnCarePlanItemList() {
    coEvery { patientRepository.fetchCarePlan(any()) } returns listOf()
    coEvery { patientRepository.fetchCarePlanItem(any()) } returns
      listOf(CarePlanItem("1", "CP-Title", due = false, overdue = false))

    val itemList = patientDetailsViewModel.fetchCarePlan().value
    Assert.assertEquals(1, itemList?.size)

    with(itemList?.get(0)!!) {
      Assert.assertEquals("1", carePlanIdentifier)
      Assert.assertEquals("CP-Title", title)
      Assert.assertFalse(due)
      Assert.assertFalse(overdue)
    }
  }

  @Test
  fun testFetchEncountersShouldReturnEncountersItemList() {
    coEvery { patientRepository.fetchCarePlan(any()) } returns listOf()
    coEvery { patientRepository.fetchUpcomingServiceItem(any()) } returns
      listOf(UpcomingServiceItem("1", "Title"))

    val itemList = patientDetailsViewModel.fetchEncounters().value
    Assert.assertEquals(1, itemList?.size)

    with(itemList?.get(0)!!) {
      Assert.assertEquals("1", encounterIdentifier)
      Assert.assertEquals("Title", title)
    }
  }
}
