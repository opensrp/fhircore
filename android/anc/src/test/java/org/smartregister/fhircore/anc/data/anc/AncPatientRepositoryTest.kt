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

package org.smartregister.fhircore.anc.data.anc

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumeration
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Goal
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import org.smartregister.fhircore.anc.data.anc.model.AncPatientItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.details.AncPatientItemMapper

class AncPatientRepositoryTest : RobolectricTest() {

  private lateinit var patientRepository: AncPatientRepository
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = mockk()

    coEvery { fhirEngine.save(any()) } returns Unit
    coEvery {
      hint(Resource::class)
      fhirEngine.search<Resource>(any())
    } answers
      {
        when (firstArg<Search>().type) {
          ResourceType.Patient -> listOf(getPatient())
          else -> listOf()
        }
      }

    coEvery { fhirEngine.load(any<Class<Resource>>(), any()) } answers
      {
        when (secondArg<String>()) {
          PATIENT_ID_1 -> getPatient()
          PATIENT_ID_2 -> getHeadPatient()
          else -> Patient()
        }
      }

    patientRepository = AncPatientRepository(fhirEngine, AncPatientItemMapper)
  }

  @Test
  fun testLoadDataShouldReturnListOfPatients() {

    runBlocking {
      val patientItems = patientRepository.loadData("", 1, false)

      Assert.assertEquals(1, patientItems.size)
      with(patientItems.first()) {
        Assert.assertEquals(PATIENT_ID_1, patientIdentifier)
        Assert.assertEquals("Jane Mc", name)
        Assert.assertEquals("M", gender)
        Assert.assertEquals("0", age)
        Assert.assertEquals("Jane Mc, M, 0", demographics)
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
  fun testFetchDemographicsShouldReturnMergedPatient() {
    val demographics = runBlocking { patientRepository.fetchDemographics(PATIENT_ID_1) }

    verifyPatient(demographics.patientDetails)
    verifyHeadPatient(demographics.patientDetailsHead)
  }

  @Test
  fun testFetchCarePlanShouldReturnExpectedCarePlan() {
    mockkStatic(FhirContext::class)

    val fhirContext = mockk<FhirContext>()
    val parser = mockk<IParser>()

    val cpTitle = "First Care Plan"
    val cpPeriodStartDate = SimpleDateFormat("yyyy-MM-dd").parse("2021-01-01")

    every { FhirContext.forR4() } returns fhirContext
    every { fhirContext.newJsonParser() } returns parser
    every { parser.parseResource(any<String>()) } returns
      CarePlan().apply {
        title = cpTitle
        period = Period().apply { start = cpPeriodStartDate }
      }

    val carePlans = runBlocking { patientRepository.fetchCarePlan(PATIENT_ID_1, "") }

    Assert.assertEquals(1, carePlans.size)
    with(carePlans.first()) {
      Assert.assertEquals(cpTitle, title)
      Assert.assertEquals(cpPeriodStartDate?.time, periodStartDate.time)
    }
  }

  @Test
  fun testEnrollIntoAncShouldVerifyEngineSaveCall() {

    runBlocking { patientRepository.enrollIntoAnc("", DateTimeType.now()) }
    coVerify(exactly = 5) { fhirEngine.save(any()) }
  }

  @Test
  fun testLoadConfigShouldReturnPregnancyCondition() {
    val config =
      ReflectionHelpers.callInstanceMethod<Map<String, String?>>(
        patientRepository,
        "buildConfigData",
        ClassParameter(String::class.java, PATIENT_ID_1),
        ClassParameter(Condition::class.java, Condition().apply { id = "condition_id" }),
        ClassParameter(EpisodeOfCare::class.java, EpisodeOfCare().apply { id = "episode_id" }),
        ClassParameter(Encounter::class.java, Encounter().apply { id = "encounter_id" }),
        ClassParameter(Goal::class.java, Goal().apply { id = "goal_id" }),
        ClassParameter(DateTimeType::class.java, DateTimeType.parseV3("2021-01-01"))
      )

    val expected =
      mapOf(
        "#Id" to config["#Id"],
        "#RefPatient" to "Patient/test_patient_id_1",
        "#RefCondition" to "condition_id",
        "#RefEpisodeOfCare" to "episode_id",
        "#RefEncounter" to "encounter_id",
        "#RefGoal" to "Goal/goal_id",
        "#RefCareTeam" to "CareTeam/325",
        "#RefPractitioner" to "Practitioner/399",
        "#RefDateOnset" to "2020-12-31",
        "#RefDateStart" to "2020-12-31",
        "#RefDateEnd" to "2021-09-30",
        "#RefDate20w" to "2021-05-20",
        "#RefDate26w" to "2021-07-01",
        "#RefDate30w" to "2021-07-29",
        "#RefDate34w" to "2021-08-26",
        "#RefDate36w" to "2021-09-09",
        "#RefDate38w" to "2021-09-23",
        "#RefDate40w" to "2021-10-07",
        "#RefDateDeliveryStart" to "2021-10-07",
        "#RefDateDeliveryEnd" to "2021-10-21",
      )

    MatcherAssert.assertThat(config, `is`(expected))
    MatcherAssert.assertThat(config.size, `is`(expected.size))
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

  companion object {
    private const val PATIENT_ID_1 = "test_patient_id_1"
    private const val PATIENT_ID_2 = "test_patient_id_2"
  }
}
