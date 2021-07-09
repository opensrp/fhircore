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

package org.smartregister.fhircore.viewmodel

import com.google.android.fhir.FhirEngine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.model.VaccineStatus
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class PatientListViewModelTest : RobolectricTest() {

  private lateinit var viewModel: PatientListViewModel

  private lateinit var appContext: FhirApplication
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    appContext = FhirApplication.getContext()
    fhirEngine = FhirApplication.fhirEngine(appContext)
    viewModel = PatientListViewModel(appContext, fhirEngine)
  }

  @Test
  fun testSearchResultsPaginatedPatients() {
    viewModel.searchResults("jane")

    val patients = viewModel.liveSearchedPaginatedPatients.value?.first
    val pagination = viewModel.liveSearchedPaginatedPatients.value?.second

    Assert.assertEquals(0, patients?.size)
    Assert.assertEquals(-1, pagination?.totalItems)
    Assert.assertEquals(0, pagination?.currentPage)
    Assert.assertEquals(10, pagination?.pageSize)
  }

  @Test
  fun testFetchPatientStatusShouldVerifyAllStatus() {

    val formatter = SimpleDateFormat("dd-MM-yy", Locale.US)
    val immunizationList =
      listOf(
        Immunization().apply {
          id = "Patient/0"
          recorded = Calendar.getInstance().apply { add(Calendar.DATE, -28) }.time
        },
        Immunization().apply {
          id = "Patient/0"
          recorded = Date()
        }
      )

    // verify VACCINATED
    runBlocking { fhirEngine.save(immunizationList[0], immunizationList[1]) }
    verifyPatientStatus(VaccineStatus.VACCINATED, formatter.format(immunizationList[0].recorded))

    // verify OVERDUE
    runBlocking { fhirEngine.remove(Immunization::class.java, "Patient/0") }
    verifyPatientStatus(VaccineStatus.OVERDUE, formatter.format(immunizationList[0].recorded))

    // verify PARTIAL
    runBlocking { fhirEngine.update(immunizationList[0].apply { recorded = Date() }) }
    verifyPatientStatus(VaccineStatus.PARTIAL, formatter.format(immunizationList[0].recorded))

    // verify DUE
    runBlocking { fhirEngine.remove(Immunization::class.java, "Patient/0") }
    verifyPatientStatus(VaccineStatus.DUE, "")
  }

  @Test
  fun testIsPatientExistsShouldVerifyResultValues() {

    var result = viewModel.isPatientExists("0")

    Assert.assertTrue(result.value!!.isFailure)
    Assert.assertFalse(result.value!!.isSuccess)

    runBlocking { fhirEngine.save(Patient().apply { id = "0" }) }

    result = viewModel.isPatientExists("0")

    Assert.assertFalse(result.value!!.isFailure)
    Assert.assertTrue(result.value!!.isSuccess)

    runBlocking { fhirEngine.remove(Patient::class.java, "0") }
  }

  @Test
  fun testClearPatientListShouldReturnEmptyList() {
    viewModel.clearPatientList()

    val patients = viewModel.liveSearchedPaginatedPatients.value?.first
    val pagination = viewModel.liveSearchedPaginatedPatients.value?.second

    Assert.assertEquals(0, patients?.size)
    Assert.assertEquals(0, pagination?.totalItems)
    Assert.assertEquals(0, pagination?.currentPage)
    Assert.assertEquals(1, pagination?.pageSize)
  }

  private fun verifyPatientStatus(vaccineStatus: VaccineStatus, detail: String) {

    runBlocking {
      val status = viewModel.getPatientStatus("0")

      Assert.assertEquals(vaccineStatus, status.status)
      Assert.assertEquals(detail, status.details)
    }
  }
}
