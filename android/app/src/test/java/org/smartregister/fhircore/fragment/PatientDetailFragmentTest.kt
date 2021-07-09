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

package org.smartregister.fhircore.fragment

import android.content.Intent
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.spyk
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
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.activity.QuestionnaireActivity
import org.smartregister.fhircore.activity.QuestionnaireActivityTest
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.SharedPreferencesHelper

@Config(shadows = [FhirApplicationShadow::class])
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
    patientDetailFragment.viewModel = spyk(patientDetailFragment.viewModel)

    every { patientDetailFragment.viewModel.getPatientItem(any()) } returns
      MutableLiveData(PatientItem("", "", "", "2000-01-01", "", "", ""))

    patientDetailFragment.editPatient()

    val expectedIntent =
      Intent(patientDetailFragment.requireContext(), QuestionnaireActivity::class.java)
    val actual = Shadows.shadowOf(FhirApplication.getContext()).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actual.component)
  }

  private fun init() {
    SharedPreferencesHelper.init(FhirApplication.getContext())
    runBlocking {
      FhirApplication.fhirEngine(FhirApplication.getContext())
        .save(QuestionnaireActivityTest.TEST_PATIENT_1, getImmunization())
    }
  }

  private fun getImmunization() =
    Immunization().apply {
      id = "Patient/${QuestionnaireActivityTest.TEST_PATIENT_1_ID}"
      recorded = Date()
      vaccineCode =
        CodeableConcept().apply {
          this.text = "dummy"
          this.coding = listOf(Coding("", "dummy", "dummy"))
        }
      occurrence = DateTimeType.today()

      protocolApplied =
        listOf(
          Immunization.ImmunizationProtocolAppliedComponent().apply {
            doseNumber = PositiveIntType(1)
          }
        )
    }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }
}
