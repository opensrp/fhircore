/*
 * Copyright 2021 Ona Systems Inc
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

package org.smartregister.fhircore.robolectric.fragment

import android.content.Intent
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.QuestionnaireActivity
import org.smartregister.fhircore.fragment.PatientDetailFragment
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.robolectric.activity.QuestionnaireActivityTest
import org.smartregister.fhircore.util.SharedPrefrencesHelper

class PatientDetailFragmentTest : FragmentRobolectricTest() {

  private lateinit var patientDetailFragment: PatientDetailFragment
  private lateinit var fragmentScenario: FragmentScenario<PatientDetailFragment>

  @Before
  fun setUp() {

    init()

    val bundle =
      bundleOf(PatientDetailFragment.ARG_ITEM_ID to QuestionnaireActivityTest.TEST_PATIENT_1_ID)
    fragmentScenario = FragmentScenario.launchInContainer(PatientDetailFragment::class.java, bundle)
    fragmentScenario.onFragment { patientDetailFragment = it }
  }

  @Test
  fun testEditPatientShouldStartQuestionnaireActivity() {
    patientDetailFragment.editPatient()

    val expectedIntent =
      Intent(patientDetailFragment.requireContext(), QuestionnaireActivity::class.java)
    val actual = Shadows.shadowOf(FhirApplication.getContext()).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actual.component)
  }

  @Test
  fun testVaccineOverdueStatusShouldRed() {

    val cal = Calendar.getInstance()
    cal.add(Calendar.DATE, PatientListFragment.SECOND_DOSE_OVERDUE_DAYS)
    val date = Date(cal.timeInMillis)

    val immunication =
      listOf(
        Immunization().apply {
          protocolApplied =
            listOf(
              Immunization.ImmunizationProtocolAppliedComponent().apply {
                doseNumber = PositiveIntType(1)
              }
            )
          vaccineCode =
            CodeableConcept().apply { coding = listOf(Coding().apply { code = "Pfizer" }) }
          recorded = date
          status = Immunization.ImmunizationStatus.COMPLETED
          occurrence = DateTimeType.today().setValue(date)
        }
      )
    patientDetailFragment.viewModel.liveSearchImmunization.value = immunication

    val tvVaccineSecondDose =
      patientDetailFragment.rootView.findViewById<TextView>(R.id.vaccination_second_dose)
    Assert.assertEquals(
      "Dose 2 due ${SimpleDateFormat("M-d-yyyy").format(Date())}",
      tvVaccineSecondDose.text.toString()
    )
    Assert.assertEquals(
      ContextCompat.getColor(patientDetailFragment.requireContext(), R.color.status_red),
      tvVaccineSecondDose.currentTextColor
    )
  }

  private fun init() {
    SharedPrefrencesHelper.init(FhirApplication.getContext())
    runBlocking {
      FhirApplication.fhirEngine(FhirApplication.getContext())
        .save(QuestionnaireActivityTest.TEST_PATIENT_1)
    }
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }
}
