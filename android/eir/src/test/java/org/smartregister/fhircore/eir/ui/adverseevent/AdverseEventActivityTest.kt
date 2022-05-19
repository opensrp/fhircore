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

package org.smartregister.fhircore.eir.ui.adverseevent

import android.app.Activity
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.Date
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
import org.junit.jupiter.api.DisplayName
import org.robolectric.Robolectric
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.data.model.PatientItem
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.details.ImmunizationAdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper

@HiltAndroidTest
internal class AdverseEventActivityTest : ActivityRobolectricTest() {

  private lateinit var fhirEngine: FhirEngine

  private lateinit var adverseEventActivity: AdverseEventActivity

  private lateinit var adverseEventAdapter: MainAdverseEventAdapter

  private lateinit var patientRepository: PatientRepository

  @get:Rule val hiltAndroidRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
    mockkObject(Sync)
    hiltAndroidRule.inject()

    adverseEventAdapter = mockk()

    fhirEngine = mockk(relaxed = true)

    patientRepository = mockk()

    val ancPatientDetailItem = spyk(PatientItemMapper(ApplicationProvider.getApplicationContext()))

    adverseEventActivity =
      Robolectric.buildActivity(AdverseEventActivity::class.java, null).create().get()

    every { adverseEventAdapter.submitList(any()) } returns Unit

    every {
      ancPatientDetailItem.transformInputToOutputModel(Pair(getPatient(), getImmunizations()))
    } returns PatientItem("samplePatientId", "Mandela Nelson", "M", "0")

    ReflectionHelpers.setField(adverseEventActivity, "adverseEventAdapter", adverseEventAdapter)
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  @DisplayName("Should start adverse event activity")
  fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(adverseEventActivity)
  }

  @Test
  fun testHandleImmunizationShouldVerifyExpectedCalls() {

    ReflectionHelpers.callInstanceMethod<Any>(
      adverseEventActivity,
      "handleImmunizations",
      ReflectionHelpers.ClassParameter(List::class.java, listOf<Immunization>())
    )

    // No CarePlan available text displayed
    val noVaccinesTextView =
      adverseEventActivity.findViewById<TextView>(R.id.noAdverseEventTextView)

    // CarePlan list is not displayed
    val immunizationsListView =
      adverseEventActivity.findViewById<RecyclerView>(R.id.adverseEventListView)

    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)

    ReflectionHelpers.callInstanceMethod<Any>(
      adverseEventActivity,
      "handleImmunizations",
      ReflectionHelpers.ClassParameter(List::class.java, getImmunizations())
    )

    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
    verify(exactly = 0) { adverseEventAdapter.submitList(any()) }
  }

  @Test
  fun testPopulateAdverseEventsListShouldVerifyAdapterSubmitCall() {

    ReflectionHelpers.callInstanceMethod<Any>(
      adverseEventActivity,
      "populateAdverseEventsList",
      ReflectionHelpers.ClassParameter(List::class.java, getImmunizations()),
      ReflectionHelpers.ClassParameter(
        List::class.java,
        listOf(
          Pair("1", getImmunizationAdverseEventItem()),
          Pair("2", getImmunizationAdverseEventItem()),
          Pair("3", getImmunizationAdverseEventItem())
        )
      )
    )

    verify(exactly = 1) { adverseEventAdapter.submitList(any()) }
  }

  override fun getActivity(): Activity {
    return adverseEventActivity
  }

  private fun getPatient(): Patient {
    val patient =
      spyk<Patient>().apply {
        id = "samplePatientId"
        gender = Enumerations.AdministrativeGender.MALE
        name =
          listOf(HumanName().setFamily("Mandela").setGiven(mutableListOf(StringType("Nelson"))))
        birthDate = Date()
      }
    return patient
  }

  private fun getImmunizations(): List<Immunization> {
    val patient = getPatient()
    val immunization1 =
      spyk<Immunization>().apply {
        id = "1"
        patientTarget = patient
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code", "code display")).setText("Astrazeneca")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(1)))
        occurrence = DateTimeType("2021-07-30")
      }

    val immunization2 =
      spyk<Immunization>().apply {
        id = "2"
        patientTarget = patient
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code", "code display")).setText("Astrazeneca")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(2)))
        occurrence = DateTimeType("2021-07-30")
      }

    val immunization3 =
      spyk<Immunization>().apply {
        id = "3"
        patientTarget = patient
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code", "code display")).setText("Pfizer")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(2)))
        occurrence = DateTimeType("2021-07-30")
      }
    return listOf(immunization1, immunization2, immunization3)
  }

  private fun getImmunizationAdverseEventItem(): List<ImmunizationAdverseEventItem> {
    val listOfImmunizationIds = arrayListOf("1")
    val listOfImmunizationAdverseEvent =
      arrayListOf(
        AdverseEventItem("22-Jan-2021", "Blood"),
        AdverseEventItem("22-Feb-2021", "Pressure")
      )
    val listDosses =
      arrayListOf<Pair<String, List<AdverseEventItem>>>(Pair("1", listOfImmunizationAdverseEvent))
    val immunization1 = ImmunizationAdverseEventItem(listOfImmunizationIds, "Moderna", listDosses)

    return listOf(immunization1)
  }
}
