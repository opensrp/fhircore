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

package org.smartregister.fhircore.eir.data

import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import io.mockk.coEvery
import io.mockk.mockk
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.eir.data.model.VaccineStatus
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.toHumanDisplay

class PatientRepositoryTest : RobolectricTest() {

  private lateinit var patientRepository: PatientRepository
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = mockk()

    coEvery {
      hint(Resource::class)
      fhirEngine.search<Resource>(any())
    } answers
      {
        when (firstArg<Search>().type) {
          ResourceType.Patient -> listOf(getPatient())
          ResourceType.Immunization -> listOf(getImmunization())
          else -> listOf()
        }
      }

    patientRepository =
      PatientRepository(
        fhirEngine,
        PatientItemMapper(ApplicationProvider.getApplicationContext()),
        DefaultDispatcherProvider()
      )
  }

  @Test
  fun testLoadDataShouldReturnListOfPatients() {

    runBlocking {
      val patientItems = patientRepository.loadData("", 1, false)

      Assert.assertEquals(1, patientItems.size)
      with(patientItems.first()) {
        Assert.assertEquals("test_patient_1_id", patientIdentifier)
        Assert.assertEquals("Jane Mc", name)
        Assert.assertEquals("M", gender)
        Assert.assertEquals("0d", age)
        Assert.assertEquals("Jane Mc, M, 0d", demographics)
        Assert.assertEquals(SimpleDateFormat("dd-MMM-yyyy").format(Date()), lastSeen)
        Assert.assertEquals(VaccineStatus.PARTIAL, vaccineStatus.status)
        Assert.assertEquals(SimpleDateFormat("dd-MMM-yyyy").format(Date()), vaccineStatus.date)
        Assert.assertEquals("", atRisk)
      }
    }
  }

  @Test
  fun testCountAllShouldReturnAsExpected() {
    coEvery { fhirEngine.count(any()) } returns 1
    runBlocking { Assert.assertEquals(1, patientRepository.countAll()) }
  }

  @Test
  fun testFetchDemographicsShouldReturnPatientDetail() {
    coEvery { fhirEngine.get(ResourceType.Patient, "test_patient_1_id") } returns getPatient()
    val patient = runBlocking { patientRepository.fetchDemographics("test_patient_1_id") }

    Assert.assertEquals("jane", patient.nameFirstRep.givenAsSingleString)
    Assert.assertEquals("Mc", patient.nameFirstRep.family)
    Assert.assertEquals("12345678", patient.telecomFirstRep.value)
    Assert.assertEquals("Nairobi", patient.addressFirstRep.city)
    Assert.assertEquals("Kenya", patient.addressFirstRep.country)
  }

  @Test
  fun testGetAdverseEventsShouldReturnExpectedItem() {

    coEvery { fhirEngine.get(ResourceType.Observation, "1") } returns
      Observation().apply {
        code = CodeableConcept().apply { addCoding().apply { display = "Obs" } }
      }

    val mDate = Date()
    val immunization =
      Immunization().apply {
        addReaction().apply {
          detail =
            Reference().apply {
              date = mDate
              reference = "Observation/1"
            }
        }
      }

    val adverseEventItem = runBlocking { patientRepository.getAdverseEvents(immunization) }

    Assert.assertEquals(1, adverseEventItem.size)
    Assert.assertEquals(mDate.toHumanDisplay(), adverseEventItem[0].date)
    Assert.assertEquals("Obs", adverseEventItem[0].detail)
  }

  private fun getPatient(): Patient {
    return Patient().apply {
      id = "test_patient_1_id"
      gender = Enumerations.AdministrativeGender.MALE
      name =
        mutableListOf(
          HumanName().apply {
            addGiven("jane")
            family = "Mc"
          }
        )
      telecom = mutableListOf(ContactPoint().apply { value = "12345678" })
      address =
        mutableListOf(
          Address().apply {
            city = "Nairobi"
            country = "Kenya"
          }
        )
      active = true
      birthDate = Date()
    }
  }

  private fun getImmunization(): Immunization {
    return Immunization().apply {
      recorded = Date()
      vaccineCode =
        CodeableConcept().apply {
          this.text = "vaccineA"
          this.coding = listOf(Coding("", "vaccineA", "vaccineA"))
        }
      occurrence = DateTimeType.today()

      protocolApplied =
        listOf(
          Immunization.ImmunizationProtocolAppliedComponent().apply {
            doseNumber = PositiveIntType(1)
          }
        )
    }
  }
}
