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

package org.smartregister.fhircore.anc.ui.anccare.details

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.app.fakes.FakeModel
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class AncDetailsViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) var coroutinesTestRule = CoroutineTestRule()

  private lateinit var fhirEngine: FhirEngine

  private lateinit var ancDetailsViewModel: AncDetailsViewModel

  private lateinit var patientRepository: PatientRepository

  private val patientId = "samplePatientId"

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()

    val ancPatientDetailItem = spyk<PatientDetailItem>()

    every { ancPatientDetailItem.patientDetails } returns
      PatientItem(patientId, "Mandela Nelson", "fam", "M", Date().plusYears(-26))
    every { ancPatientDetailItem.patientDetailsHead } returns PatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    ancDetailsViewModel =
      spyk(AncDetailsViewModel(patientRepository, coroutinesTestRule.testDispatcherProvider))
  }

  @Test
  fun fetchDemographics() {
    coroutinesTestRule.runBlockingTest {
      val patient =
        spyk<Patient>().apply {
          idElement.id = patientId
          birthDate = Date().plusYears(-26)
        }
      coEvery { fhirEngine.get(ResourceType.Patient, patientId) } returns patient
      val patientDetailItem: PatientDetailItem =
        ancDetailsViewModel.fetchDemographics(patientId).value!!
      Assert.assertNotNull(patientDetailItem)
      Assert.assertEquals(patientDetailItem.patientDetails.patientIdentifier, patientId)
      val patientDetails =
        patientDetailItem.patientDetails.name +
          ", " +
          patientDetailItem.patientDetails.gender +
          ", " +
          patientDetailItem.patientDetails.birthDate.toAgeDisplay()
      val patientId = " ID: " + patientDetailItem.patientDetails.patientIdentifier

      Assert.assertEquals(patientDetails, "Mandela Nelson, M, 26y")
      Assert.assertEquals(patientId, " ID: samplePatientId")
    }
  }

  @Test
  fun testFetchCarePlanShouldReturnExpectedCarePlan() {

    coEvery { patientRepository.searchCarePlan(any()) } returns mockk()
    coEvery { patientRepository.fetchCarePlanItem(any()) } returns
      listOf(CarePlanItem("1", "First Care Plan", due = false, overdue = false))

    val carePlanList = ancDetailsViewModel.fetchCarePlan(patientId).value

    if (carePlanList != null && carePlanList.isNotEmpty()) {
      Assert.assertEquals(1, carePlanList.size)
      with(carePlanList.first()) {
        Assert.assertEquals("1", carePlanIdentifier)
        Assert.assertEquals("First Care Plan", title)
        Assert.assertFalse(due)
        Assert.assertFalse(overdue)
      }
    }
  }

  @Test
  fun testFetchObservationShouldReturnExpectedAncOverviewItem() {
    val eddDate = DateTimeType(Date())
    coEvery { patientRepository.fetchObservations(any(), "edd") } returns
      Observation().apply { value = eddDate }
    coEvery { patientRepository.fetchObservations(any(), "risk") } returns
      FakeModel.getObservation(testValue = 1)
    coEvery { patientRepository.fetchObservations(any(), "fetuses") } returns
      FakeModel.getObservation(testValue = 2)
    coEvery { patientRepository.fetchObservations(any(), "ga") } returns
      FakeModel.getObservation(testValue = 25)

    val ancOverviewItem = ancDetailsViewModel.fetchObservation("").value

    with(ancOverviewItem) {
      Assert.assertEquals(eddDate.value.makeItReadable(), this?.edd)
      Assert.assertEquals("25", this?.ga)
      Assert.assertEquals("2", this?.noOfFetuses)
      Assert.assertEquals("1", this?.risk)
    }
  }

  @Test
  fun testFetchUpcomingServicesShouldReturnExpectedUpcomingServiceItemList() {

    coEvery { patientRepository.fetchCarePlan(any()) } returns mockk()
    coEvery { patientRepository.fetchUpcomingServiceItem(any()) } returns
      listOf(UpcomingServiceItem("1", "Upcoming Service Title", ""))

    val upcomingServiceList = ancDetailsViewModel.fetchUpcomingServices(patientId).value

    Assert.assertEquals(1, upcomingServiceList?.size)
    with(upcomingServiceList?.first()) {
      Assert.assertEquals("1", this?.encounterIdentifier)
      Assert.assertEquals("Upcoming Service Title", this?.title)
      Assert.assertEquals("", this?.date)
    }
  }

  @Test
  fun testFetchLastSeenShouldReturnExpectedEncounterItemList() {

    coEvery { patientRepository.fetchEncounters(any()) } returns mockk()
    coEvery { patientRepository.fetchLastSeenItem(any()) } returns
      listOf(EncounterItem("1", Encounter.EncounterStatus.FINISHED, "completed", Date()))

    val lastSeenList = ancDetailsViewModel.fetchLastSeen(patientId).value

    Assert.assertEquals(1, lastSeenList?.size)
    with(lastSeenList?.first()) {
      Assert.assertEquals("1", this?.id)
      Assert.assertEquals(Encounter.EncounterStatus.FINISHED, this?.status)
      Assert.assertEquals("completed", this?.display)
    }
  }

  @Test
  fun testDeletePatientShouldCallPatientRepository() = runBlockingTest {
    coEvery { patientRepository.deletePatient("111", any()) } answers {}

    ancDetailsViewModel.deletePatient("111", DeletionReason.MOVED_OUT)

    coVerify { patientRepository.deletePatient("111", any()) }
  }

  @Test
  fun testMarkDeceasedShouldCallPatientRepository() = runBlockingTest {
    coEvery { patientRepository.markDeceased("111", any()) } answers {}

    ancDetailsViewModel.markDeceased("111", Date())

    coVerify { patientRepository.markDeceased("111", any()) }
  }
}
