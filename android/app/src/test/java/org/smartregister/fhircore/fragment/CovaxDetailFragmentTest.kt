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
import android.widget.TextView
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
import io.mockk.mockkObject
import io.mockk.spyk
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.CovaxDetailActivity
import org.smartregister.fhircore.model.CovaxDetailView
import org.smartregister.fhircore.model.PatientDetailsCard
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.shadow.TestUtils
import org.smartregister.fhircore.viewmodel.CovaxListViewModel

@Config(shadows = [FhirApplicationShadow::class])
class CovaxDetailFragmentTest : FragmentRobolectricTest() {

  private lateinit var viewModel: CovaxListViewModel
  private lateinit var fhirEngine: FhirEngine
  private lateinit var context: Context
  private lateinit var covaxDetailActivity: CovaxDetailActivity
  private lateinit var covaxDetailFragment: CovaxDetailFragment
  private lateinit var fragmentScenario: FragmentScenario<CovaxDetailFragment>

  private val patient = TestUtils.TEST_PATIENT_1

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    fhirEngine = spyk()
    coEvery { fhirEngine.load(Patient::class.java, patient.id) } returns patient
    coEvery { fhirEngine.search<Patient>(any()) } returns listOf()

    mockkObject(FhirApplication)
    every { FhirApplication.fhirEngine(any()) } returns fhirEngine

    val bundle = bundleOf(CovaxDetailView.COVAX_ARG_ITEM_ID to patient.id)
    val intent = Intent().putExtras(bundle)

    covaxDetailActivity =
      Robolectric.buildActivity(CovaxDetailActivity::class.java, intent).create().get()

    viewModel = spyk(covaxDetailActivity.viewModel)

    fragmentScenario =
      launchFragmentInContainer(
        bundle,
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
  fun testFragmentShouldLoadProfile() {
    assertEquals(View.INVISIBLE, getView<TextView>(R.id.risk_flag).visibility)
    assertEquals("ID: " + patient.id, getView<TextView>(R.id.id_patient_number).text)
  }

  @Test
  fun testFragmentShouldHaveRecordVaccineEnabledWithDueDose() {
    coEvery { viewModel.fetchPatientDetailsCards(any(), any()) } returns
      MutableLiveData(
        listOf(buildPatientDetailsCard("registration"), buildPatientDetailsCard("vaccine dose 1"))
      )

    covaxDetailFragment.loadProfile()

    val btnRecordVaccine = getView<View>(R.id.btn_record_vaccine)
    assertEquals(View.VISIBLE, btnRecordVaccine.visibility)
  }

  @Test
  fun testFragmentShouldHaveRecordVaccineHiddenWith2Doses() {
    coEvery { viewModel.fetchPatientDetailsCards(any(), any()) } returns
      MutableLiveData(
        listOf(
          buildPatientDetailsCard("registration"),
          buildPatientDetailsCard("vaccine dose 1"),
          buildPatientDetailsCard("vaccine dose 2")
        )
      )

    covaxDetailFragment.loadProfile()

    val btnRecordVaccine = getView<View>(R.id.btn_record_vaccine)
    assertEquals(View.INVISIBLE, btnRecordVaccine.visibility)
  }

  private fun buildPatientDetailsCard(id: String): PatientDetailsCard {
    return PatientDetailsCard(1, 1, id, "", "", "")
  }

  override fun getFragment(): Fragment {
    return covaxDetailFragment
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }
}
