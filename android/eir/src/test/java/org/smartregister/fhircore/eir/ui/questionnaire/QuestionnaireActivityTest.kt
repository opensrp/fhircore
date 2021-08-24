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

package org.smartregister.fhircore.eir.ui.questionnaire

import android.app.Activity
import android.content.Intent
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModelLazy
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
<<<<<<< HEAD:android/eir/src/test/java/org/smartregister/fhircore/eir/ui/questionnaire/QuestionnaireActivityTest.kt
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.shadow.TestUtils
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsActivity
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_BARCODE_KEY
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR

@Config(shadows = [EirApplicationShadow::class])
=======
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_BARCODE_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.shadow.TestUtils
import org.smartregister.fhircore.util.QuestionnaireUtils
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel

@Config(shadows = [FhirApplicationShadow::class])
class QuestionnaireActivityTest : ActivityRobolectricTest() {
  private lateinit var context: EirApplication
  private lateinit var questionnaireActivity: QuestionnaireActivity
  private lateinit var intent: Intent

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    val samplePatientRegisterQuestionnaire =
      TestUtils.loadQuestionnaire(context, REGISTER_QUESTIONNAIRE_ID)

    val fhirEngine = spyk(FhirApplication.fhirEngine(context))

    mockkObject(FhirApplication)
    every { FhirApplication.fhirEngine(any()) } returns fhirEngine

    // val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.load(Questionnaire::class.java, REGISTER_QUESTIONNAIRE_ID) } returns
      samplePatientRegisterQuestionnaire
    coEvery { fhirEngine.load(Patient::class.java, TEST_PATIENT_1_ID) } returns TEST_PATIENT_1
    coEvery { fhirEngine.save<Patient>(any()) } answers {}

    ReflectionHelpers.setField(context, "fhirEngine\$delegate", lazy { fhirEngine })
    coEvery { fhirEngine.save(any()) } answers {}

    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY, REGISTER_QUESTIONNAIRE_ID)
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, TEST_PATIENT_1_ID)
      }

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = spyk(controller.get())

    controller.create().resume()
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(questionnaireActivity)
  }

  @Test
  fun testActivityShouldSetPreAssignedId() {

    val response =
      ReflectionHelpers.callInstanceMethod<QuestionnaireResponse>(
        questionnaireActivity,
        "getQuestionnaireResponse"
      )
    // Assert.assertEquals("test-id", response.find("patient-barcode")?.value.toString())

    val barcode = QuestionnaireUtils.valueStringWithLinkId(response, QUESTIONNAIRE_ARG_BARCODE_KEY)
    Assert.assertEquals(barcode, response.find("patient-barcode")?.value.toString())
  }

  @Test
  fun testVerifyPrePopulatedQuestionnaire() {

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
  @Ignore("Fails automated execution but works locally") // TODO Investigate why test fails
  fun testVerifyPatientResourceSaved() {
    questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).performClick()

    val expectedIntent = Intent(questionnaireActivity, PatientDetailsActivity::class.java)
    val actualIntent = shadowOf(context).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Ignore
  @Test
  fun `save-button click should call savedExtractedResources()`() {
    every { questionnaireActivity.saveExtractedResources(any()) } just runs

    questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).performClick()

    verify(exactly = 1) { questionnaireActivity.findViewById<Button>(any()) }
    verify(exactly = 1) { questionnaireActivity.finish() }
    verify(exactly = 1) { questionnaireActivity.saveExtractedResources(any()) }
  }

  @Test
  fun `saveExtractedResources() should call viewModel#saveExtractedResources`() {
    val viewModel = spyViewModel()

    // questionnaire and response must map
    viewModel.questionnaire.item.clear()
    viewModel.questionnaire.addItem().linkId = "test_field_i"

    val questionnaireResponse = QuestionnaireResponse()
    questionnaireResponse.addItem().linkId = "test_field_i"

    // todo app temporarily bypass it so enable
    questionnaireActivity.intent.removeExtra(QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR)

    questionnaireActivity.saveExtractedResources(questionnaireResponse)

    verify(exactly = 1) {
      viewModel.saveExtractedResources(any(), intent, any(), questionnaireResponse)
    }
    verify(exactly = 1) { viewModel.saveBundleResources(any(), any()) }
    verify { questionnaireActivity.finish() }
  }

  @Test
  fun `saveExtractedResources() should call viewModel#saveParsedResource`() {
    val viewModel = spyViewModel()

    // questionnaire and response must map
    viewModel.questionnaire.item.clear()
    viewModel.questionnaire.addItem().linkId = "test_field_i"
    viewModel.questionnaire.addSubjectType("Patient")

    val questionnaireResponse = QuestionnaireResponse()
    questionnaireResponse.addItem().linkId = "test_field_i"

    questionnaireActivity.saveExtractedResources(questionnaireResponse)

    verifyOrder {
      viewModel.saveExtractedResources(any(), intent, any(), questionnaireResponse)
      viewModel.saveParsedResource(any(), any())
    }
    verify(inverse = true) { viewModel.saveBundleResources(any()) }
    verify { questionnaireActivity.finish() }
  }

  private fun spyViewModel(): QuestionnaireViewModel {
    val viewModel =
      spyk(
        ReflectionHelpers.getField<ViewModelLazy<QuestionnaireViewModel>>(
            questionnaireActivity,
            "viewModel\$delegate"
          )
          .value
      )
    ReflectionHelpers.setField(questionnaireActivity, "viewModel\$delegate", lazy { viewModel })
    return viewModel
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

  companion object {
    const val REGISTER_QUESTIONNAIRE_ID = "sample_patient_registration.json"
    const val TEST_PATIENT_1_ID = "test_patient_1_id"
    val TEST_PATIENT_1 = TestUtils.TEST_PATIENT_1
  }
}
