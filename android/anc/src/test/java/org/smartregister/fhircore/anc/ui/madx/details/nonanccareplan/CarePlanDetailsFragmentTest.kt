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

package org.smartregister.fhircore.anc.ui.madx.details.nonanccareplan

import android.view.View
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.robolectric.FragmentRobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.ui.madx.details.NonAncDetailsActivity
import org.smartregister.fhircore.anc.ui.madx.details.adapter.CarePlanAdapter
import org.smartregister.fhircore.anc.ui.madx.details.adapter.UpcomingServicesAdapter

@ExperimentalCoroutinesApi
@Config(shadows = [AncApplicationShadow::class])
internal class CarePlanDetailsFragmentTest : FragmentRobolectricTest() {

  private lateinit var fhirEngine: FhirEngine

  private lateinit var patientDetailsViewModel: CarePlanDetailsViewModel

  private lateinit var patientDetailsActivity: NonAncDetailsActivity

  private lateinit var patientRepository: PatientRepository

  private lateinit var fragmentScenario: FragmentScenario<CarePlanDetailsFragment>

  private lateinit var patientDetailsFragment: CarePlanDetailsFragment

  private lateinit var carePlanAdapter: CarePlanAdapter

  private lateinit var upcomingServicesAdapter: UpcomingServicesAdapter

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val patientId = "samplePatientId"
  var ancPatientDetailItem = spyk<AncPatientDetailItem>()

  @Before
  fun setUp() {

    fhirEngine = mockk(relaxed = true)

    patientRepository = mockk()
    carePlanAdapter = mockk()
    upcomingServicesAdapter = mockk()

    every { carePlanAdapter.submitList(any()) } returns Unit
    every { upcomingServicesAdapter.submitList(any()) } returns Unit

    every { ancPatientDetailItem.patientDetails } returns
      AncPatientItem(patientId, "Mandela Nelson", "M", "26")
    every { ancPatientDetailItem.patientDetailsHead } returns AncPatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    patientDetailsViewModel =
      spyk(
        CarePlanDetailsViewModel(
          patientRepository,
          coroutinesTestRule.testDispatcherProvider,
          patientId
        )
      )

    patientDetailsActivity =
      Robolectric.buildActivity(NonAncDetailsActivity::class.java).create().get()
    fragmentScenario =
      launchFragmentInContainer(
        factory =
          object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
              val fragment = spyk(CarePlanDetailsFragment.newInstance())
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

    // CarePlan list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.carePlanListView)

    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleCarePlan",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(CarePlanItem("1111", "", due = true, overdue = false))
      )
    )

    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.VISIBLE, immunizationsListView?.visibility)

    verify(exactly = 1) { carePlanAdapter.submitList(any()) }
  }

  @Test
  fun testHandleEncounterShouldVerifyExpectedCalls() {

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleEncounters",
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
      "handleEncounters",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(UpcomingServiceItem("1111", "ABC", "2020-02-01"))
      )
    )

    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.VISIBLE, immunizationsListView?.visibility)

    verify(exactly = 1) { upcomingServicesAdapter.submitList(any()) }
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  override fun getFragment(): Fragment {
    return patientDetailsFragment
  }

  @Test
  fun testThatCarePlansAreUpdated() {
    coroutinesTestRule.runBlockingTest { fragmentScenario.moveToState(Lifecycle.State.RESUMED) }
  }
}
