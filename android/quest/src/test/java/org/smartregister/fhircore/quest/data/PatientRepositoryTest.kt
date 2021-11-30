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

package org.smartregister.fhircore.quest.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.genderFull
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper

class PatientRepositoryTest : RobolectricTest() {

  private lateinit var repository: PatientRepository

  private lateinit var fhirEngine: FhirEngine

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @Before
  fun setUp() {
    fhirEngine = mockk()

    val config =
      ApplicationProvider.getApplicationContext<QuestApplication>()
        .registerViewConfigurationOf(
          primaryFilter = SearchFilter("_tag", "1111", "http://mysystem")
        )

    repository =
      PatientRepository(
        fhirEngine,
        PatientItemMapper,
        MutableLiveData(config),
        coroutinesTestRule.testDispatcherProvider
      )
  }

  @Test
  fun testFetchDemographicsShouldReturnTestPatient() {
    coEvery { fhirEngine.load(Patient::class.java, "1") } returns
      buildPatient("1", "doe", "john", 0)

    val patient = repository.fetchDemographics("1").value
    Assert.assertEquals("john", patient?.name?.first()?.given?.first()?.value)
    Assert.assertEquals("doe", patient?.name?.first()?.family)
  }

  @Test
  fun testLoadDataShouldReturnPatientItemList() = runBlockingTest {
    coEvery { fhirEngine.search<Patient>(any()) } returns
      listOf(buildPatient("1234", "Doe", "John", 1, Enumerations.AdministrativeGender.FEMALE))
    coEvery { fhirEngine.count(any()) } returns 1

    val data = repository.loadData("", 0, true)

    Assert.assertEquals("1234", data[0].id)
    Assert.assertEquals("John Doe", data[0].name)
    Assert.assertEquals("1y", data[0].age)
    Assert.assertEquals("F", data[0].gender)
    Assert.assertEquals("Female", data[0].genderFull())

    coVerify { fhirEngine.search<Patient>(any()) }
  }

  @Test
  fun testFetchTestResultsShouldReturnListOfTestReports() {

    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
    val yesterday =
      Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())

    coEvery { fhirEngine.search<QuestionnaireResponse>(any()) } returns
      listOf(
        QuestionnaireResponse().apply {
          meta = Meta().apply { tag = listOf(Coding().apply { display = "Blood Count" }) }
          authored = yesterday
        },
        QuestionnaireResponse().apply {
          meta = Meta().apply { tag = listOf(Coding().apply { display = "Cell Count" }) }
          authored = today
        }
      )

    val results = runBlocking { repository.fetchTestResults("1") }.value

    Assert.assertEquals("Cell Count", results?.get(0)?.meta?.tagFirstRep?.display)
    Assert.assertEquals(today.time, results?.get(0)?.authored?.time)

    Assert.assertEquals("Blood Count", results?.get(1)?.meta?.tagFirstRep?.display)
    Assert.assertEquals(yesterday.time, results?.get(1)?.authored?.time)
  }

  @Test
  fun testFetchTestFormShouldReturnListOfQuestionnaireConfig() {
    coEvery { fhirEngine.search<Questionnaire>(any()) } returns
      listOf(
        Questionnaire().apply {
          name = "g6pd-test"
          title = "G6PD Test"
        }
      )

    val results = runBlocking { repository.fetchTestForms(SearchFilter("", "abc", "cde")) }.value

    with(results!!.first()) {
      Assert.assertEquals("g6pd-test", form)
      Assert.assertEquals("G6PD Test", title)
    }
  }

  private fun buildPatient(
    id: String,
    family: String,
    given: String,
    age: Int,
    gender: Enumerations.AdministrativeGender = Enumerations.AdministrativeGender.MALE
  ): Patient {
    return Patient().apply {
      this.id = id
      this.identifierFirstRep.value = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.gender = gender
      this.birthDate = DateType(Date()).apply { add(Calendar.YEAR, -age) }.dateTimeValue().value

      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }
    }
  }
}
