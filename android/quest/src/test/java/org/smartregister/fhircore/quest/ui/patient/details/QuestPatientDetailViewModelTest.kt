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

import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class QuestPatientDetailViewModelTest : RobolectricTest() {

  private lateinit var viewModel: QuestPatientDetailViewModel
  private lateinit var repository: PatientRepository
  private val patientId = "0"

  @Before
  fun setUp() {
    repository = mockk()
    viewModel =
      QuestPatientDetailViewModel.get(
        Robolectric.buildActivity(QuestPatientDetailActivity::class.java).get(),
        ApplicationProvider.getApplicationContext(),
        repository,
        patientId
      )
  }

  @Test
  fun testVerifyBackPressListener() {
    var count = 0

    viewModel.setOnBackPressListener { ++count }

    viewModel.onBackPressListener().invoke()
    Assert.assertEquals(1, count)
  }

  @Test
  fun testVerifyMenuItemClickListener() {

    viewModel.setOnMenuItemClickListener { Assert.assertEquals("ONE", it) }
    viewModel.onMenuItemClickListener().invoke("ONE")
  }

  @Test
  fun testGetDemographicsShouldReturnDummyPatient() {

    every { repository.fetchDemographics(patientId) } returns
      MutableLiveData(
        Patient().apply {
          name =
            listOf(
              HumanName().apply {
                given = listOf(StringType("john"))
                family = "doe"
              }
            )
        }
      )

    val patient = viewModel.getDemographics().value

    Assert.assertEquals("john", patient?.name?.first()?.given?.first()?.value)
    Assert.assertEquals("doe", patient?.name?.first()?.family)
  }

  @Test
  fun testGetAllFormsShouldReturnListOfQuestionnaireConfig() {

    every { repository.fetchTestForms(any(), any()) } returns
      MutableLiveData(listOf(QuestionnaireConfig("g6pd-test-result", "G6PD Test Result", "3440")))

    val forms = viewModel.getAllForms().value

    with(forms!!.first()) {
      Assert.assertEquals("3440", identifier)
      Assert.assertEquals("g6pd-test-result", form)
      Assert.assertEquals("G6PD Test Result", title)
    }
  }

  @Test
  fun testGetAllResultsShouldReturnListOfTestReports() {

    every { repository.fetchTestResults(patientId) } returns
      MutableLiveData(
        listOf(
          QuestionnaireResponse().apply {
            meta = Meta().apply { tag = listOf(Coding().apply { display = "Blood Count" }) }
          }
        )
      )

    val results = viewModel.getAllResults().value

    with(results!!.first()) { Assert.assertEquals("Blood Count", meta?.tagFirstRep?.display) }
  }

  @Test
  fun testVerifyFormItemClickListener() {

    viewModel.setOnFormItemClickListener {
      Assert.assertEquals("test", it.form)
      Assert.assertEquals("Test", it.title)
      Assert.assertEquals("0", it.identifier)
    }

    viewModel.onFormItemClickListener().invoke(QuestionnaireConfig("test", "Test", "0"))
  }

  @Test
  fun testVerifyTestResultItemClickListener() {

    viewModel.setOnTestResultItemClickListener {
      Assert.assertEquals("Blood Count", it.meta?.tagFirstRep?.display)
    }

    viewModel
      .onTestResultItemClickListener()
      .invoke(
        QuestionnaireResponse().apply {
          meta = Meta().apply { tag = listOf(Coding().apply { display = "Blood Count" }) }
        }
      )
  }
}
