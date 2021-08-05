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

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.CovaxDetailActivity
import org.smartregister.fhircore.activity.CovaxListActivity
import org.smartregister.fhircore.activity.QuestionnaireActivityTest
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.model.CovaxDetailView
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.SharedPreferencesHelper

@Config(shadows = [FhirApplicationShadow::class])
class CovaxDetailFragmentTest : FragmentRobolectricTest() {

  private lateinit var fhirEngine: FhirEngine
  private lateinit var context: Context
  private lateinit var covaxDetailActivity: CovaxDetailActivity
  private lateinit var covaxDetailFragment: CovaxDetailFragment
  private lateinit var fragmentScenario: FragmentScenario<CovaxDetailFragment>

  private val PATIENT_ID = "123456"

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    init()

    val bundle =
      bundleOf(CovaxDetailView.COVAX_ARG_ITEM_ID to PATIENT_ID)
    val intent = Intent().putExtras(bundle)

    fhirEngine = spyk()
    coEvery { fhirEngine.load(Patient::class.java, PATIENT_ID) } returns Patient()

    covaxDetailActivity = Robolectric.buildActivity(CovaxDetailActivity::class.java, intent).create().get()

    val viewModel = spyk(covaxDetailFragment.viewModel)

    every { viewModel.getPatientItem(any()) } returns
            MutableLiveData(
              PatientItem("", "", "", "2000-01-01", "", "", "", "HR", lastSeen = "07-26-2021")
            )

    fragmentScenario =
      launchFragmentInContainer(bundle,
        factory =
        object : FragmentFactory() {
          override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            val fragment = spyk(CovaxDetailFragment())
            every { fragment.activity } returns covaxDetailActivity
            every { fragment.viewModel } returns viewModel
            return fragment
          }
        }
      )

    fragmentScenario.onFragment { covaxDetailFragment = it }
  }

  @Test
  fun testEditPatientShouldStartQuestionnaireActivity() {
    val viewModel = spyk(covaxDetailFragment.viewModel)

    every { viewModel.getPatientItem(any()) } returns
      MutableLiveData(
        PatientItem("", "", "", "2000-01-01", "", "", "", "HR", lastSeen = "07-26-2021")
      )

    //covaxDetailFragment.editPatient() //todo ????????????????????????????

    val expectedIntent =
      Intent(covaxDetailFragment.requireContext(), QuestionnaireActivity::class.java)
    val actual = Shadows.shadowOf(FhirApplication.getContext()).nextStartedActivity
    assertEquals(expectedIntent.component, actual.component)
  }

  @Test
  fun testFragmentShouldHaveRecordVaccineEnabled() {
    val btnRecordVaccine = getView<View>(R.id.btn_record_vaccine)
    assertEquals(View.VISIBLE, btnRecordVaccine.visibility)
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

  override fun getFragment(): Fragment {
    return covaxDetailFragment
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }
}
