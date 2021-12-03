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

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.hl7.fhir.r4.model.Observation
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.HiltActivityForTest
import org.smartregister.fhircore.anc.app.fakes.FakeModel
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper

@HiltAndroidTest
class AncDetailsFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val activityScenarioRule = ActivityScenarioRule(HiltActivityForTest::class.java)

  @get:Rule(order = 2) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 3) val coroutineTestRule = CoroutineTestRule()

  @BindValue val patientRepository: PatientRepository = mockk()

  private lateinit var ancDetailsFragment: AncDetailsFragment

  @Before
  fun setUp() {
    hiltRule.inject()
    initPatientRepositoryMocks()
  }

  private fun initPatientRepositoryMocks() {
    every { patientRepository.setAncItemMapperType(any()) } just runs
    patientRepository.setAncItemMapperType(AncItemMapper.AncItemMapperType.DETAILS)
    coEvery { patientRepository.searchCarePlan(any()) } returns listOf(FakeModel.buildCarePlan("1"))
    coEvery { patientRepository.fetchCarePlan(any()) } returns listOf(FakeModel.buildCarePlan("1"))
    coEvery { patientRepository.fetchObservations(any(), "ga") } returns FakeModel.getObservation()
    coEvery { patientRepository.fetchEncounters(any()) } returns listOf(FakeModel.getEncounter("1"))
    coEvery { patientRepository.fetchDemographics(any()) } returns
      PatientDetailItem(
        patientDetails =
          PatientItem(gender = "Male", name = "Martha Mary", age = "27", isPregnant = true),
        patientDetailsHead =
          PatientItem(gender = "Male", name = "John Jared", age = "26", isPregnant = false)
      )
  }

  private fun launchAncDetailsFragment() {
    activityScenarioRule.scenario.onActivity {
      it.supportFragmentManager.commitNow {
        add(
          AncDetailsFragment.newInstance().also { detailsFragment ->
            ancDetailsFragment = detailsFragment
          },
          AncDetailsFragment.TAG
        )
      }
    }
  }

  @Test
  fun testHandleObservation() {
    coEvery { patientRepository.fetchObservations(any(), "edd") } returns Observation()
    coEvery { patientRepository.fetchObservations(any(), "risk") } returns
      FakeModel.getObservation(testValue = 1)
    coEvery { patientRepository.fetchObservations(any(), "fetuses") } returns
      FakeModel.getObservation(testValue = 2)
    coEvery { patientRepository.fetchObservations(any(), "ga") } returns
      FakeModel.getObservation(testValue = 25)

    launchAncDetailsFragment()
    Assert.assertNotNull(ancDetailsFragment)
    ancDetailsFragment.ancDetailsViewModel.fetchObservation("1")

    val viewBinding = ancDetailsFragment.viewBinding
    Assert.assertEquals("", viewBinding.txtViewEDDDoseDate.text)
    Assert.assertEquals("25", viewBinding.txtViewGAPeriod.text)
    Assert.assertEquals("2", viewBinding.txtViewFetusesCount.text.toString())
    Assert.assertEquals("1", viewBinding.txtViewRiskValue.text.toString())
  }

  @Test
  fun testThatViewsWereCorrectlySetup() {
    launchAncDetailsFragment()
    val viewBinding = ancDetailsFragment.viewBinding
    Assert.assertEquals(View.GONE, viewBinding.txtViewNoLastSeenServices.visibility)
    Assert.assertEquals(View.VISIBLE, viewBinding.upcomingServicesListView.visibility)
    Assert.assertEquals(View.VISIBLE, viewBinding.txtViewUpcomingServicesSeeAllHeading.visibility)
    Assert.assertEquals(View.VISIBLE, viewBinding.imageViewUpcomingServicesSeeAllArrow.visibility)
  }

  @After
  fun tearDown() {
    activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)
  }
}
