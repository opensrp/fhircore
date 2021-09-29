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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Goal
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.anc.model.AncVisitStatus
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable
import org.smartregister.fhircore.engine.util.extension.plusWeeksAsString

class AncPatientRepositoryTest : RobolectricTest() {
  private lateinit var repository: AncPatientRepository
  private lateinit var fhirEngine: FhirEngine

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    fhirEngine = spyk()
    repository = spyk(AncPatientRepository(fhirEngine, AncItemMapper))
  }

  @Test
  fun testLoadAllShouldReturnListOfFamilyItem() {
    coEvery { fhirEngine.search<Condition>(any()) } returns listOf(buildCondition("1111"))
    coEvery { repository.searchCarePlan(any()) } returns listOf(buildCarePlan("1111"))
    coEvery { fhirEngine.load(Patient::class.java, "1111") } returns
      buildPatient("1111", "Test", "Abc")
    coEvery { fhirEngine.load(Patient::class.java, "1110") } returns
      buildPatient("1110", "Test0", "Abc0")
    coEvery { fhirEngine.count(any()) } returns 10

    runBlocking {
      val ancList = repository.loadData("", 0, true)

      assertEquals("Abc Test", ancList[0].name)
      assertEquals("1111", ancList[0].patientIdentifier)

      assertEquals(AncVisitStatus.DUE, ancList[0].visitStatus)
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

      assertEquals(subject, condition.captured.subject.reference)
      assertEquals(subject, episode.captured.patient.reference)
      assertEquals(subject, encounter.captured.subject.reference)
      assertEquals(subject, goal.captured.subject.reference)
      assertEquals(subject, carePlan.captured.subject.reference)
    }
  }

  private fun buildCondition(subject: String): Condition {
    return Condition().apply {
      this.id = id
      this.code = CodeableConcept().apply { addCoding().apply { code = "123456" } }
      this.subject = Reference().apply { reference = "Patient/$subject" }
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
      this.addLink().apply { this.other = Reference().apply { reference = "Patient/1110" } }
    }
  }

  @Test
  fun fetchCarePlanItemTest() {
    val patientId = "1111"
    val carePlan = listOf(buildCarePlanWithActive(patientId))
    val listCarePlan = repository.fetchCarePlanItem(carePlan = carePlan, patientId = patientId)
    if (listCarePlan.isNotEmpty()) {
      assertEquals(patientId, listCarePlan[0].patientIdentifier)
      assertEquals("ABC", listCarePlan[0].title)
    }
  }

  @Test
  fun fetchUpcomingServiceItemTest() {
    val patientId = "1111"
    val carePlan = listOf(buildCarePlanWithActive(patientId))
    val listUpcomingServiceItem =
      repository.fetchUpcomingServiceItem(patientId = patientId, carePlan = carePlan)
    assertEquals(patientId, listUpcomingServiceItem[0].patientIdentifier)
    assertEquals("ABC", listUpcomingServiceItem[0].title)
    assertEquals(Date().makeItReadable(), listUpcomingServiceItem[0].date)
  }

  @Test
  fun fetchLastSceneItemTest() {
    val patientId = "1111"
    val encounter = listOf(getEncounter(patientId))
    val listLastItem = repository.fetchLastSeenItem(patientId = patientId, encounter)
    assertEquals(patientId, listLastItem[0].patientIdentifier)
    assertEquals("ABC", listLastItem[0].title)
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
}
