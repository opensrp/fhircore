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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.app.fakes.Faker.buildPatient
import org.smartregister.fhircore.quest.configuration.parser.QuestDetailConfigParser
import org.smartregister.fhircore.quest.configuration.view.patientDetailsViewConfigurationOf
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.genderFull
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.quest.util.loadAdditionalData

@HiltAndroidTest
class PatientRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var patientItemMapper: PatientItemMapper
  @Inject lateinit var accountAuthenticator: AccountAuthenticator
  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private val fhirEngine: FhirEngine = mockk()

  private lateinit var repository: PatientRepository

  @Before
  fun setUp() {
    hiltRule.inject()
    configurationRegistry.loadAppConfigurations("g6pd", accountAuthenticator) {}
    repository =
      PatientRepository(
        fhirEngine,
        patientItemMapper,
        coroutineTestRule.testDispatcherProvider,
        configurationRegistry
      )
  }

  @Test
  fun testFetchDemographicsShouldReturnTestPatient() =
    coroutineTestRule.runBlockingTest {
      coEvery { fhirEngine.load(Patient::class.java, "1") } returns
        buildPatient("1", "doe", "john", 0)

      val patient = repository.fetchDemographics("1")
      Assert.assertEquals("john", patient.name?.first()?.given?.first()?.value)
      Assert.assertEquals("doe", patient.name?.first()?.family)
    }

  @Test
  fun testFetchDemographicsWithAdditionalDataShouldReturnTestPatientWithAdditionalData() =
    coroutineTestRule.runBlockingTest {
      mockkStatic(::loadAdditionalData)
      coEvery { loadAdditionalData(any(), any(), any()) } returns
        listOf(AdditionalData("label", "value", "valuePrefix", null))
      coEvery { fhirEngine.load(Patient::class.java, "1") } returns
        buildPatient("1", "doe", "john", 0)

      val patientItem = repository.fetchDemographicsWithAdditionalData("1")
      Assert.assertEquals("John Doe", patientItem.name)
      Assert.assertTrue(patientItem.additionalData!!.isNotEmpty())
      unmockkStatic(::loadAdditionalData)
    }

  @Test
  fun testLoadDataShouldReturnPatientItemList() = runBlockingTest {
    mockkStatic(::loadAdditionalData)
    coEvery { loadAdditionalData(any(), any(), any()) } returns listOf()
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
    unmockkStatic(::loadAdditionalData)
  }

  @Test
  fun testCountAllShouldReturnNumberOfPatients() = runBlockingTest {
    coEvery { fhirEngine.count(any()) } returns 1
    val data = repository.countAll()
    Assert.assertEquals(1, data)
  }

  @Test
  fun testFetchTestResultsShouldReturnListOfTestReports() =
    coroutineTestRule.runBlockingTest {
      val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
      val yesterday =
        Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())

      coEvery { fhirEngine.load(Questionnaire::class.java, "1") } returns
        Questionnaire().apply { name = "First Questionnaire" }

      coEvery { fhirEngine.load(Questionnaire::class.java, "2") } returns
        Questionnaire().apply { name = "Second Questionnaire" }

      coEvery {
        fhirEngine.search<QuestionnaireResponse> {
          filter(QuestionnaireResponse.SUBJECT, { value = "Patient/1" })
          filter(QuestionnaireResponse.QUESTIONNAIRE, { value = "Questionnaire/1" })
        }
      } returns
        listOf(
          QuestionnaireResponse().apply {
            authored = today
            questionnaire = "Questionnaire/1"
          },
          QuestionnaireResponse().apply {
            authored = yesterday
            questionnaire = "Questionnaire/2"
          }
        )

      val parser = QuestDetailConfigParser(fhirEngine)

      val results =
        repository.fetchTestResults(
          "1",
          listOf(QuestionnaireConfig("quest", "form", "title", "1")),
          patientDetailsViewConfigurationOf(),
          parser
        )

      Assert.assertEquals("First Questionnaire", results[0].data[0][0].value)
      Assert.assertEquals(" (${today.asDdMmmYyyy()})", results[0].data[0][1].value)

      Assert.assertEquals("Second Questionnaire", results[1].data[0][0].value)
      Assert.assertEquals(" (${yesterday.asDdMmmYyyy()})", results[1].data[0][1].value)
    }

  @Test
  fun testGetQuestionnaireOfQuestionnaireResponseShouldReturnNonEmptyQuestionnaire() {
    coroutineTestRule.runBlockingTest {
      coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns
        Questionnaire().apply {
          id = "1"
          name = "Sample Questionnaire name"
          title = "Sample Questionnaire title"
        }

      val questionnaire =
        repository.getQuestionnaire(
          QuestionnaireResponse().apply { questionnaire = "Questionnaire/1" }
        )

      Assert.assertEquals("1", questionnaire.id)
      Assert.assertEquals("Sample Questionnaire name", questionnaire.name)
      Assert.assertEquals("Sample Questionnaire title", questionnaire.title)
    }
  }

  @Test
  fun testLoadEncounterShouldReturnNonEmptyEncounter() = runBlockingTest {
    coEvery { fhirEngine.load(Encounter::class.java, any()) } returns
      Encounter().apply {
        id = "1"
        status = Encounter.EncounterStatus.INPROGRESS
      }

    val result = repository.loadEncounter("1")

    Assert.assertEquals("1", result.id)
    Assert.assertEquals(Encounter.EncounterStatus.INPROGRESS, result.status)
  }

  @Test
  fun testGetQuestionnaireOfQuestionnaireResponseShouldReturnEmptyQuestionnaire() {
    coroutineTestRule.runBlockingTest {
      coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns Questionnaire()

      val questionnaire = repository.getQuestionnaire(QuestionnaireResponse())

      Assert.assertNull(questionnaire.id)
      Assert.assertNull(questionnaire.name)
      Assert.assertNull(questionnaire.title)
    }
  }

  @Test
  fun testFetchTestFormShouldReturnListOfQuestionnaireConfig() =
    coroutineTestRule.runBlockingTest {
      coEvery { fhirEngine.search<Questionnaire>(any()) } returns
        listOf(
          Questionnaire().apply {
            name = "g6pd-test"
            title = "G6PD Test"
          }
        )

      val results = repository.fetchTestForms(SearchFilter("", "abc", "cde"))

      with(results.first()) {
        Assert.assertEquals("g6pd-test", form)
        Assert.assertEquals("G6PD Test", title)
      }
    }

  @Test
  fun testFetchTestFormShouldHandleNullNameAndTitle() =
    coroutineTestRule.runBlockingTest {
      coEvery { fhirEngine.search<Questionnaire>(any()) } returns
        listOf(Questionnaire().apply { id = "1234" })

      val results = repository.fetchTestForms(SearchFilter("", "abc", "cde"))

      with(results.first()) {
        Assert.assertEquals("1234", form)
        Assert.assertEquals("1234", title)
      }
    }

  @Test
  fun testFetchTestFormShouldHandleNullTitle() =
    coroutineTestRule.runBlockingTest {
      coEvery { fhirEngine.search<Questionnaire>(any()) } returns
        listOf(
          Questionnaire().apply {
            id = "1234"
            name = "Form name"
          }
        )

      val results = repository.fetchTestForms(SearchFilter("", "abc", "cde"))
      with(results.first()) {
        Assert.assertEquals("Form name", form)
        Assert.assertEquals("Form name", title)
      }
    }
}
