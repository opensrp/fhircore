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
import javax.inject.Inject
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
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

  private lateinit var questPatientDetailViewModel: ListDataDetailViewModel
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = mockk()
    Faker.initPatientRepositoryMocks(patientRepository)
    questPatientDetailViewModel =
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
    questPatientDetailViewModel.getDemographicsWithAdditionalData(
      patientId,
      mockk { every { valuePrefix } returns "" }
    )
    val patient = questPatientDetailViewModel.patientItem.value
    Assert.assertNotNull(patient)
    Assert.assertEquals(patientId, patient!!.id)
  }

  @Test
  fun testOnMenuItemClickListener() {
    questPatientDetailViewModel.onMenuItemClickListener(R.string.test_results)
    Assert.assertNotNull(questPatientDetailViewModel.onMenuItemClicked.value)
  }

  @Test
  fun testOnBackPressed() {
    questPatientDetailViewModel.onBackPressed(true)
    Assert.assertNotNull(questPatientDetailViewModel.onBackPressClicked.value)
    Assert.assertTrue(questPatientDetailViewModel.onBackPressClicked.value!!)
  }

  @Test
  fun testOnFormItemClickListener() {
    val questionnaireConfig = mockk<QuestionnaireConfig>()
    questPatientDetailViewModel.onFormItemClickListener(questionnaireConfig)
    Assert.assertNotNull(questPatientDetailViewModel.onFormItemClicked.value)
    Assert.assertEquals(questionnaireConfig, questPatientDetailViewModel.onFormItemClicked.value!!)
  }

  @Test
  fun testOnTestResultItemClickListener() {
    val resultItem = mockk<QuestResultItem>()
    questPatientDetailViewModel.onTestResultItemClickListener(resultItem)
    Assert.assertNotNull(questPatientDetailViewModel.onFormTestResultClicked.value)
    Assert.assertEquals(resultItem, questPatientDetailViewModel.onFormTestResultClicked.value!!)
  }

  @Test
  fun testGetAllForms() {
    questPatientDetailViewModel.getAllForms(mockk())
    Assert.assertNotNull(questPatientDetailViewModel.questionnaireConfigs.value)
    Assert.assertEquals(2, questPatientDetailViewModel.questionnaireConfigs.value!!.size)
    Assert.assertEquals(
      "12345",
      questPatientDetailViewModel.questionnaireConfigs.value!!.first().identifier
    )
    Assert.assertEquals(
      "67890",
      questPatientDetailViewModel.questionnaireConfigs.value!!.last().identifier
    )
  }

  fun testFetchResultNonNullNameShouldReturnNameValue() {
    val result =
      questPatientDetailViewModel.fetchResultItemLabel(
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
      questPatientDetailViewModel.fetchResultItemLabel(
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
      questPatientDetailViewModel.fetchResultItemLabel(
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
}
