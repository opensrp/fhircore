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
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.FragmentRobolectricTest
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity

@ExperimentalCoroutinesApi
internal class AncDetailsFragmentTest : FragmentRobolectricTest() {

  private lateinit var fhirEngine: FhirEngine
  private lateinit var patientDetailsViewModel: AncDetailsViewModel
  private lateinit var patientDetailsActivity: PatientDetailsActivity
  private lateinit var patientRepository: PatientRepository
  private lateinit var fragmentScenario: FragmentScenario<AncDetailsFragment>
  private lateinit var patientDetailsFragment: AncDetailsFragment
  private lateinit var carePlanAdapter: CarePlanAdapter
  private lateinit var upcomingServicesAdapter: UpcomingServicesAdapter
  private lateinit var lastSeen: EncounterAdapter

  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val patientId = "samplePatientId"
  var ancPatientDetailItem = spyk<PatientDetailItem>()

  @Before
  fun setUp() {

    MockKAnnotations.init(this, relaxUnitFun = true)

    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()
    carePlanAdapter = mockk()
    upcomingServicesAdapter = mockk()
    lastSeen = mockk()

    every { carePlanAdapter.submitList(any()) } returns Unit
    every { upcomingServicesAdapter.submitList(any()) } returns Unit
    every { lastSeen.submitList(any()) } returns Unit
    every { ancPatientDetailItem.patientDetails } returns
      PatientItem(patientId, "Mandela Nelson", "M", "26")
    every { ancPatientDetailItem.patientDetailsHead } returns PatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    patientDetailsViewModel =
      spyk(
        AncDetailsViewModel(patientRepository, coroutinesTestRule.testDispatcherProvider, patientId)
      )

    patientDetailsActivity =
      Robolectric.buildActivity(PatientDetailsActivity::class.java).create().get()
    fragmentScenario =
      launchFragmentInContainer(
        factory =
          object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
              val fragment = spyk(AncDetailsFragment.newInstance())
              every { fragment.activity } returns patientDetailsActivity
              fragment.ancDetailsViewModel = patientDetailsViewModel

              return fragment
            }
          }
      )

    fragmentScenario.onFragment {
      patientDetailsFragment = it
      ReflectionHelpers.setField(patientDetailsFragment, "carePlanAdapter", carePlanAdapter)
      ReflectionHelpers.setField(
        patientDetailsFragment,
        "upcomingServicesAdapter",
        upcomingServicesAdapter
      )
      ReflectionHelpers.setField(patientDetailsFragment, "lastSeen", lastSeen)
    }
  }

  @Test
  fun testHandleCarePlanShouldVerifyExpectedCalls() {

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleCarePlan",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<CarePlanItem>())
    )

    // No CarePlan available text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noCarePlan)

    // CarePlan label is not displayed
    val carePlansLabel = patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_carePlan)

    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.GONE, carePlansLabel?.visibility)

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleCarePlan",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(CarePlanItem("1111", "", due = true, overdue = false))
      )
    )

    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.VISIBLE, carePlansLabel?.visibility)
  }

  @Test
  fun testHandleObservation() {
    val ancOverviewItem = AncOverviewItem("12-03-2022", "23", "1", "none")

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleObservation",
      ReflectionHelpers.ClassParameter(AncOverviewItem::class.java, ancOverviewItem)
    )

    val txtViewEDDDoseDate =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_EDDDoseDate)
    val txtViewGAPeriod = patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_GAPeriod)
    val txtViewFetusesCount =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_FetusesCount)
    val txtViewRiskValue =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_RiskValue)

    Assert.assertEquals(ancOverviewItem.edd, txtViewEDDDoseDate!!.text.toString())
    Assert.assertEquals(ancOverviewItem.ga, txtViewGAPeriod!!.text.toString())
    Assert.assertEquals(ancOverviewItem.noOfFetuses, txtViewFetusesCount!!.text.toString())
    Assert.assertEquals(ancOverviewItem.risk, txtViewRiskValue!!.text.toString())
  }

  @Test
  fun testHandleEncounterShouldVerifyExpectedCalls() {

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleUpcomingServices",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<UpcomingServiceItem>())
    )

    // No CarePlan available text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noUpcomingServices)

    // CarePlan list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.upcomingServicesListView)

    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleUpcomingServices",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(UpcomingServiceItem("1111", "ABC", "2020-02-01"))
      )
    )

    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.VISIBLE, immunizationsListView?.visibility)

    verify(exactly = 1) { upcomingServicesAdapter.submitList(any()) }
  }

  @Test
  fun testHandleLastSceneShouldVerifyExpectedCalls() {

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleLastSeen",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<UpcomingServiceItem>())
    )

    // No CarePlan available text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noLastSeenServices)

    // CarePlan list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.lastSeenListView)

    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleLastSeen",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(UpcomingServiceItem("1111", "ABC", "2020-02-01"))
      )
    )

    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.VISIBLE, immunizationsListView?.visibility)

    verify(exactly = 1) { lastSeen.submitList(any()) }
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  override fun getFragment(): Fragment {
    return patientDetailsFragment
  }
}
