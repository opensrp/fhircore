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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowBuild
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.shadow.TestUtils

@Config(shadows = [EirApplicationShadow::class, QuestionnaireActivityTest.CustomBuilder::class])
class QuestionnaireActivityTest : ActivityRobolectricTest() {
  private lateinit var context: EirApplication
  private lateinit var questionnaireActivity: QuestionnaireActivity
  private lateinit var questionnaireViewModel: QuestionnaireViewModel
  private lateinit var intent: Intent

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    val questionnaireConfig =
      QuestionnaireConfig(form = "patient-registration", title = "Add Patient", identifier = "1452")
    questionnaireViewModel = spyk(QuestionnaireViewModel(context, questionnaireConfig))
    val samplePatientRegisterQuestionnaire =
      TestUtils.loadQuestionnaire(context, REGISTER_QUESTIONNAIRE_ID)

    val fhirEngine: FhirEngine = mockk()

    coEvery { fhirEngine.load(Questionnaire::class.java, "1452") } returns
      samplePatientRegisterQuestionnaire
    coEvery { fhirEngine.load(Patient::class.java, TEST_PATIENT_1_ID) } returns TEST_PATIENT_1
    coEvery { fhirEngine.save<Patient>(any()) } answers {}

    ReflectionHelpers.setField(context, "fhirEngine\$delegate", lazy { fhirEngine })
    coEvery { fhirEngine.save(any()) } answers {}

    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, TEST_PATIENT_1_ID)
      }

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.questionnaireViewModel = questionnaireViewModel
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(questionnaireActivity)
  }

  @Test
  fun testVerifyPrePopulatedQuestionnaire() {
    Thread.sleep(2000)
    questionnaireActivity.supportFragmentManager.executePendingTransactions()
    val response =
      ReflectionHelpers.callInstanceMethod<QuestionnaireResponse>(
        questionnaireActivity,
        "getQuestionnaireResponse"
      )
    // Assert.assertEquals(TEST_PATIENT_1.id, response.find("patient-barcode")?.value.toString())
    Assert.assertEquals(
      TEST_PATIENT_1.name[0].given[0].toString(),
      response.find("PR-name-text")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.name[0].family,
      response.find("PR-name-family")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.birthDate.toString(),
      response.find("patient-0-birth-date")?.valueDateType?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.gender.toCode(),
      response.find("patient-0-gender")?.valueCoding?.code
    )
    Assert.assertEquals(
      TEST_PATIENT_1.telecom[0].value,
      response.find("PR-telecom-value")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.address[0].city,
      response.find("PR-address-city")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.address[0].country,
      response.find("PR-address-country")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.active,
      response.find("PR-active")?.valueBooleanType?.booleanValue()
    )
  }

  @Test
  fun `save-button click should call savedExtractedResources()`() {
    Thread.sleep(2000)
    questionnaireActivity.supportFragmentManager.executePendingTransactions()

    val viewModel = mockk<QuestionnaireViewModel>()
    every { viewModel.extractionProgress } returns MutableLiveData(false)
    every { viewModel.saveExtractedResources(any(), any(), any(), any()) } answers {}
    questionnaireActivity.questionnaireViewModel = viewModel

    questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).performClick()

    verify(exactly = 1) { viewModel.saveExtractedResources(any(), any(), any(), any()) }
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }

  private fun QuestionnaireResponse.find(
    linkId: String
  ): QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent? {
    return item.find(linkId, null)
  }

  private fun List<QuestionnaireResponse.QuestionnaireResponseItemComponent>.find(
    linkId: String,
    default: QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent?
  ): QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent? {
    var result = default
    run loop@{
      forEach {
        if (it.linkId == linkId) {
          result = if (it.answer.isNotEmpty()) it.answer[0] else default
          return@loop
        } else if (it.item.isNotEmpty()) {
          result = it.item.find(linkId, result)
        }
      }
    }

    return result
  }

  @Implements(AlertDialog.Builder::class)
  class CustomBuilder : ShadowBuild() {

    @RealObject private var builder: AlertDialog.Builder? = null

    @Implementation
    fun setView(restId: Int): AlertDialog.Builder? {
      return builder
    }
  }

  companion object {
    const val REGISTER_QUESTIONNAIRE_ID = "sample_patient_registration.json"
    const val TEST_PATIENT_1_ID = "test_patient_1_id"
    val TEST_PATIENT_1 = TestUtils.TEST_PATIENT_1
  }
}
