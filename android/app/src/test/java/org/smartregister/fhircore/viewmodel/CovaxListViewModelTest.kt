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

import androidx.lifecycle.MutableLiveData
import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.google.android.fhir.FhirEngine
import io.mockk.every
import io.mockk.spyk
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.model.VaccineStatus
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.util.Utils.makeItReadable

@Config(shadows = [FhirApplicationShadow::class])
class CovaxListViewModelTest : RobolectricTest() {

  private lateinit var viewModel: CovaxListViewModel

  private lateinit var appContext: FhirApplication
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    appContext = FhirApplication.getContext()
    fhirEngine = FhirApplication.fhirEngine(appContext)
    viewModel = spyk(CovaxListViewModel(appContext, fhirEngine))
  }

  @Test
  fun testSearchResultsPaginatedPatients() {
    viewModel.showOverduePatientsOnly.value = true
    viewModel.searchResults("jane")

    every { viewModel.liveSearchedPaginatedPatients } answers
      {
        MutableLiveData<Pair<List<PatientItem>, Pagination>>(Pair(listOf(), Pagination(-1, 10, 0)))
      }

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

  @Test
  fun testFetchPatientDetailsCardsShouldVerifyAllCases() {

    val patientRes =
      Patient().apply {
        id = "0"
        meta = Meta().apply { lastUpdated = Date() }
      }

    runBlocking { fhirEngine.save(patientRes) }
    var cards = viewModel.fetchPatientDetailsCards(appContext, "0").value!!

    val patientCard = cards[0]

    Assert.assertEquals("0", patientCard.id)
    Assert.assertEquals("Patient", patientCard.type)
    Assert.assertEquals(
      "${appContext.getString(R.string.registered_date)} ${Date().makeItReadable()}",
      patientCard.title
    )

    val noVaccineCard = cards[1]
    Assert.assertEquals(appContext.getString(R.string.no_vaccine_received), noVaccineCard.title)

    val dateFirstDose = Calendar.getInstance().apply { add(Calendar.DATE, -14) }.time

    val firstImmunization = saveImmunization("Patient/0", dateFirstDose, "Moderna", 1)
    cards = viewModel.fetchPatientDetailsCards(appContext, "0").value!!

    val firstImmunizationCard = cards[1]

    Assert.assertEquals("Patient/0", firstImmunizationCard.id)
    Assert.assertEquals("Immunization", firstImmunizationCard.type)
    Assert.assertEquals(
      appContext.getString(R.string.immunization_brief_text, "Moderna", 1),
      firstImmunizationCard.title
    )
    Assert.assertEquals(
      appContext.getString(
        R.string.immunization_next_dose_text,
        2,
        Utils.addDays(firstImmunization.occurrenceDateTimeType.toHumanDisplay(), 28)
      ),
      firstImmunizationCard.details
    )

    saveImmunization("Patient/0", Date(), "Pfizer", 2)
    cards = viewModel.fetchPatientDetailsCards(appContext, "0").value!!

    val fullyVaccinatedCard = cards[2]

    Assert.assertEquals("Patient/0", fullyVaccinatedCard.id)
    Assert.assertEquals("Immunization", fullyVaccinatedCard.type)
    Assert.assertEquals(
      appContext.getString(R.string.immunization_brief_text, "Pfizer", 2),
      fullyVaccinatedCard.title
    )
    Assert.assertEquals(
      appContext.getString(R.string.fully_vaccinated),
      fullyVaccinatedCard.details
    )

    // remove all temp resources
    runBlocking {
      fhirEngine.remove(Patient::class.java, "0")
      fhirEngine.remove(Immunization::class.java, "Patient/0")
      fhirEngine.remove(Immunization::class.java, "Patient/0")
    }
  }

  private fun saveImmunization(
    resId: String,
    dateDose: Date,
    vaccineName: String,
    dose: Int
  ): Immunization {
    val immunization =
      Immunization().apply {
        this.id = resId
        this.recorded = dateDose
        this.occurrence = DateTimeType(dateDose, TemporalPrecisionEnum.DAY)

        this.vaccineCode =
          CodeableConcept().apply {
            this.text = vaccineName
            this.coding = listOf(Coding("", vaccineName, vaccineName))
          }

        protocolApplied =
          listOf(
            Immunization.ImmunizationProtocolAppliedComponent().apply {
              this.doseNumber = PositiveIntType(dose)
            }
          )
      }
    runBlocking { fhirEngine.save(immunization) }

    return immunization
  }

  private fun verifyPatientStatus(vaccineStatus: VaccineStatus, detail: String) {

    runBlocking {
      val status = viewModel.getPatientStatus("0")

      Assert.assertEquals(vaccineStatus, status.status)
      Assert.assertEquals(detail, status.details)
    }
  }
}
