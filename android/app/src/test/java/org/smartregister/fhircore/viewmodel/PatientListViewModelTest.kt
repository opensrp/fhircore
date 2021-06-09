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

package org.smartregister.fhircore.viewmodel

import com.google.android.fhir.FhirEngine
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Immunization
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.fragment.PatientListFragment

class PatientListViewModelTest : RobolectricTest() {

  private lateinit var viewModel: PatientListViewModel

  @RelaxedMockK lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    val app = FhirApplication.getContext()
    viewModel = PatientListViewModel(app, fhirEngine)
  }

  @Test
  fun testFetchPatientStatus() {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DATE, PatientListFragment.SECOND_DOSE_OVERDUE_DAYS)
    val overdueDate = Date(cal.timeInMillis)

    val immunizations = mutableListOf<Immunization>()

    coEvery { fhirEngine.search<Immunization>(any()) } returns immunizations

    verifyStatus(PatientListViewModel.VaccineStatus.DUE)

    immunizations.add(Immunization().apply { recorded = Date() })
    verifyStatus(PatientListViewModel.VaccineStatus.PARTIAL)

    immunizations[0].recorded = overdueDate
    verifyStatus(PatientListViewModel.VaccineStatus.OVERDUE)

    immunizations.add(Immunization())
    verifyStatus(PatientListViewModel.VaccineStatus.VACCINATED)
  }

  private fun verifyStatus(vaccineStatus: PatientListViewModel.VaccineStatus) {

    val status = runBlocking { viewModel.fetchPatientStatus("").value?.status }

    Assert.assertEquals(vaccineStatus, status)
  }
}
