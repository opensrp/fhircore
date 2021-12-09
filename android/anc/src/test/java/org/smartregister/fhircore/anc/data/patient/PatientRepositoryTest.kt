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

package org.smartregister.fhircore.anc.data.patient

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumeration
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Goal
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.app.fakes.FakeModel.buildCarePlan
import org.smartregister.fhircore.anc.app.fakes.FakeModel.buildPatient
import org.smartregister.fhircore.anc.app.fakes.FakeModel.getEncounter
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.plusWeeksAsString

@HiltAndroidTest
class PatientRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var repository: PatientRepository
  private val fhirEngine: FhirEngine = spyk()
  private val context = ApplicationProvider.getApplicationContext<Application>()
  private val ancItemMapper = spyk(AncItemMapper(context))

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Before
  fun setUp() {
    hiltRule.inject()
    repository =
      spyk(
        PatientRepository(
          context = ApplicationProvider.getApplicationContext(),
          fhirEngine = fhirEngine,
          domainMapper = ancItemMapper,
          dispatcherProvider = dispatcherProvider
        )
      )
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
    coEvery { fhirEngine.search<Condition>(any()) } returns emptyList()

    val demographics = runBlocking { repository.fetchDemographics(PATIENT_ID_1) }
    verifyPatient(demographics.patientDetails)
    verifyHeadPatient(demographics.patientDetailsHead)
  }

  @Test
  fun fetchCarePlanItemTest() {
    val patientId = "1111"
    val carePlan = listOf(buildCarePlanWithActive(patientId))
    val listCarePlan = repository.fetchCarePlanItem(carePlan = carePlan)
    if (listCarePlan.isNotEmpty()) {
      Assert.assertEquals("ABC", listCarePlan[0].title)
    }
  }

  @Test
  fun fetchUpcomingServiceItemTest() {
    val patientId = "1111"
    val carePlan = listOf(buildCarePlanWithActive(patientId))
    val task = getTask()
    coEvery { fhirEngine.search<Task>(any()) } returns listOf(task)
    val listUpcomingServiceItem = runBlocking {
      repository.fetchUpcomingServiceItem(carePlan = carePlan)
    }
    Assert.assertEquals("ABC", listUpcomingServiceItem[0].title)
    Assert.assertEquals(
      task.executionPeriod.start.makeItReadable(),
      listUpcomingServiceItem[0].date
    )
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
            addLine("12 B")
            addLine("Gulshan")
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

  private fun verifyPatient(patient: PatientItem) {
    with(patient) {
      Assert.assertEquals(PATIENT_ID_1, patientIdentifier)
      Assert.assertEquals("Jane Mc", name)
      Assert.assertEquals("Male", gender)
      Assert.assertEquals("0d", age)
      Assert.assertEquals("Nairobi Kenya", demographics)
      Assert.assertEquals("", atRisk)
    }
  }

  private fun verifyHeadPatient(patient: PatientItem) {
    with(patient) {
      Assert.assertEquals(PATIENT_ID_1, patientIdentifier)
      Assert.assertEquals("Salina Jetly", name)
      Assert.assertEquals("Female", gender)
      Assert.assertEquals("0d", age)
      Assert.assertEquals("12 B, Gulshan, Nairobi Kenya", demographics)
      Assert.assertEquals("", atRisk)
    }
  }

  @Test
  fun testLoadAllShouldReturnListOfFamilyItem() {
    coEvery { fhirEngine.search<Condition>(any()) } returns listOf(buildCondition("1111"))
    coEvery { repository.searchCarePlan(any()) } returns listOf(buildCarePlan("1111"))
    coEvery { fhirEngine.load(Patient::class.java, "1111") } returns
      buildPatient(id = "1111", family = "Test", given = "Abc")
    coEvery { fhirEngine.load(Patient::class.java, "1110") } returns
      buildPatient(id = "1110", family = "Test0", given = "Abc0")
    coEvery { fhirEngine.count(any()) } returns 10

    runBlocking {
      val ancList = repository.loadData("", 0, true)

      Assert.assertEquals("Abc Test", ancList[0].name)
      Assert.assertEquals("1111", ancList[0].patientIdentifier)
    }
  }

  @Test
  fun testEnrollIntoAncShouldSaveEntities() {
    coEvery { fhirEngine.save(any()) } just runs

    val condition = slot<Condition>()
    val episode = slot<EpisodeOfCare>()
    val encounter = slot<Encounter>()
    val goal = slot<Goal>()
    val carePlan = slot<CarePlan>()

    runBlocking {
      repository.enrollIntoAnc("1111", DateType(Date()))

      coVerifyOrder {
        fhirEngine.save(capture(condition))
        fhirEngine.save(capture(episode))
        fhirEngine.save(capture(encounter))
        fhirEngine.save(capture(goal))
        fhirEngine.save(capture(carePlan))
      }

      val subject = "Patient/1111"

      Assert.assertEquals(subject, condition.captured.subject.reference)
      Assert.assertEquals(subject, episode.captured.patient.reference)
      Assert.assertEquals(subject, encounter.captured.subject.reference)
      Assert.assertEquals(subject, goal.captured.subject.reference)
      Assert.assertEquals(subject, carePlan.captured.subject.reference)
    }
  }

  @Test
  fun testFetchCarePlanShouldReturnExpectedCarePlan() {
    coEvery {
      fhirEngine.search<CarePlan> { filter(CarePlan.SUBJECT) { value = "Patient/$PATIENT_ID_1" } }
    } returns listOf(buildCarePlanWithActive(PATIENT_ID_1))

    val carePlans = runBlocking { repository.fetchCarePlan(PATIENT_ID_1) }
    if (carePlans.isNotEmpty()) {
      Assert.assertEquals(1, carePlans.size)
      with(carePlans.first()) { Assert.assertEquals("11190", id) }
    }
  }

  @Test
  fun fetchLastSceneItemTest() {
    val patientId = "1111"
    val encounter = listOf(getEncounter(patientId))
    val listLastItem = repository.fetchLastSeenItem(encounter)
    Assert.assertEquals("ABC", listLastItem[0].display)
  }

  @Test
  fun testFetchObservationsShouldReturnCorrectObs() {
    coEvery { fhirEngine.search<Observation>(any()) } returns
      listOf(
        Observation().apply {
          value = IntegerType(4)
          effective = DateTimeType.now()
        }
      )
    val result = runBlocking { repository.fetchObservations("1111", "edd") }
    Assert.assertEquals(4, result.valueIntegerType.value)
  }

  @Test(expected = UnsupportedOperationException::class)
  fun testFetchObservationsShouldThrowExceptionOnUnrecognizedFilterType() {
    val result = runBlocking { repository.fetchObservations("1111", "not known") }
    Assert.assertEquals(4, result.valueIntegerType.value)
  }

  @Test
  fun testRecordComputedBmiShouldExtractAndSaveResources() {
    every { repository.resourceMapperExtended } returns
      mockk { coEvery { saveParsedResource(any(), any(), any(), any()) } returns Unit }

    coEvery { fhirEngine.save(any()) } returns Unit

    runBlocking {
      val result =
        repository.recordComputedBmi(
          questionnaire = mockk(),
          questionnaireResponse = mockk(),
          patientId = "patient_1",
          encounterID = "encounter_1",
          height = 1.6764,
          weight = 50.0,
          computedBmi = 9.8
        )

      coVerify(exactly = 4) { fhirEngine.save(any()) }
      Assert.assertTrue(result)
    }
  }

  @Test
  fun testSearchCarePlanShouldReturnListOfCarePlans() {

    coEvery { fhirEngine.search<CarePlan>(any()) } returns listOf(buildCarePlan("99"))
    val carePlans = runBlocking { repository.searchCarePlan("") }

    Assert.assertEquals(1, carePlans.size)
    Assert.assertEquals("Patient/99", carePlans[0].subject.reference)
    Assert.assertEquals(
      CarePlan.CarePlanActivityStatus.SCHEDULED,
      carePlans[0].activityFirstRep.detail.status
    )
  }

  @Test
  fun testCountAllShouldReturnMoreThanOneCount() {

    coEvery { fhirEngine.count(any()) } returns 5
    val count = runBlocking { repository.countAll() }
    Assert.assertEquals(5, count)
  }

  @Test
  fun testFetchEncountersShouldReturnExpectedEncounterList() {

    coEvery { fhirEngine.search<Encounter>(any()) } returns listOf(getEncounter("99"))
    val encounterList = runBlocking { repository.fetchEncounters("") }

    Assert.assertEquals(1, encounterList.size)
    with(encounterList[0]) {
      Assert.assertEquals("1", id)
      Assert.assertEquals("Patient/99", subject.reference)
      Assert.assertEquals(Encounter.EncounterStatus.FINISHED, status)
    }
  }

  @Test
  fun testSetAncItemMapperTypeShouldVerifyAncItemMapperType() {
    every { ancItemMapper.setAncItemMapperType(any()) } returns Unit
    repository.setAncItemMapperType(mockk())

    verify(exactly = 1) { ancItemMapper.setAncItemMapperType(any()) }
  }

  private fun buildCarePlan(subject: String): CarePlan {
    return CarePlan().apply {
      this.subject = Reference().apply { reference = "Patient/$subject" }
      this.addActivity().detail.apply {
        this.scheduledPeriod.start = Date()
        this.status = CarePlan.CarePlanActivityStatus.SCHEDULED
      }
    }
  }

  private fun getEncounter(patientId: String): Encounter {
    return Encounter().apply {
      id = "1"
      type = listOf(getCodeableConcept())
      subject = Reference().apply { reference = "Patient/$patientId" }
      status = Encounter.EncounterStatus.FINISHED
      class_ = Coding("", "", "ABC")
      period = Period().apply { start = SimpleDateFormat("yyyy-MM-dd").parse("2021-01-01") }
    }
  }

  private fun getCodeableConcept(): CodeableConcept {
    return CodeableConcept().apply {
      id = "1"
      coding = listOf(getCodingList())
      text = "ABC"
    }
  }

  private fun getCodingList(): Coding {
    return Coding().apply {
      id = "1"
      system = "123"
      code = "123"
      display = "ABC"
    }
  }

  private fun getTask(): Task {
    return Task().apply {
      id = "1"
      code = getCodeableConcept()
      executionPeriod = Period().setStart(Date())
    }
  }

  private fun buildPatient(id: String, family: String, given: String): Patient {
    return Patient().apply {
      this.id = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }
      active = true
    }
  }

  companion object {
    private const val PATIENT_ID_1 = "test_patient_id_1"
    private const val PATIENT_ID_2 = "test_patient_id_2"
  }
}
