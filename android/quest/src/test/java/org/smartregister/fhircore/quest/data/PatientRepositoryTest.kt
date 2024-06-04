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
import com.google.android.fhir.SearchResult
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.app.fakes.Faker.buildPatient
import org.smartregister.fhircore.quest.configuration.view.Properties
import org.smartregister.fhircore.quest.configuration.view.filterOf
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.genderFull
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.quest.util.loadAdditionalData

@HiltAndroidTest
@Ignore("To be deleted test class; new test to be written after refactor")
class PatientRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var patientItemMapper: PatientItemMapper

  @BindValue var configurationRegistry: ConfigurationRegistry = mockk()

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private val fhirEngine: FhirEngine = mockk()

  private lateinit var repository: PatientRepository

  @Before
  fun setUp() {
    hiltRule.inject()

    every { configurationRegistry.appId } returns "quest"

    repository =
      PatientRepository(
        fhirEngine,
        patientItemMapper,
        dispatcherProvider,
        configurationRegistry,
      )
  }

  private fun searchFilter() =
    SearchFilter(
      id = "",
      key = "abc",
      filterType = Enumerations.SearchParamType.STRING,
      valueString = "cde",
      valueType = Enumerations.DataType.CODEABLECONCEPT,
    )

  @Test
  fun testFetchDemographicsShouldReturnTestPatient() = runTest {
    coEvery { fhirEngine.get(ResourceType.Patient, "1") } returns
      buildPatient("1", "doe", "john", 0)

    val patient = repository.fetchDemographics("1")
    Assert.assertEquals("john", patient.name?.first()?.given?.first()?.value)
    Assert.assertEquals("doe", patient.name?.first()?.family)
  }

  @Test
  fun testFetchDemographicsWithAdditionalDataShouldReturnTestPatientWithAdditionalData() = runTest {
    mockkStatic(::loadAdditionalData)
    coEvery { loadAdditionalData(any(), any(), any()) } returns
      listOf(AdditionalData("label", "value", "valuePrefix", null))
    coEvery { fhirEngine.get(ResourceType.Patient, "1") } returns
      buildPatient("1", "doe", "john", 0)

    val patientItem = repository.fetchDemographicsWithAdditionalData("1")
    Assert.assertEquals("John Doe", patientItem.name)
    Assert.assertTrue(patientItem.additionalData!!.isNotEmpty())
    unmockkStatic(::loadAdditionalData)
  }

  @Test
  fun testLoadDataShouldReturnPatientItemList() = runTest {
    mockkStatic(::loadAdditionalData)
    coEvery { loadAdditionalData(any(), any(), any()) } returns listOf()
    coEvery { fhirEngine.search<Patient>(any<Search>()) } returns
      listOf(
        SearchResult(
          buildPatient("1234", "Doe", "John", 1, Enumerations.AdministrativeGender.FEMALE),
          included = null,
          revIncluded = null,
        ),
      )
    coEvery { fhirEngine.count(any()) } returns 1

    val data = repository.loadData("", 0, true)
    Assert.assertEquals("1234", data[0].id)
    Assert.assertEquals("John Doe", data[0].name)
    Assert.assertEquals("1y", data[0].age)
    Assert.assertEquals("F", data[0].gender)
    Assert.assertEquals("Female", data[0].genderFull())

    coVerify { fhirEngine.search<Patient>(any<Search>()) }
    unmockkStatic(::loadAdditionalData)
  }

  @Test
  fun testCountAllShouldReturnNumberOfPatients() = runTest {
    coEvery { fhirEngine.count(any()) } returns 1
    val data = repository.countAll()
    Assert.assertEquals(1, data)
  }

  @Test
  fun testGetQuestionnaireOfQuestionnaireResponseShouldReturnNonEmptyQuestionnaire() {
    runTest {
      coEvery { fhirEngine.get(ResourceType.Questionnaire, any()) } returns
        Questionnaire().apply {
          id = "1"
          name = "Sample Questionnaire name"
          title = "Sample Questionnaire title"
        }

      val questionnaire =
        repository.getQuestionnaire(
          QuestionnaireResponse().apply { questionnaire = "Questionnaire/1" },
        )

      Assert.assertEquals("1", questionnaire.id)
      Assert.assertEquals("Sample Questionnaire name", questionnaire.name)
      Assert.assertEquals("Sample Questionnaire title", questionnaire.title)
    }
  }

  @Test
  fun testLoadEncounterShouldReturnNonEmptyEncounter() = runTest {
    coEvery { fhirEngine.get(ResourceType.Encounter, any()) } returns
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
    runTest {
      coEvery { fhirEngine.get(ResourceType.Questionnaire, any()) } returns Questionnaire()

      val questionnaire = repository.getQuestionnaire(QuestionnaireResponse())

      Assert.assertNull(questionnaire.id)
      Assert.assertNull(questionnaire.name)
      Assert.assertNull(questionnaire.title)
    }
  }

  @Test
  fun testFetchTestFormShouldReturnListOfQuestionnaireConfig() = runTest {
    coEvery { fhirEngine.search<Questionnaire>(any<Search>()) } returns
      listOf(
        SearchResult(
          Questionnaire().apply {
            name = "g6pd-test"
            title = "G6PD Test"
          },
          included = null,
          revIncluded = null,
        ),
      )

    val results = repository.fetchTestForms(searchFilter())

    with(results.first()) {
      Assert.assertEquals("g6pd-test", form)
      Assert.assertEquals("G6PD Test", title)
    }
  }

  @Test
  fun testFetchTestFormShouldHandleNullNameAndTitle() = runTest {
    coEvery { fhirEngine.search<Questionnaire>(any<Search>()) } returns
      listOf(
        SearchResult(Questionnaire().apply { id = "1234" }, included = null, revIncluded = null),
      )

    val results = repository.fetchTestForms(searchFilter())

    with(results.first()) {
      Assert.assertEquals("1234", form)
      Assert.assertEquals("1234", title)
    }
  }

  @Test
  fun testFetchTestFormShouldHandleNullTitle() = runTest {
    coEvery { fhirEngine.search<Questionnaire>(any<Search>()) } returns
      listOf(
        SearchResult(
          Questionnaire().apply {
            id = "1234"
            name = "Form name"
          },
          included = null,
          revIncluded = null,
        ),
      )

    val results = repository.fetchTestForms(searchFilter())
    with(results.first()) {
      Assert.assertEquals("Form name", form)
      Assert.assertEquals("Form name", title)
    }
  }

  @Test
  fun testFetchTestFormShouldHandleNullName() = runTest {
    coEvery { fhirEngine.search<Questionnaire>(any<Search>()) } returns
      listOf(
        SearchResult(
          Questionnaire().apply {
            id = "1234"
            title = "Form name"
          },
          included = null,
          revIncluded = null,
        ),
      )

    val results = repository.fetchTestForms(searchFilter())
    with(results.first()) {
      Assert.assertEquals("1234", form)
      Assert.assertEquals("Form name", title)
    }
  }

  private fun getEncounter() = Encounter().apply { id = "1" }

  private fun getObservations(): List<SearchResult<Observation>> {
    return listOf(
      SearchResult(
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
        included = null,
        revIncluded = null,
      ),
      SearchResult(
        Observation().apply {
          encounter = Reference().apply { reference = "Encounter/1" }
          code =
            CodeableConcept().apply {
              addCoding().apply {
                system = "http://snomed.info/sct"
                code = "259695003"
              }
            }
        },
        included = null,
        revIncluded = null,
      ),
    )
  }

  private fun getConditions(): List<SearchResult<Condition>> {
    val today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
    return listOf(
      SearchResult(
        Condition().apply {
          recordedDate = today
          category =
            listOf(
              CodeableConcept().apply {
                addCoding().apply {
                  system = "http://snomed.info/sct"
                  code = "9024005"
                }
              },
            )
          code =
            CodeableConcept().apply {
              addCoding().apply {
                system = "http://snomed.info/sct"
                code = "11896004"
                display = "Intermediate"
              }
            }
        },
        included = null,
        revIncluded = null,
      ),
    )
  }

  @Test
  fun testGetConditionShouldReturnValidCondition() = runBlockingTest {
    coEvery { fhirEngine.search<Condition>(any<Search>()) } returns
      listOf(SearchResult(Condition().apply { id = "c1" }, included = null, revIncluded = null))

    val result =
      repository.getCondition(
        Encounter().apply { id = "123" },
        filterOf("code", "Code", Properties()),
      )

    coVerify { fhirEngine.search<Condition>(any<Search>()) }

    Assert.assertEquals("c1", result!!.first().logicalId)
  }

  @Test
  fun testGetObservationShouldReturnValidObservation() = runBlockingTest {
    coEvery { fhirEngine.search<Observation>(any<Search>()) } returns
      listOf(SearchResult(Observation().apply { id = "o1" }, included = null, revIncluded = null))

    val result =
      repository.getObservation(
        Encounter().apply { id = "123" },
        filterOf("code", "Code", Properties()),
      )

    coVerify { fhirEngine.search<Observation>(any<Search>()) }

    Assert.assertEquals("o1", result.first().logicalId)
  }

  @Test
  fun testGetMedicationRequestShouldReturnValidMedicationRequest() = runBlockingTest {
    coEvery { fhirEngine.search<MedicationRequest>(any<Search>()) } returns
      listOf(
        SearchResult(MedicationRequest().apply { id = "mr1" }, included = null, revIncluded = null),
      )

    val result =
      repository.getMedicationRequest(
        Encounter().apply { id = "123" },
        filterOf("code", "Code", Properties()),
      )

    coVerify { fhirEngine.search<MedicationRequest>(any<Search>()) }

    Assert.assertEquals("mr1", result.first().logicalId)
  }

  @Test
  fun fetchResultItemLabelShouldReturnLocalizedQuestionnaireTitle() {
    val questionnaire =
      Questionnaire().apply {
        titleElement =
          StringType("Registration").apply {
            addExtension(
              Extension().apply {
                url = "http://hl7.org/fhir/StructureDefinition/translation"
                addExtension("lang", StringType("sw"))
                addExtension("content", StringType("Sajili"))
              },
            )
          }

        nameElement =
          StringType("Registration2").apply {
            addExtension(
              Extension().apply {
                url = "http://hl7.org/fhir/StructureDefinition/translation"
                addExtension("lang", StringType("sw"))
                addExtension("content", StringType("Sajili2"))
              },
            )
          }
      }

    Locale.setDefault(Locale.forLanguageTag("en"))
    Assert.assertEquals("Registration", repository.fetchResultItemLabel(questionnaire))

    Locale.setDefault(Locale.forLanguageTag("sw"))
    Assert.assertEquals("Sajili", repository.fetchResultItemLabel(questionnaire))
  }

  @Test
  fun fetchResultItemLabelShouldReturnLocalizedQuestionnaireNameWhenTitleIsAbsent() {
    val questionnaire =
      Questionnaire().apply {
        nameElement =
          StringType("Registration").apply {
            addExtension(
              Extension().apply {
                url = "http://hl7.org/fhir/StructureDefinition/translation"
                addExtension("lang", StringType("sw"))
                addExtension("content", StringType("Sajili"))
              },
            )
          }
      }

    Locale.setDefault(Locale.forLanguageTag("en"))
    Assert.assertEquals("Registration", repository.fetchResultItemLabel(questionnaire))

    Locale.setDefault(Locale.forLanguageTag("sw"))
    Assert.assertEquals("Sajili", repository.fetchResultItemLabel(questionnaire))
  }
}
