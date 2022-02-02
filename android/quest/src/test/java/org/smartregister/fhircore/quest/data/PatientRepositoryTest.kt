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

import ca.uhn.fhir.rest.gclient.TokenClientParam
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.quest.app.fakes.Faker.buildPatient
import org.smartregister.fhircore.quest.configuration.view.FontWeight
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
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

      val results =
        repository.fetchTestResults(
          "1",
          listOf(QuestionnaireConfig("quest", "form", "title", "1")),
          patientDetailsViewConfigurationOf()
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

  @Test
  fun testFetchTestFormShouldHandleNullName() =
    coroutineTestRule.runBlockingTest {
      coEvery { fhirEngine.search<Questionnaire>(any()) } returns
        listOf(
          Questionnaire().apply {
            id = "1234"
            title = "Form name"
          }
        )

      val results = repository.fetchTestForms(SearchFilter("", "abc", "cde"))
      with(results.first()) {
        Assert.assertEquals("1234", form)
        Assert.assertEquals("Form name", title)
      }
    }

  fun createTestConfigurationsData(): List<PatientDetailsViewConfiguration> =
    "configs/sample_patient_details_view_configurations.json".readFile().decodeJson()

  @Test
  fun testGetResultItemDynamicRowsEmptyShouldReturnCorrectData() {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())

    val questionnaire =
      Questionnaire().apply {
        this.id = "1"
        this.name = "Questionnaire Name"
        this.title = "Questionnaire Title"
      }

    val questionnaireResponse =
      QuestionnaireResponse().apply {
        this.id = "1"
        this.questionnaire = "Questionnaire/1"
        this.authored = today
      }

    val quest = createTestConfigurationsData()[0]
    val patientDetailsViewConfiguration =
      patientDetailsViewConfigurationOf(
        appId = quest.appId,
        classification = quest.classification,
        contentTitle = quest.contentTitle,
        dynamicRows = quest.dynamicRows
      )

    val data = runBlocking {
      repository.getResultItem(
        questionnaire,
        questionnaireResponse,
        patientDetailsViewConfiguration
      )
    }
    with(data.data[0]) {
      Assert.assertEquals("Questionnaire Name", this[0].value)
      Assert.assertEquals(" (${today.asDdMmmYyyy()})", this[1].value)
    }

    with(data.source) {
      Assert.assertEquals("1", first.id)
      Assert.assertEquals("Questionnaire/1", first.questionnaire)
      Assert.assertEquals(today, first.authored)

      Assert.assertEquals("1", second.id)
      Assert.assertEquals("Questionnaire Name", second.name)
      Assert.assertEquals("Questionnaire Title", second.title)
    }
  }

  @Test
  fun testGetResultItemDynamicRowsNonEmptyShouldReturnCorrectData() {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())

    coEvery {
      fhirEngine.search<Condition> {
        filter(Condition.ENCOUNTER, { value = "Encounter/1" })
        filter(
          TokenClientParam("category"),
          {
            value =
              of(CodeableConcept().addCoding(Coding("http://snomed.info/sct", "9024005", null)))
          }
        )
      }
    } returns getConditions()

    coEvery {
      fhirEngine.search<Observation> {
        filter(Observation.ENCOUNTER, { value = "Encounter/1" })
        filter(
          TokenClientParam("code"),
          {
            value =
              of(CodeableConcept().addCoding(Coding("http://snomed.info/sct", "259695003", null)))
          }
        )
      }
    } returns getObservations()

    coEvery { fhirEngine.load(Encounter::class.java, any()) } returns getEncounter()

    val questionnaire =
      Questionnaire().apply {
        this.id = "1"
        this.name = "Questionnaire Name"
        this.title = "Questionnaire Title"
      }

    val questionnaireResponse =
      QuestionnaireResponse().apply {
        this.id = "1"
        this.questionnaire = "Questionnaire/1"
        this.authored = today
        this.contained = listOf(Encounter().apply { id = "1" })
      }

    val g6pd = createTestConfigurationsData()[1]

    val patientDetailsViewConfiguration =
      patientDetailsViewConfigurationOf(
        appId = g6pd.appId,
        classification = g6pd.classification,
        contentTitle = g6pd.contentTitle,
        valuePrefix = g6pd.valuePrefix,
        dynamicRows = g6pd.dynamicRows
      )

    val data = runBlocking {
      repository.getResultItem(
        questionnaire,
        questionnaireResponse,
        patientDetailsViewConfiguration
      )
    }

    Assert.assertEquals(2, data.data.size)

    with(data.data[0]) {
      Assert.assertEquals("Intermediate", this[0].value)
      Assert.assertEquals("${today.asDdMmmYyyy()}", this[1].value)
    }

    with(data.data[1]) {
      Assert.assertEquals("G6PD: ", this[0].label)
      Assert.assertEquals("#74787A", this[0].properties?.label?.color)
      Assert.assertEquals(16, this[0].properties?.label?.textSize)
      Assert.assertEquals(FontWeight.NORMAL, this[0].properties?.label?.fontWeight)

      Assert.assertEquals(" - Hb: ", this[1].label)
      Assert.assertEquals("#74787A", this[1].properties?.label?.color)
      Assert.assertEquals(16, this[1].properties?.label?.textSize)
      Assert.assertEquals(FontWeight.NORMAL, this[1].properties?.label?.fontWeight)
    }
  }
  private fun getEncounter() = Encounter().apply { id = "1" }

  private fun getObservations(): List<Observation> {
    return listOf(
      Observation().apply {
        encounter = Reference().apply { reference = "Encounter/1" }
        code =
          CodeableConcept().apply {
            addCoding().apply {
              system = "http://snomed.info/sct"
              code = "86859003"
            }
          }
      },
      Observation().apply {
        encounter = Reference().apply { reference = "Encounter/1" }
        code =
          CodeableConcept().apply {
            addCoding().apply {
              system = "http://snomed.info/sct"
              code = "259695003"
            }
          }
      }
    )
  }

  private fun getConditions(): List<Condition> {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
    return listOf(
      Condition().apply {
        recordedDate = today
        category =
          listOf(
            CodeableConcept().apply {
              addCoding().apply {
                system = "http://snomed.info/sct"
                code = "9024005"
              }
            }
          )
        code =
          CodeableConcept().apply {
            addCoding().apply {
              system = "http://snomed.info/sct"
              code = "11896004"
              display = "Intermediate"
            }
          }
      }
    )
  }
}
