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

package org.smartregister.fhircore.anc.data.madx

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumeration
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.madx.model.AncPatientItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.madx.details.CarePlanItemMapper
import org.smartregister.fhircore.anc.ui.madx.details.UpcomingServiceItemMapper
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable
import org.smartregister.fhircore.engine.util.extension.plusWeeksAsString

class NonAncPatientRepositoryTest : RobolectricTest() {
  private lateinit var repository: NonAncPatientRepository
  private lateinit var fhirEngine: FhirEngine

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    fhirEngine = spyk()
    repository =
      spyk(NonAncPatientRepository(fhirEngine, CarePlanItemMapper, UpcomingServiceItemMapper))
  }

  @Test
  fun testFetchDemographicsShouldReturnMergedPatient() {

    coEvery { fhirEngine.load(any<Class<Resource>>(), any()) } answers
      {
        when (secondArg<String>()) {
          PATIENT_ID_1 -> getPatient()
          PATIENT_ID_2 -> getHeadPatient()
          else -> Patient()
        }
      }

    val demographics = runBlocking { repository.fetchDemographics(PATIENT_ID_1) }

    verifyPatient(demographics.patientDetails)
    verifyHeadPatient(demographics.patientDetailsHead)
  }

  @Test
  fun fetchCarePlanItemTest() {
    val patientId = "1111"
    val carePlan = listOf(buildCarePlanWithActive(patientId))
    val listCarePlan = repository.fetchCarePlanItem(carePlan = carePlan, patientId = patientId)
    if (listCarePlan.isNotEmpty()) {
      Assert.assertEquals("ABC", listCarePlan[0].title)
    }
  }

  @Test
  fun fetchConditionItemTest() {
    val patientId = "1111"
    val conditionList = listOf(buildCondition(patientId))
    val listConditionList = repository.fetchConditionItem(conditionList)
    Assert.assertEquals(listConditionList.isEmpty(), true)
  }

  @Test
  fun fetchAllergiesItemTest() {
    val patientId = "1111"
    val conditionList = listOf(buildCondition(patientId))
    val listConditionList = repository.fetchAllergiesItem(conditionList)
    Assert.assertEquals(listConditionList.isEmpty(), true)
  }

  @Test
  fun fetchEncounterItemTest() {
    val patientId = "1111"
    val listConditionList = repository.fetchEncounterItem(arrayListOf())
    Assert.assertEquals(listConditionList.isEmpty(), true)
  }

  @Test
  fun fetchUpcomingServiceItemTest() {
    val patientId = "1111"
    val carePlan = listOf(buildCarePlanWithActive(patientId))
    val listUpcomingServiceItem = repository.fetchUpcomingServiceItem(carePlan = carePlan)
    Assert.assertEquals("ABC", listUpcomingServiceItem[0].title)
    Assert.assertEquals(Date().makeItReadable(), listUpcomingServiceItem[0].date)
  }

  private fun buildCarePlanWithActive(subject: String): CarePlan {
    val date = DateType(Date())
    val end = date.plusWeeksAsString(4).getDate("yyyy-MM-dd")
    return CarePlan().apply {
      this.id = "11190"
      this.status = CarePlan.CarePlanStatus.ACTIVE
      this.period.start = date.value
      this.period.end = end
      this.subject = Reference().apply { reference = "Patient/$subject" }
      this.addActivity().detail.apply {
        this.description = "ABC"
        this.scheduledPeriod.start = Date()
        this.status = CarePlan.CarePlanActivityStatus.SCHEDULED
      }
    }
  }

  private fun buildCondition(subject: String): Condition {
    return Condition().apply {
      this.id = id
      this.code = CodeableConcept().apply { addCoding().apply { code = "123456" } }
      this.subject = Reference().apply { reference = "Patient/$subject" }
    }
  }

  private fun getHeadPatient(): Patient {
    return Patient().apply {
      id = PATIENT_ID_2
      gender = Enumerations.AdministrativeGender.FEMALE
      name =
        mutableListOf(
          HumanName().apply {
            addGiven("salina")
            family = "jetly"
          }
        )
      telecom = mutableListOf(ContactPoint().apply { value = "87654321" })
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

  private fun getPatient(): Patient {
    return Patient().apply {
      id = PATIENT_ID_1
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
      link =
        listOf(
          Patient.PatientLinkComponent(
            Reference(PATIENT_ID_2),
            Enumeration(Patient.LinkTypeEnumFactory())
          )
        )
    }
  }

  private fun verifyPatient(patient: AncPatientItem) {
    with(patient) {
      Assert.assertEquals(PATIENT_ID_1, patientIdentifier)
      Assert.assertEquals("Jane Mc", name)
      Assert.assertEquals("Male", gender)
      Assert.assertEquals("0", age)
      Assert.assertEquals("", demographics)
      Assert.assertEquals("", atRisk)
    }
  }

  private fun verifyHeadPatient(patient: AncPatientItem) {
    with(patient) {
      Assert.assertEquals(PATIENT_ID_1, patientIdentifier)
      Assert.assertEquals("Salina Jetly", name)
      Assert.assertEquals("Female", gender)
      Assert.assertEquals("0", age)
      Assert.assertEquals("Kenya", demographics)
      Assert.assertEquals("", atRisk)
    }
  }

  companion object {
    private const val PATIENT_ID_1 = "test_patient_id_1"
    private const val PATIENT_ID_2 = "test_patient_id_2"
  }
}
