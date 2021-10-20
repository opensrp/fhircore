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
import android.app.Application
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.datacapture.QuestionnaireFragment
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import org.smartregister.fhircore.engine.util.FormConfigUtil
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class QuestionnaireActivityTest : ActivityRobolectricTest() {
  private lateinit var context: Application
  private lateinit var questionnaireActivity: QuestionnaireActivity
  private lateinit var questionnaireViewModel: QuestionnaireViewModel
  private lateinit var intent: Intent

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @Before
  fun setUp() {
    mockkObject(SharedPreferencesHelper)
    every { SharedPreferencesHelper.read(any(), any()) } returns ""

    mockkObject(FormConfigUtil)
    every { FormConfigUtil.loadConfig(any(), any()) } returns
      listOf(QuestionnaireConfig("patient-registration", "Patient registration", "1234567"))

    context = ApplicationProvider.getApplicationContext()
    questionnaireViewModel = spyk(QuestionnaireViewModel(context))

    coEvery { questionnaireViewModel.loadQuestionnaire("1234567") } returns
      Questionnaire().apply { id = "1234567" }

    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
      }

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.questionnaireViewModel = questionnaireViewModel
    questionnaireActivity.supportFragmentManager.commitNow {
      add(questionnaireFragment, QUESTIONNAIRE_FRAGMENT_TAG)
    }
  }

  @After
  fun cleanup() {
    unmockkObject(SharedPreferencesHelper)
    unmockkObject(FormConfigUtil)
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(questionnaireActivity)
  }

  @Test
  fun testRequiredIntentShouldInsertValues() {
    val result = QuestionnaireActivity.requiredIntentArgs("1234", "my-form")
    Assert.assertEquals("my-form", result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(
      "1234",
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)
    )
  }

  @Test
  fun testOnBackPressedShouldShowAlert() {
    questionnaireActivity.onBackPressed()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_back_pressed_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
    Assert.assertEquals(
      getString(R.string.questionnaire_alert_back_pressed_button_title),
      alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text
    )
  }

  @Test
  fun testHandleQuestionnaireResponseShouldCallExtractAndSaveResources() {
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())

    questionnaireActivity.handleQuestionnaireResponse(QuestionnaireResponse())

    verify { questionnaireViewModel.extractAndSaveResources(any(), any(), any(), any()) }
  }

  @Test
  fun testHandleQuestionnaireSubmitShouldShowProgressAndCallExtractAndSaveResources() {
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())
    questionnaireActivity.handleQuestionnaireSubmit()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.saving_registration),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )

    verify(timeout = 2000) {
      questionnaireViewModel.extractAndSaveResources(any(), any(), any(), any())
    }
  }

  @Test
  fun testOnClickSaveButtonShouldShowSubmitConfirmationAlert() {
    questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).performClick()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_submit_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }
}
