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

package org.smartregister.fhircore.quest.ui.patient.details

import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.Locale
import javax.inject.Inject
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper

@HiltAndroidTest
class ListDataDetailViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var patientItemMapper: PatientItemMapper

  @BindValue val patientRepository: PatientRepository = mockk()
  @BindValue val defaultRepository: DefaultRepository = mockk()
  @BindValue val libraryEvaluator: LibraryEvaluator = spyk()

  private val patientId = "5583145"

  private lateinit var listDataDetailViewModel: ListDataDetailViewModel
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = mockk()
    Faker.initPatientRepositoryMocks(patientRepository)
    listDataDetailViewModel =
      ListDataDetailViewModel(
        patientRepository = patientRepository,
        defaultRepository = defaultRepository,
        patientItemMapper = patientItemMapper,
        libraryEvaluator = libraryEvaluator,
        fhirEngine = fhirEngine
      )
  }

  @Test
  fun testGetDemographicsShouldFetchPatient() {
    listDataDetailViewModel.getDemographicsWithAdditionalData(
      patientId,
      mockk { every { valuePrefix } returns "" }
    )
    val patient = listDataDetailViewModel.patientItem.value
    Assert.assertNotNull(patient)
    Assert.assertEquals(patientId, patient!!.id)
  }

  @Test
  fun testOnMenuItemClickListener() {
    listDataDetailViewModel.onMenuItemClickListener(R.string.test_results)
    Assert.assertNotNull(listDataDetailViewModel.onMenuItemClicked.value)
  }

  @Test
  fun testOnBackPressed() {
    listDataDetailViewModel.onBackPressed(true)
    Assert.assertNotNull(listDataDetailViewModel.onBackPressClicked.value)
    Assert.assertTrue(listDataDetailViewModel.onBackPressClicked.value!!)
  }

  @Test
  fun testOnFormItemClickListener() {
    val questionnaireConfig = mockk<QuestionnaireConfig>()
    listDataDetailViewModel.onFormItemClickListener(questionnaireConfig)
    Assert.assertNotNull(listDataDetailViewModel.onFormItemClicked.value)
    Assert.assertEquals(questionnaireConfig, listDataDetailViewModel.onFormItemClicked.value!!)
  }

  @Test
  fun testOnTestResultItemClickListener() {
    val resultItem = mockk<QuestResultItem>()
    listDataDetailViewModel.onTestResultItemClickListener(resultItem)
    Assert.assertNotNull(listDataDetailViewModel.onFormTestResultClicked.value)
    Assert.assertEquals(resultItem, listDataDetailViewModel.onFormTestResultClicked.value!!)
  }

  @Test
  fun testGetAllForms() {
    listDataDetailViewModel.getAllForms(mockk())
    Assert.assertNotNull(listDataDetailViewModel.questionnaireConfigs.value)
    Assert.assertEquals(2, listDataDetailViewModel.questionnaireConfigs.value!!.size)
    Assert.assertEquals(
      "12345",
      listDataDetailViewModel.questionnaireConfigs.value!!.first().identifier
    )
    Assert.assertEquals(
      "67890",
      listDataDetailViewModel.questionnaireConfigs.value!!.last().identifier
    )
  }

  fun testFetchResultNonNullNameShouldReturnNameValue() {
    val result =
      listDataDetailViewModel.fetchResultItemLabel(
        testResult =
          Pair(
            QuestionnaireResponse(),
            Questionnaire().apply {
              name = "Sample name"
              title = "Sample title"
            }
          )
      )

    Assert.assertEquals("Sample name", result)
    Assert.assertNotEquals("Sample title", result)
  }

  @Test
  fun testFetchResultNullNameShouldReturnTitleValue() {
    val result =
      listDataDetailViewModel.fetchResultItemLabel(
        testResult =
          Pair(
            QuestionnaireResponse(),
            Questionnaire().apply {
              name = null
              title = "Sample title"
            }
          )
      )

    Assert.assertEquals("Sample title", result)
    Assert.assertNotEquals("Sample name", result)
  }

  @Test
  fun testFetchResultNullNameTitleShouldReturnId() {
    val result =
      listDataDetailViewModel.fetchResultItemLabel(
        testResult =
          Pair(
            QuestionnaireResponse(),
            Questionnaire().apply {
              id = "1234"
              name = null
              title = null
            }
          )
      )

    Assert.assertEquals("1234", result)
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
              }
            )
          }

        nameElement =
          StringType("Registration2").apply {
            addExtension(
              Extension().apply {
                url = "http://hl7.org/fhir/StructureDefinition/translation"
                addExtension("lang", StringType("sw"))
                addExtension("content", StringType("Sajili2"))
              }
            )
          }
      }

    Locale.setDefault(Locale.forLanguageTag("en"))
    Assert.assertEquals(
      "Registration",
      listDataDetailViewModel.fetchResultItemLabel(Pair(QuestionnaireResponse(), questionnaire))
    )

    Locale.setDefault(Locale.forLanguageTag("sw"))
    Assert.assertEquals(
      "Sajili",
      listDataDetailViewModel.fetchResultItemLabel(Pair(QuestionnaireResponse(), questionnaire))
    )
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
              }
            )
          }
      }

    Locale.setDefault(Locale.forLanguageTag("en"))
    Assert.assertEquals(
      "Registration",
      listDataDetailViewModel.fetchResultItemLabel(Pair(QuestionnaireResponse(), questionnaire))
    )

    Locale.setDefault(Locale.forLanguageTag("sw"))
    Assert.assertEquals(
      "Sajili",
      listDataDetailViewModel.fetchResultItemLabel(Pair(QuestionnaireResponse(), questionnaire))
    )
  }
}
