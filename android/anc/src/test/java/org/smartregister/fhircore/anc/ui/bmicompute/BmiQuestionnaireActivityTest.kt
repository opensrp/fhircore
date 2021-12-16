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

package org.smartregister.fhircore.anc.ui.bmicompute

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.time.OffsetDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.details.bmicompute.BmiQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.details.bmicompute.BmiQuestionnaireViewModel
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class BmiQuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private lateinit var bmiQuestionnaireActivity: BmiQuestionnaireActivity
  private lateinit var bmiQuestionnaireActivitySpy: BmiQuestionnaireActivity
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
  @BindValue val patientRepository: PatientRepository = mockk()

  @Before
  fun setUp() {

    hiltRule.inject()
    mockkObject(Sync)

    every { Sync.basicSyncJob(any()).stateFlow() } returns flowOf()
    every { Sync.basicSyncJob(any()).lastSyncTimestamp() } returns OffsetDateTime.now()
    every { patientRepository.setAncItemMapperType(any()) } returns Unit

    val intent =
      Intent().apply {
        putExtra(QUESTIONNAIRE_ARG_FORM, "family-patient_bmi_compute")
        putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, "Patient/1")
      }

    bmiQuestionnaireActivity =
      Robolectric.buildActivity(BmiQuestionnaireActivity::class.java, intent).create().get()
    bmiQuestionnaireActivitySpy = spyk(bmiQuestionnaireActivity, recordPrivateCalls = true)

    every { bmiQuestionnaireActivitySpy.finish() } returns Unit
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testActivityShouldNotNull() {
    assertNotNull(getActivity())
  }

  @Test
  fun testOnClickShouldCallHandleQuestionnaireSubmitMethod() {
    every { bmiQuestionnaireActivitySpy.handleQuestionnaireSubmit() } returns Unit

    val view =
      mockk<View> {
        every { id } returns org.smartregister.fhircore.engine.R.id.btn_save_client_info
      }

    bmiQuestionnaireActivitySpy.onClick(view)
    verify(exactly = 1) { bmiQuestionnaireActivitySpy.handleQuestionnaireSubmit() }
  }

  @Test
  fun testHandleQuestionnaireResponseShouldCorrectShowAlertDialog() {

    // handle invalid questionnaire response scenario
    bmiQuestionnaireActivity.handleQuestionnaireResponse(getInvalidQuestionnaireResponse())
    var dialog = ShadowAlertDialog.getLatestDialog() as AlertDialog

    assertNotNull(dialog)
    assertEquals(
      bmiQuestionnaireActivity.getString(R.string.try_again),
      dialog.findViewById<TextView>(R.id.alertTitle)?.text
    )
    assertEquals(
      bmiQuestionnaireActivity.getString(R.string.error_saving_form),
      dialog.findViewById<TextView>(android.R.id.message)?.text
    )

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

    // handle valid questionnaire response scenario
    bmiQuestionnaireActivity.handleQuestionnaireResponse(getQuestionnaireResponse())
    dialog = ShadowAlertDialog.getLatestDialog() as AlertDialog

    assertNotNull(dialog)
    // TODO FIX BMI RESULT @ABDUL WAHAB
    //    assertEquals(
    //      bmiQuestionnaireActivity.getString(org.smartregister.fhircore.anc.R.string.your_bmi) +
    //        " 19.04",
    //      dialog.findViewById<TextView>(R.id.alertTitle)?.text
    //    )

    coEvery {
      patientRepository.recordComputedBmi(any(), any(), any(), any(), any(), any(), any(), any())
    } returns true
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
  }

  @Test
  fun testExitFormShouldCallFinishMethod() {
    ReflectionHelpers.callInstanceMethod<Any>(bmiQuestionnaireActivitySpy, "exitForm")
    verify(exactly = 1) { bmiQuestionnaireActivitySpy.finish() }
  }

  @Test
  fun testProceedRecordBMIShouldSaveData() {
    every { bmiQuestionnaireActivitySpy getProperty "questionnaire" } returns mockk<Questionnaire>()
    coEvery {
      patientRepository.recordComputedBmi(any(), any(), any(), any(), any(), any(), any(), any())
    } returns true

    ReflectionHelpers.callInstanceMethod<Any>(
      bmiQuestionnaireActivitySpy,
      "proceedRecordBMI",
      ReflectionHelpers.ClassParameter(
        QuestionnaireResponse::class.java,
        getQuestionnaireResponse()
      ),
      ReflectionHelpers.ClassParameter(String::class.java, "Patient/1"),
      ReflectionHelpers.ClassParameter(Double::class.java, 5),
      ReflectionHelpers.ClassParameter(Double::class.java, 5),
      ReflectionHelpers.ClassParameter(Double::class.java, 19),
      ReflectionHelpers.ClassParameter(Boolean::class.java, true)
    )

    verify(exactly = 1) { bmiQuestionnaireActivitySpy.finish() }

    coEvery {
      patientRepository.recordComputedBmi(any(), any(), any(), any(), any(), any(), any(), any())
    } returns false
    every { bmiQuestionnaireActivitySpy.getString(any()) } returns ""
    every { bmiQuestionnaireActivitySpy["showErrorAlert"](any<String>(), any<String>()) }

    ReflectionHelpers.callInstanceMethod<Any>(
      bmiQuestionnaireActivitySpy,
      "proceedRecordBMI",
      ReflectionHelpers.ClassParameter(
        QuestionnaireResponse::class.java,
        getQuestionnaireResponse()
      ),
      ReflectionHelpers.ClassParameter(String::class.java, "Patient/1"),
      ReflectionHelpers.ClassParameter(Double::class.java, 5),
      ReflectionHelpers.ClassParameter(Double::class.java, 5),
      ReflectionHelpers.ClassParameter(Double::class.java, 19),
      ReflectionHelpers.ClassParameter(Boolean::class.java, true)
    )

    verify(exactly = 1) {
      bmiQuestionnaireActivitySpy["showErrorAlert"](any<String>(), any<String>())
    }
  }

  private fun getQuestionnaireResponse(): QuestionnaireResponse {
    return QuestionnaireResponse().apply {

      // add metric unit item and answer
      addItem().apply {
        linkId = BmiQuestionnaireViewModel.KEY_UNIT_SELECTION
        addAnswer().apply { value = Coding().apply { code = "metric" } }
      }

      // add height item and answer
      addItem().apply {
        linkId = BmiQuestionnaireViewModel.KEY_HEIGHT_CM
        addAnswer().apply { value = DecimalType(170) }
      }

      // add weight item and answer
      addItem().apply {
        linkId = BmiQuestionnaireViewModel.KEY_WEIGHT_KG
        addAnswer().apply { value = DecimalType(55) }
      }
    }
  }

  private fun getInvalidQuestionnaireResponse(): QuestionnaireResponse {
    return QuestionnaireResponse().apply {

      // add metric unit item and answer
      addItem().apply {
        linkId = BmiQuestionnaireViewModel.KEY_UNIT_SELECTION
        addAnswer().apply { value = Coding().apply { code = "metric" } }
      }

      // add height item
      addItem().apply { linkId = BmiQuestionnaireViewModel.KEY_HEIGHT_CM }

      // add weight item
      addItem().apply { linkId = BmiQuestionnaireViewModel.KEY_WEIGHT_KG }
    }
  }

  override fun getActivity(): Activity {
    return bmiQuestionnaireActivity
  }
}
