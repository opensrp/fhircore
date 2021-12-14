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
import androidx.core.content.ContextCompat
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
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
import org.hl7.fhir.r4.model.Encounter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.app.fakes.FakeModel
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.util.extension.plusYears

@HiltAndroidTest
class AncDetailsFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val activityScenarioRule = ActivityScenarioRule(HiltActivityForTest::class.java)

  @get:Rule(order = 2) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 3) val coroutineTestRule = CoroutineTestRule()

  @BindValue val patientRepository: PatientRepository = mockk()
  @BindValue val upcomingServiceAdapter = spyk(UpcomingServicesAdapter())
  @BindValue val encounterAdapter = spyk(EncounterAdapter())

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
          PatientItem(
            gender = "Male",
            name = "Martha Mary",
            birthDate = Date().plusYears(-27),
            isPregnant = true
          ),
        patientDetailsHead =
          PatientItem(
            gender = "Male",
            name = "John Jared",
            birthDate = Date().plusYears(-26),
            isPregnant = false
          )
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

    launchAncDetailsFragment()
    Assert.assertNotNull(ancDetailsFragment)

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "handleObservation",
      ReflectionHelpers.ClassParameter(
        AncOverviewItem::class.java,
        AncOverviewItem("", "25", "2", "1")
      )
    )

    with(ancDetailsFragment.viewBinding) {
      Assert.assertEquals("", txtViewEDDDoseDate.text)
      Assert.assertEquals("25", txtViewGAPeriod.text)
      Assert.assertEquals("2", txtViewFetusesCount.text.toString())
      Assert.assertEquals("1", txtViewRiskValue.text.toString())
    }
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

  @Test
  fun testHandleUpcomingServices() {

    launchAncDetailsFragment()
    val viewBinding = ancDetailsFragment.viewBinding

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "handleUpcomingServices",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<UpcomingServiceItem>())
    )

    with(viewBinding) {
      Assert.assertEquals(View.VISIBLE, txtViewNoUpcomingServices.visibility)
      Assert.assertEquals(View.GONE, upcomingServicesListView.visibility)
      Assert.assertEquals(View.GONE, txtViewUpcomingServicesSeeAllHeading.visibility)
      Assert.assertEquals(View.GONE, imageViewUpcomingServicesSeeAllArrow.visibility)
    }

    every { upcomingServiceAdapter.submitList(any()) } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "handleUpcomingServices",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(UpcomingServiceItem("1", "Main Item", "2021-01-01"))
      )
    )

    with(viewBinding) {
      Assert.assertEquals(View.GONE, txtViewNoUpcomingServices.visibility)
      Assert.assertEquals(View.VISIBLE, upcomingServicesListView.visibility)
      Assert.assertEquals(View.VISIBLE, txtViewUpcomingServicesSeeAllHeading.visibility)
      Assert.assertEquals(View.VISIBLE, imageViewUpcomingServicesSeeAllArrow.visibility)
    }

    verify(exactly = 1) { upcomingServiceAdapter.submitList(any()) }
  }

  @Test
  fun testHandleLastSeen() {

    launchAncDetailsFragment()
    val viewBinding = ancDetailsFragment.viewBinding

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "handleLastSeen",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<EncounterItem>())
    )

    with(viewBinding) {
      Assert.assertEquals(View.VISIBLE, txtViewNoLastSeenServices.visibility)
      Assert.assertEquals(View.GONE, lastSeenListView.visibility)
    }

    every { encounterAdapter.submitList(any()) } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "handleLastSeen",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(EncounterItem("1", Encounter.EncounterStatus.FINISHED, "", Date()))
      )
    )

    with(viewBinding) {
      Assert.assertEquals(View.GONE, txtViewNoLastSeenServices.visibility)
      Assert.assertEquals(View.VISIBLE, lastSeenListView.visibility)
    }

    verify(exactly = 1) { encounterAdapter.submitList(any()) }
  }

  @Test
  fun testHandleCarePlan() {

    launchAncDetailsFragment()
    val viewBinding = ancDetailsFragment.viewBinding

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "handleCarePlan",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<CarePlanItem>())
    )

    with(viewBinding) {
      Assert.assertEquals(View.VISIBLE, txtViewNoCarePlan.visibility)
      Assert.assertEquals(View.GONE, txtViewCarePlanSeeAllHeading.visibility)
      Assert.assertEquals(View.GONE, imageViewSeeAllArrow.visibility)
      Assert.assertEquals(View.GONE, txtViewCarePlan.visibility)
    }

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "handleCarePlan",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(CarePlanItem("1", "Main Title", due = false, overdue = false))
      )
    )

    with(viewBinding) {
      Assert.assertEquals(View.GONE, txtViewNoCarePlan.visibility)
      Assert.assertEquals(View.VISIBLE, txtViewCarePlanSeeAllHeading.visibility)
      Assert.assertEquals(View.VISIBLE, imageViewSeeAllArrow.visibility)
      Assert.assertEquals(View.VISIBLE, txtViewCarePlan.visibility)
    }
  }

  @Test
  fun testPopulateImmunizationListShouldUpdateBasedOnProperty() {

    launchAncDetailsFragment()
    val viewBinding = ancDetailsFragment.viewBinding

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "populateImmunizationList",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(CarePlanItem("", "", due = false, overdue = true))
      )
    )

    with(viewBinding) {
      Assert.assertEquals(
        ancDetailsFragment.getString(R.string.anc_record_visit_with_overdue, 1),
        txtViewCarePlan.text
      )
      Assert.assertEquals(
        ContextCompat.getColor(ancDetailsFragment.requireContext(), R.color.status_red),
        txtViewCarePlan.currentTextColor
      )
    }

    ReflectionHelpers.callInstanceMethod<Any>(
      ancDetailsFragment,
      "populateImmunizationList",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(CarePlanItem("", "", due = true, overdue = false))
      )
    )

    with(viewBinding) {
      Assert.assertEquals(
        ancDetailsFragment.getString(R.string.anc_record_visit),
        txtViewCarePlan.text
      )
      Assert.assertEquals(
        ContextCompat.getColor(ancDetailsFragment.requireContext(), R.color.colorPrimaryLight),
        txtViewCarePlan.currentTextColor
      )
    }
  }

  @After
  fun tearDown() {
    activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)
  }
}
