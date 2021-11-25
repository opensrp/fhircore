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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestPatientDetailViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @BindValue val patientRepository: PatientRepository = Faker.patientRepository

  private val patientId = "5583145"

  private lateinit var questPatientDetailViewModel: QuestPatientDetailViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    questPatientDetailViewModel = QuestPatientDetailViewModel(patientRepository)
  }

  @Test
  fun testGetDemographicsShouldFetchPatient() {
    questPatientDetailViewModel.getDemographics(patientId)
    val patient = questPatientDetailViewModel.patient.value
    Assert.assertNotNull(patient)
    Assert.assertEquals(patientId, patient!!.id)
  }

  @Test
  fun testOnMenuItemClickListener() {
    questPatientDetailViewModel.onMenuItemClickListener(true)
    Assert.assertNotNull(questPatientDetailViewModel.onMenuItemClicked.value)
    Assert.assertTrue(questPatientDetailViewModel.onMenuItemClicked.value!!)
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
    val questionnaireResponse = mockk<QuestionnaireResponse>()
    questPatientDetailViewModel.onTestResultItemClickListener(questionnaireResponse)
    Assert.assertNotNull(questPatientDetailViewModel.onFormTestResultClicked.value)
    Assert.assertEquals(
      questionnaireResponse,
      questPatientDetailViewModel.onFormTestResultClicked.value!!
    )
  }

  @Test
  fun testGetAllForms() {
    questPatientDetailViewModel.getAllForms(ApplicationProvider.getApplicationContext())
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
}
