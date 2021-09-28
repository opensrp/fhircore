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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.madx.NonAncPatientRepository
import org.smartregister.fhircore.anc.data.madx.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.madx.model.AncPatientItem
import org.smartregister.fhircore.anc.robolectric.FragmentRobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.ui.madx.details.NonAncDetailsActivity

@ExperimentalCoroutinesApi
@Config(shadows = [AncApplicationShadow::class])
internal class CarePlanDetailsFragmentTest : FragmentRobolectricTest() {

  private lateinit var fhirEngine: FhirEngine

  private lateinit var patientDetailsViewModel: CarePlanDetailsViewModel

  private lateinit var patientDetailsActivity: NonAncDetailsActivity

  private lateinit var patientRepository: NonAncPatientRepository

  private lateinit var fragmentScenario: FragmentScenario<CarePlanDetailsFragment>

  private lateinit var patientDetailsFragment: CarePlanDetailsFragment

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val patientId = "samplePatientId"
  var ancPatientDetailItem = spyk<AncPatientDetailItem>()

  @Before
  fun setUp() {

    fhirEngine = mockk(relaxed = true)

    patientRepository = mockk()

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

    fragmentScenario.onFragment { patientDetailsFragment = it }
  }

  @Test
  fun testThatCarePlanViewsAreSetupCorrectly() {
    fragmentScenario.moveToState(Lifecycle.State.RESUMED)
    Assert.assertNotNull(patientDetailsFragment.view)

    // No care plan  text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noCarePlan)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("No care plan", noVaccinesTextView?.text.toString())

    // CarePlan list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.carePlanListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }

  @Test
  fun testThatUpcomingServicesViewsAreSetupCorrectly() {
    fragmentScenario.moveToState(Lifecycle.State.RESUMED)
    Assert.assertNotNull(patientDetailsFragment.view)

    // No services scheduled text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noUpcomingServices)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("No services scheduled", noVaccinesTextView?.text.toString())

    // Upcoming list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.upcomingServicesListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
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
