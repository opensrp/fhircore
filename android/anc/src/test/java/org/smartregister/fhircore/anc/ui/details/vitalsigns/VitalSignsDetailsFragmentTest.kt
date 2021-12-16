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
import org.smartregister.fhircore.anc.data.model.PatientVitalItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.engine.HiltActivityForTest

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class VitalSignsDetailsFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val activityScenarioRule = ActivityScenarioRule(HiltActivityForTest::class.java)

  @get:Rule(order = 2) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 3) val coroutineTestRule = CoroutineTestRule()

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
      PatientItem(
        patientIdentifier = patientId,
        name = "Mandela Nelson",
        gender = "M",
        birthDate = Date().plusYears(-26)
      )
    every { ancPatientDetailItem.patientDetailsHead } returns PatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    patientDetailsActivityController = Robolectric.buildActivity(PatientDetailsActivity::class.java)

    val patientDetailsActivity = patientDetailsActivityController.create().resume().get()

    vitalSignsDetailsFragment = VitalSignsDetailsFragment.newInstance(bundleOf())

    patientDetailsActivity.supportFragmentManager.commitNow {
      add(vitalSignsDetailsFragment, VitalSignsDetailsFragment.TAG)
    }
  }

  private fun launchVitalSignsDetailsFragment() {
    activityScenarioRule.scenario.onActivity {
      it.supportFragmentManager.commitNow {
        add(
          VitalSignsDetailsFragment.newInstance().also { detailsFragment ->
            vitalSignsDetailsFragment = detailsFragment
          },
          VitalSignsDetailsFragment.TAG
        )
      }
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
  fun testHandleVitalSigns() {

    launchVitalSignsDetailsFragment()

    Assert.assertNotNull(vitalSignsDetailsFragment)

    ReflectionHelpers.callInstanceMethod<Any>(
      vitalSignsDetailsFragment,
      "handleVitalSigns",
      ReflectionHelpers.ClassParameter(
        PatientVitalItem::class.java,
        PatientVitalItem(
          "40",
          "kg",
          "34",
          "in",
          "86",
          "%",
          "45",
          "bgunit",
          "23",
          "bpunit",
          "23",
          "bpunit",
          "23",
          "pulseunit"
        )
      )
    )

    with(vitalSignsDetailsFragment.binding) {
      Assert.assertEquals("40", txtViewWeightValue.text.toString())
      Assert.assertEquals("kg", txtViewWeightUnit.text.toString())
      Assert.assertEquals("34", txtViewHeightValue.text.toString())
      Assert.assertEquals("in", txtViewHeightUnit.text.toString())
      Assert.assertEquals("86", txtViewSpValue.text.toString())
      Assert.assertEquals("%", txtViewSpUnit.text.toString())
      Assert.assertEquals("45", txtViewBgValue.text.toString())
      Assert.assertEquals("bgunit", txtViewBgUnit.text.toString())
      Assert.assertEquals("23", txtViewBpValue.text.toString())
      Assert.assertEquals("bpunit", txtViewBpUnit.text.toString())
      Assert.assertEquals("23", txtViewPulseValue.text.toString())
      Assert.assertEquals("pulseunit", txtViewPulseUnit.text.toString())
    }

    ReflectionHelpers.callInstanceMethod<Any>(
      vitalSignsDetailsFragment,
      "handleVitalSigns",
      ReflectionHelpers.ClassParameter(PatientVitalItem::class.java, PatientVitalItem())
    )

    with(vitalSignsDetailsFragment.binding) {
      Assert.assertEquals("-", txtViewWeightValue.text.toString())
      Assert.assertEquals("", txtViewWeightUnit.text.toString())
      Assert.assertEquals("-", txtViewHeightValue.text.toString())
      Assert.assertEquals("", txtViewHeightUnit.text.toString())
      Assert.assertEquals("-", txtViewSpValue.text.toString())
      Assert.assertEquals("", txtViewSpUnit.text.toString())
      Assert.assertEquals("-", txtViewBgValue.text.toString())
      Assert.assertEquals("", txtViewBgUnit.text.toString())
      Assert.assertEquals("-", txtViewBpValue.text.toString())
      Assert.assertEquals("", txtViewBpUnit.text.toString())
      Assert.assertEquals("-", txtViewPulseValue.text.toString())
      Assert.assertEquals("", txtViewPulseUnit.text.toString())
    }
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
