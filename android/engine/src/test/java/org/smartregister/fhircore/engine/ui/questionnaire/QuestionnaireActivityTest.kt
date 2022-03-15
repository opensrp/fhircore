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
import android.content.Context
import android.content.Intent
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.QuestionnaireFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
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
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString

@HiltAndroidTest
class QuestionnaireActivityTest : ActivityRobolectricTest() {

  private lateinit var questionnaireActivity: QuestionnaireActivity

  private lateinit var intent: Intent

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  val dispatcherProvider: DispatcherProvider = spyk(DefaultDispatcherProvider())

  @BindValue
  val questionnaireViewModel: QuestionnaireViewModel =
    spyk(
      QuestionnaireViewModel(
        fhirEngine = mockk(),
        defaultRepository = mockk(),
        configurationRegistry = mockk(),
        transformSupportServices = mockk(),
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = mockk(),
        libraryEvaluator = mockk()
      )
    )

  @Before
  fun setUp() {
    // TODO Proper set up
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "1234")
      }

    coEvery { questionnaireViewModel.libraryEvaluator.initialize() } just runs

    val questionnaireConfig = QuestionnaireConfig("appId", "form", "title", "form-id")
    coEvery { questionnaireViewModel.getQuestionnaireConfig(any(), any()) } returns
      questionnaireConfig
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns Questionnaire()
    coEvery { questionnaireViewModel.generateQuestionnaireResponse(any(), any()) } returns
      QuestionnaireResponse()

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.supportFragmentManager.executePendingTransactions()
    questionnaireActivity.supportFragmentManager.commitNow {
      add(questionnaireFragment, QUESTIONNAIRE_FRAGMENT_TAG)
    }
  }

  @After
  fun cleanup() {
    unmockkObject(SharedPreferencesHelper)
    unmockkObject(AssetUtil)
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(questionnaireActivity)
  }

  @Test
  fun testIntentArgsShouldInsertValues() {
    val questionnaireResponse = QuestionnaireResponse()
    val patient = Patient().apply { id = "my-patient-id" }
    val populationResources = ArrayList<Resource>()
    populationResources.add(patient)
    val result =
      QuestionnaireActivity.intentArgs(
        "1234",
        "my-form",
        QuestionnaireType.READ_ONLY,
        questionnaireResponse,
        populationResources = populationResources,
        immunizationId = "2323"
      )
    Assert.assertEquals("my-form", result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(
      "1234",
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)
    )
    Assert.assertEquals(
      QuestionnaireType.READ_ONLY.name,
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_TYPE)
    )
    Assert.assertEquals(
      FhirContext.forR4Cached().newJsonParser().encodeResourceToString(questionnaireResponse),
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE)
    )
    Assert.assertEquals(
      "2323",
      result.getString(QuestionnaireActivity.ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY)
    )
    Assert.assertEquals(
      FhirContext.forR4Cached().newJsonParser().encodeResourceToString(patient),
      result.getStringArrayList(QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES)?.get(0)
    )
  }

  @Test
  fun testReadOnlyIntentShouldBeReadToReadOnlyFlag() {
    Assert.assertFalse(questionnaireActivity.questionnaireType.isReadOnly())

    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_TYPE, QuestionnaireType.READ_ONLY.name)
      }

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    Assert.assertTrue(questionnaireActivity.questionnaireType.isReadOnly())
  }

  @Test
  fun testReadOnlyIntentShouldChangeSaveButtonToDone() {
    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_TYPE, QuestionnaireType.READ_ONLY.name)
        putExtra(
          QuestionnaireActivity.QUESTIONNAIRE_RESPONSE,
          QuestionnaireResponse().encodeResourceToString()
        )
      }

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.supportFragmentManager.executePendingTransactions()
    questionnaireActivity.supportFragmentManager.commitNow {
      add(questionnaireFragment, QUESTIONNAIRE_FRAGMENT_TAG)
    }

    Assert.assertEquals(
      "Done",
      questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).text
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
  fun testOnBackPressedShouldCallFinishWhenInReadOnlyMode() {
    val qActivity = spyk(questionnaireActivity)
    ReflectionHelpers.setField(qActivity, "questionnaireType", QuestionnaireType.READ_ONLY)
    qActivity.onBackPressed()

    verify { qActivity.finish() }
  }

  @Test
  fun testHandleQuestionnaireResponseShouldCallExtractAndSaveResources() {
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())

    questionnaireActivity.handleQuestionnaireResponse(QuestionnaireResponse())

    verify { questionnaireViewModel.extractAndSaveResources(any(), any(), any(), any(), any()) }
  }

  @Test
  fun testHandleQuestionnaireSubmitShouldShowProgressAndCallExtractAndSaveResources() {
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())
    questionnaireActivity.handleQuestionnaireSubmit()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.form_progress_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )

    verify(timeout = 2000) {
      questionnaireViewModel.extractAndSaveResources(any(), any(), any(), any(), any())
    }
  }

  @Test
  fun testHandleQuestionnaireSubmitShouldShowErrorAlertOnInvalidData() {
    val questionnaire = buildQuestionnaireWithConstraints()

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)
    questionnaireActivity.handleQuestionnaireSubmit()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_invalid_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )

    verify(inverse = true) {
      questionnaireViewModel.extractAndSaveResources(any(), any(), any(), any(), any())
    }
  }

  @Test
  fun testOnClickSaveButtonShouldShowSubmitConfirmationAlert() {
    ReflectionHelpers.setField(
      questionnaireActivity,
      "questionnaire",
      Questionnaire().apply { experimental = false }
    )

    questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).performClick()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_submit_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  @Test
  fun testOnClickSaveWithExperimentalButtonShouldShowTestOnlyConfirmationAlert() {
    ReflectionHelpers.setField(
      questionnaireActivity,
      "questionnaire",
      Questionnaire().apply { experimental = true }
    )

    questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).performClick()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_test_only_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  @Test
  fun testOnClickEditButtonShouldSetEditModeToTrue() = runBlockingTest {
    val questionnaire = Questionnaire().apply { experimental = false }
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)
    Assert.assertFalse(questionnaireActivity.questionnaireType.isEditMode())

    questionnaireActivity.onClick(questionnaireActivity.findViewById(R.id.btn_edit_qr))

    Assert.assertTrue(questionnaireActivity.questionnaireType.isEditMode())
  }

  @Test
  fun testValidQuestionnaireResponseShouldReturnTrueForValidData() {
    val questionnaire = buildQuestionnaireWithConstraints()

    val questionnaireResponse = QuestionnaireResponse()
    questionnaireResponse.apply {
      addItem().apply {
        addAnswer().apply { this.value = StringType("v1") }
        linkId = "1"
      }
    }

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)

    val result = questionnaireActivity.validQuestionnaireResponse(questionnaireResponse)

    Assert.assertTrue(result)
  }

  @Test
  fun testValidQuestionnaireResponseShouldReturnFalseForInvalidData() {
    val questionnaire = buildQuestionnaireWithConstraints()

    val questionnaireResponse = QuestionnaireResponse()
    questionnaireResponse.apply { addItem().apply { linkId = "1" } }

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)

    val result = questionnaireActivity.validQuestionnaireResponse(questionnaireResponse)

    Assert.assertFalse(result)
  }

  @Test
  fun testPostSaveSuccessfulShouldFinishActivity() {
    questionnaireActivity.postSaveSuccessful(QuestionnaireResponse())

    Assert.assertTrue(questionnaireActivity.isFinishing)
  }

  @Test
  fun testPostSaveSuccessfulWithExtractionMessageShouldShowAlert() {
    questionnaireActivity.questionnaireViewModel.extractionProgressMessage.postValue("ABC")
    questionnaireActivity.postSaveSuccessful(QuestionnaireResponse())

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals("ABC", alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text)
  }

  @Test
  fun onOptionsItemSelectedShouldCallOnBackPressedWhenHomeIsPressed() {
    val spiedActivity = spyk(questionnaireActivity)
    val menuItem = mockk<MenuItem>()
    every { menuItem.itemId } returns android.R.id.home
    every { spiedActivity.onBackPressed() } just runs

    Assert.assertTrue(spiedActivity.onOptionsItemSelected(menuItem))

    verify { spiedActivity.onBackPressed() }
  }

  @Test
  fun onPostSaveShouldCallPostSaveSuccessfulWhenResultIsTrue() {
    val spiedActivity = spyk(questionnaireActivity)
    val qr = mockk<QuestionnaireResponse>()
    every { qr.copy() } returns qr
    every { spiedActivity.postSaveSuccessful(qr) } just runs

    spiedActivity.onPostSave(true, qr)

    verify { spiedActivity.postSaveSuccessful(qr) }
    verify { spiedActivity.dismissSaveProcessing() }
  }

  private fun buildQuestionnaireWithConstraints(): Questionnaire {
    return Questionnaire().apply {
      addItem().apply {
        required = true
        linkId = "1"
        type = Questionnaire.QuestionnaireItemType.STRING
      }
      addItem().apply {
        maxLength = 4
        linkId = "2"
        type = Questionnaire.QuestionnaireItemType.STRING
      }
    }
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }
}
