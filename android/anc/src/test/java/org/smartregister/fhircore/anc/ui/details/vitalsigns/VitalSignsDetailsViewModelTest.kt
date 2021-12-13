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

package org.smartregister.fhircore.anc.ui.details.vitalsigns

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
@HiltAndroidTest
class VitalSignsDetailsViewModelTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)
  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule val coroutinesTestRule = CoroutineTestRule()

  private lateinit var fhirEngine: FhirEngine
  private lateinit var patientDetailsViewModel: VitalSignsDetailsViewModel
  private lateinit var patientRepository: PatientRepository

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()
    patientDetailsViewModel =
      spyk(VitalSignsDetailsViewModel(patientRepository, coroutinesTestRule.testDispatcherProvider))
  }

  @Test
  fun testFetchEncountersShouldReturnEncountersItemList() {
    val startDate = Date()
    coEvery { patientRepository.fetchEncounters(any()) } returns
      listOf(
        Encounter().apply {
          id = "1"
          status = Encounter.EncounterStatus.FINISHED
          period = Period().apply { start = startDate }
        }
      )

    val itemList = patientDetailsViewModel.fetchEncounters("").value
    Assert.assertEquals(1, itemList?.size)

    with(itemList?.get(0)!!) {
      Assert.assertEquals("1", id)
      Assert.assertEquals("", display)
      Assert.assertEquals(Encounter.EncounterStatus.FINISHED, status)
      Assert.assertEquals(startDate.time, periodStartDate?.time)
    }
  }

  @Test
  fun testFetchObservations() {
    coroutinesTestRule.runBlockingTest {
      val testObservation = getTestObservation()
      coEvery {
        hint(Observation::class)
        fhirEngine.search<Observation>(any())
      } returns listOf(testObservation)
      coEvery { patientDetailsViewModel.fetchObservation(any()) } returns
        MutableLiveData(getTestAncOverviewItem())
      coEvery { patientRepository.fetchObservations(any(), any()) } returns testObservation
      val ancOverviewItem = patientDetailsViewModel.fetchObservation("").value!!
      Assert.assertNotNull(ancOverviewItem)
      Assert.assertEquals(ancOverviewItem.height, getTestAncOverviewItem().height)
      Assert.assertEquals(ancOverviewItem.weight, getTestAncOverviewItem().weight)
      Assert.assertEquals(ancOverviewItem.bmi, getTestAncOverviewItem().bmi)
    }
  }

  private fun getTestObservation(): Observation {
    return Observation().apply { id = "2" }
  }

  private fun getTestAncOverviewItem(): AncOverviewItem {
    return AncOverviewItem().apply {
      height = "180 cm"
      weight = "73 kg"
      bmi = "22.54 kg/m"
    }
  }
}
