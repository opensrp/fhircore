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

import android.view.View
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.Encounter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class VitalSignsDetailsFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutinesTestRule = CoroutineTestRule()

  @get:Rule(order = 2) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @BindValue val patientRepository: PatientRepository = mockk(relaxUnitFun = true)

  private lateinit var patientDetailsActivityController: ActivityController<PatientDetailsActivity>

  private lateinit var vitalSignsDetailsFragment: VitalSignsDetailsFragment

  private val patientId = "samplePatientId"

  private var ancPatientDetailItem = spyk<PatientDetailItem>()

  @Before
  fun setUp() {
    hiltRule.inject()
    every { patientRepository.setAncItemMapperType(AncItemMapper.AncItemMapperType.DETAILS) } just
      runs
    patientRepository.setAncItemMapperType(AncItemMapper.AncItemMapperType.DETAILS)
    every { ancPatientDetailItem.patientDetails } returns
      PatientItem(patientIdentifier = patientId, name = "Mandela Nelson", gender = "M", age = "26")
    every { ancPatientDetailItem.patientDetailsHead } returns PatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    patientDetailsActivityController = Robolectric.buildActivity(PatientDetailsActivity::class.java)

    val patientDetailsActivity = patientDetailsActivityController.create().resume().get()

    vitalSignsDetailsFragment = VitalSignsDetailsFragment.newInstance(bundleOf())

    patientDetailsActivity.supportFragmentManager.commitNow {
      add(vitalSignsDetailsFragment, VitalSignsDetailsFragment.TAG)
    }
  }

  @After
  fun tearDown() {
    patientDetailsActivityController.destroy()
  }

  @Test
  fun testHandleEncounterShouldVerifyExpectedCalls() {

    ReflectionHelpers.callInstanceMethod<Any>(
      vitalSignsDetailsFragment,
      "handleEncounters",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<EncounterItem>())
    )

    // No CarePlan available text displayed
    val noVaccinesTextView =
      vitalSignsDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noEncounter)

    // CarePlan list is not displayed
    val immunizationsListView =
      vitalSignsDetailsFragment.view?.findViewById<RecyclerView>(R.id.encounterListView)

    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)

    ReflectionHelpers.callInstanceMethod<Any>(
      vitalSignsDetailsFragment,
      "handleEncounters",
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(EncounterItem("1111", Encounter.EncounterStatus.ARRIVED, "first", Date()))
      )
    )

    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.VISIBLE, immunizationsListView?.visibility)
  }

  @Test
  fun testThatEncounterViewsAreSetupCorrectly() {
    Assert.assertNotNull(vitalSignsDetailsFragment.view)

    // None text displayed
    val noVaccinesTextView =
      vitalSignsDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noEncounter)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("None", noVaccinesTextView?.text.toString())

    // Encounter list is not displayed
    val immunizationsListView =
      vitalSignsDetailsFragment.view?.findViewById<RecyclerView>(R.id.encounterListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }

  @Test
  fun testThatAllergiesViewsAreSetupCorrectly() {
    Assert.assertNotNull(vitalSignsDetailsFragment.view)

    // None text displayed
    val noVaccinesTextView =
      vitalSignsDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noAllergies)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("None", noVaccinesTextView?.text.toString())

    // Allergies list is not displayed
    val immunizationsListView =
      vitalSignsDetailsFragment.view?.findViewById<RecyclerView>(R.id.allergiesListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }

  @Test
  fun testThatConditionsViewsAreSetupCorrectly() {
    Assert.assertNotNull(vitalSignsDetailsFragment.view)

    // None text displayed
    val noVaccinesTextView =
      vitalSignsDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noConditions)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("None", noVaccinesTextView?.text.toString())

    // Conditions list is not displayed
    val immunizationsListView =
      vitalSignsDetailsFragment.view?.findViewById<RecyclerView>(R.id.conditionsListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }
}
