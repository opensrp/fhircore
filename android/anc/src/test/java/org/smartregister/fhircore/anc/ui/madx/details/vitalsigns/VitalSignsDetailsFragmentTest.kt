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

package org.smartregister.fhircore.anc.ui.madx.details.vitalsigns

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
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Encounter
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.FragmentRobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.ui.madx.details.NonAncDetailsActivity
import org.smartregister.fhircore.anc.ui.madx.details.adapter.EncounterAdapter

@ExperimentalCoroutinesApi
@Config(shadows = [AncApplicationShadow::class])
internal class VitalSignsDetailsFragmentTest : FragmentRobolectricTest() {

  private lateinit var fhirEngine: FhirEngine

  private lateinit var patientDetailsViewModel: VitalSignsDetailsViewModel

  private lateinit var patientDetailsActivity: NonAncDetailsActivity

  private lateinit var patientRepository: PatientRepository

  private lateinit var fragmentScenario: FragmentScenario<VitalSignsDetailsFragment>

  private lateinit var patientDetailsFragment: VitalSignsDetailsFragment

  private lateinit var encounterAdapter: EncounterAdapter

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val patientId = "samplePatientId"
  var ancPatientDetailItem = spyk<AncPatientDetailItem>()
  @Before
  fun setUp() {

    fhirEngine = mockk(relaxed = true)

    patientRepository = mockk()

    encounterAdapter = mockk()

    every { encounterAdapter.submitList(any()) } returns Unit

    every { ancPatientDetailItem.patientDetails } returns
      AncPatientItem(patientId, "Mandela Nelson", "M", "26")
    every { ancPatientDetailItem.patientDetailsHead } returns AncPatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    patientDetailsViewModel =
      spyk(
        VitalSignsDetailsViewModel(
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
              val fragment = spyk(VitalSignsDetailsFragment.newInstance())
              every { fragment.activity } returns patientDetailsActivity
              fragment.ancDetailsViewModel = patientDetailsViewModel
              return fragment
            }
          }
      )

    fragmentScenario.onFragment {
      patientDetailsFragment = it
      ReflectionHelpers.setField(patientDetailsFragment, "encounterAdapter", encounterAdapter)
    }
  }

  @Test
  fun testHandleEncounterShouldVerifyExpectedCalls() {

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleEncounters",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<EncounterItem>())
    )

    // No CarePlan available text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noEncounter)

    // CarePlan list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.encounterListView)

    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsFragment,
      "handleEncounters",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(EncounterItem("1111", Encounter.EncounterStatus.ARRIVED, "first", Date()))
      )
    )

    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.VISIBLE, immunizationsListView?.visibility)

    verify(exactly = 1) { encounterAdapter.submitList(any()) }
  }

  @Test
  fun testThatEncounterViewsAreSetupCorrectly() {
    fragmentScenario.moveToState(Lifecycle.State.RESUMED)
    Assert.assertNotNull(patientDetailsFragment.view)

    // None text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noEncounter)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("None", noVaccinesTextView?.text.toString())

    // Encounter list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.encounterListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }

  @Test
  fun testThatAllergiesViewsAreSetupCorrectly() {
    fragmentScenario.moveToState(Lifecycle.State.RESUMED)
    Assert.assertNotNull(patientDetailsFragment.view)

    // None text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noAllergies)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("None", noVaccinesTextView?.text.toString())

    // Allergies list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.allergiesListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }

  @Test
  fun testThatConditionsViewsAreSetupCorrectly() {
    fragmentScenario.moveToState(Lifecycle.State.RESUMED)
    Assert.assertNotNull(patientDetailsFragment.view)

    // None text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noConditions)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("None", noVaccinesTextView?.text.toString())

    // Conditions list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.conditionsListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  override fun getFragment(): Fragment {
    return patientDetailsFragment
  }

  @Test
  fun testThatEncountersViewAreUpdated() {
    coroutinesTestRule.runBlockingTest { fragmentScenario.moveToState(Lifecycle.State.RESUMED) }
  }
}
