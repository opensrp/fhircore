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

package org.smartregister.fhircore.eir.ui.patient.details

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.StringType
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.eir.R
//import org.smartregister.fhircore.eir.fake.Faker
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
/*
@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class PatientDetailsFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val activityScenarioRule = ActivityScenarioRule(HiltActivityForTest::class.java)

  @get:Rule(order = 2) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("covax", mockk())
  private lateinit var patientDetailsFragment: PatientDetailsFragment

  private val patientId = "samplePatientId"

  @Before
  fun setUp() {
    hiltRule.inject()

    patientDetailsFragment =
      PatientDetailsFragment.newInstance(
        bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId))
      )
    activityScenarioRule.scenario.onActivity {
      it.supportFragmentManager.commitNow {
        add(patientDetailsFragment, PatientDetailsFragment.TAG)
      }
    }
  }

  @After
  fun tearDown() {
    activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)
  }

  @Test
  fun testThatViewsAreSetupCorrectly() {
    Assert.assertNotNull(patientDetailsFragment.view)
    val recordVaccinesButton =
      patientDetailsFragment.view?.findViewById<Button>(R.id.recordVaccineButton)
    Assert.assertEquals(View.GONE, recordVaccinesButton?.visibility)

    val reportAdverseEventButton =
      patientDetailsFragment.view?.findViewById<Button>(R.id.reportAdverseEventButton)
    Assert.assertEquals(View.GONE, reportAdverseEventButton?.visibility)

    val showQRCodeButton = patientDetailsFragment.view?.findViewById<Button>(R.id.showQRCodeButton)
    Assert.assertEquals(View.GONE, showQRCodeButton?.visibility)

    val immuneStatusImageView =
      patientDetailsFragment.view?.findViewById<ImageView>(R.id.immuneStatusImageView)
    Assert.assertEquals(View.VISIBLE, immuneStatusImageView?.visibility)
    Assert.assertNull(immuneStatusImageView?.background)

    val immuneTextView = patientDetailsFragment.view?.findViewById<TextView>(R.id.immuneTextView)
    Assert.assertEquals("", immuneTextView?.text.toString())

    // No vaccine received text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.noVaccinesTextView)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("No vaccine received", noVaccinesTextView?.text.toString())

    // Vaccines list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.immunizationsListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }

  @Test
  fun testThatDemographicViewsAreUpdated() {
    val patient =
      spyk<Patient>().apply {
        idElement.id = patientId
        gender = Enumerations.AdministrativeGender.MALE
        name =
          listOf(HumanName().setFamily("Mandela").setGiven(mutableListOf(StringType("Nelson"))))
        birthDate = Date()
      }

    patientDetailsFragment.patientDetailsViewModel.patientDemographics.value = patient

    val patientNameTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.patientNameTextView)
    Assert.assertEquals("Nelson Mandela", patientNameTextView?.text.toString())

    val patientGenderTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.patientGenderTextView)
    Assert.assertEquals("Male", patientGenderTextView?.text.toString())

    val patientAgeTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.patientAgeTextView)
    Assert.assertEquals("Age 0d", patientAgeTextView?.text.toString())
  }

  @Test
  fun testImmunizationStatusWithCompleteImmunizationList() {
    patientDetailsFragment.patientDetailsViewModel.patientImmunizations.value = getImmunizations()
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.noVaccinesTextView)
    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)

    // 3 immunizations administered (vaccines are grouped so there can be more than 1 vaccine per
    // category)
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.immunizationsListView)
    Assert.assertEquals(View.VISIBLE, immunizationsListView?.visibility)
    Assert.assertNotNull(immunizationsListView?.adapter)
    Assert.assertEquals(2, immunizationsListView?.adapter?.itemCount)

    val recordVaccineButton =
      patientDetailsFragment.view?.findViewById<Button>(R.id.recordVaccineButton)
    Assert.assertEquals(View.GONE, recordVaccineButton?.visibility)

    val reportAdverseEventButton =
      patientDetailsFragment.view?.findViewById<Button>(R.id.reportAdverseEventButton)
    Assert.assertEquals(View.GONE, reportAdverseEventButton?.visibility)

    val showQRCodeButton = patientDetailsFragment.view?.findViewById<Button>(R.id.showQRCodeButton)
    Assert.assertEquals(View.VISIBLE, showQRCodeButton?.visibility)

    val immuneTextView = patientDetailsFragment.view?.findViewById<TextView>(R.id.immuneTextView)
    Assert.assertEquals("Immune", immuneTextView?.text.toString())
  }

  @Test
  fun testImmunizationStatusWithEmptyImmunizationList() {

    patientDetailsFragment.patientDetailsViewModel.patientImmunizations.value = emptyList()
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.noVaccinesTextView)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)

    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.immunizationsListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)

    val recordVaccineButton =
      patientDetailsFragment.view?.findViewById<Button>(R.id.recordVaccineButton)
    Assert.assertEquals(View.VISIBLE, recordVaccineButton?.visibility)

    val reportAdverseEventButton =
      patientDetailsFragment.view?.findViewById<Button>(R.id.reportAdverseEventButton)
    Assert.assertEquals(View.GONE, reportAdverseEventButton?.visibility)

    val showQRCodeButton = patientDetailsFragment.view?.findViewById<Button>(R.id.showQRCodeButton)
    Assert.assertEquals(View.GONE, showQRCodeButton?.visibility)

    val immuneTextView = patientDetailsFragment.view?.findViewById<TextView>(R.id.immuneTextView)
    Assert.assertEquals("Not immune", immuneTextView?.text.toString())
  }

  @Test
  fun testImmunizationStatusWithPartialImmunizationList() {
    val immunizations = getImmunizations()
    patientDetailsFragment.patientDetailsViewModel.patientImmunizations.value =
      mutableListOf(immunizations.last())

    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.noVaccinesTextView)
    Assert.assertEquals(View.GONE, noVaccinesTextView?.visibility)

    // Only 1 immunization administered
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.immunizationsListView)
    Assert.assertEquals(View.VISIBLE, immunizationsListView?.visibility)
    Assert.assertNotNull(immunizationsListView?.adapter)
    Assert.assertEquals(1, immunizationsListView?.adapter?.itemCount)

    val recordVaccineButton =
      patientDetailsFragment.view?.findViewById<Button>(R.id.recordVaccineButton)
    Assert.assertEquals(View.VISIBLE, recordVaccineButton?.visibility)

    val reportAdverseEventButton =
      patientDetailsFragment.view?.findViewById<Button>(R.id.reportAdverseEventButton)
    Assert.assertEquals(View.GONE, reportAdverseEventButton?.visibility)

    val showQRCodeButton = patientDetailsFragment.view?.findViewById<Button>(R.id.showQRCodeButton)
    Assert.assertEquals(View.GONE, showQRCodeButton?.visibility)

    val immuneTextView = patientDetailsFragment.view?.findViewById<TextView>(R.id.immuneTextView)
    Assert.assertEquals("Not immune", immuneTextView?.text.toString())
  }

  private fun getImmunizations(): List<Immunization> {
    val immunization1 =
      spyk<Immunization>().apply {
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code", "code display")).setText("Astrazeneca")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(1)))
        occurrence = DateTimeType.now().setDay(16).setMonth(8).setYear(2021)
      }

    val immunization2 =
      spyk<Immunization>().apply {
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code", "code display")).setText("Astrazeneca")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(2)))
        occurrence = DateTimeType.now().setDay(16).setMonth(8).setYear(2021)
      }

    val immunization3 =
      spyk<Immunization>().apply {
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code2", "code display")).setText("Pfizer")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(2)))
        occurrence = DateTimeType.now().setDay(16).setMonth(8).setYear(2021)
      }
    return listOf(immunization1, immunization2, immunization3)
  }
}*/
