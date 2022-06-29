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

package org.smartregister.fhircore.anc.ui.details.bmicompute

import android.app.Activity
import android.content.Intent
import android.text.SpannableString
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
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
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertIntent
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@ExperimentalCoroutinesApi
@HiltAndroidTest
class BmiQuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private lateinit var bmiQuestionnaireActivity: BmiQuestionnaireActivity
  private lateinit var bmiQuestionnaireActivitySpy: BmiQuestionnaireActivity
  private lateinit var realFhirEngine: FhirEngine
  val fhirEngine: FhirEngine = mockk()
  @BindValue val patientRepository: PatientRepository = mockk()

  @BindValue
  val bmiQuestionnaireViewModel =
    spyk(
      BmiQuestionnaireViewModel(
        fhirEngine = fhirEngine,
        defaultRepository = mockk(),
        configurationRegistry = mockk(),
        transformSupportServices = mockk(),
        dispatcherProvider = DefaultDispatcherProvider(),
        sharedPreferencesHelper = mockk(),
        libraryEvaluator = mockk()
      )
    )

  @Before
  fun setUp() {
    hiltRule.inject()
    mockkObject(Sync)

    realFhirEngine = FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext())

    coEvery { fhirEngine.get(ResourceType.Patient, "Patient/1") } returns Patient()
    coEvery { fhirEngine.search<Immunization>(any()) } returns listOf()
    coEvery { fhirEngine.get(ResourceType.Questionnaire, any()) } returns Questionnaire()
    coEvery { fhirEngine.get(ResourceType.Immunization, any()) } returns Immunization()
    coEvery { fhirEngine.create(any()) } returns listOf()

    val intent =
      Intent().apply {
        putExtra(QUESTIONNAIRE_ARG_FORM, "family-patient_bmi_compute")
        putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, "Patient/1")
      }

    coEvery { bmiQuestionnaireViewModel.loadQuestionnaire(any(), any()) } returns Questionnaire()
    coEvery { bmiQuestionnaireViewModel.loadQuestionnaire(any(), any()) } returns Questionnaire()
    // coEvery { bmiQuestionnaireViewModel.libraryEvaluator.initialize() } just runs
    val questionnaireConfig = QuestionnaireConfig("appId", "form", "title", "form-id")
    coEvery { bmiQuestionnaireViewModel.getQuestionnaireConfig(any(), any()) } returns
      questionnaireConfig
    coEvery { bmiQuestionnaireViewModel.generateQuestionnaireResponse(any(), any()) } returns
      QuestionnaireResponse()

    every { Sync.basicSyncJob(any()).stateFlow() } returns flowOf()
    every { Sync.basicSyncJob(any()).lastSyncTimestamp() } returns OffsetDateTime.now()
    every { patientRepository.setAncItemMapperType(any()) } returns Unit

    ReflectionHelpers.setStaticField(FhirEngineProvider::class.java, "fhirEngine", fhirEngine)

    bmiQuestionnaireActivity =
      Robolectric.buildActivity(BmiQuestionnaireActivity::class.java, intent).create().get()
    bmiQuestionnaireActivitySpy = spyk(bmiQuestionnaireActivity, recordPrivateCalls = true)

    every { bmiQuestionnaireActivitySpy.finish() } returns Unit
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
    ReflectionHelpers.setStaticField(FhirEngineProvider::class.java, "fhirEngine", realFhirEngine)
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
  fun testExitFormShouldCallFinishMethod() {
    ReflectionHelpers.callInstanceMethod<Any>(bmiQuestionnaireActivitySpy, "exitForm")
    verify(exactly = 1) { bmiQuestionnaireActivitySpy.finish() }
  }

  @Test
  fun handleQuestionnaireResponseShouldInvokeShowBmiDataAlert() {
    val questionnaireViewModel = mockk<BmiQuestionnaireViewModel>()
    val questionnaire = Questionnaire()

    ReflectionHelpers.setField(bmiQuestionnaireActivitySpy, "questionnaire", questionnaire)
    every { bmiQuestionnaireActivitySpy.questionnaireViewModel } returns questionnaireViewModel

    val bmi = 20.0

    val bundle =
      Bundle().apply {
        addEntry(Bundle.BundleEntryComponent().setResource(Encounter()))
        addEntry(Bundle.BundleEntryComponent().setResource(Observation()))
        addEntry(Bundle.BundleEntryComponent().setResource(Observation()))
        addEntry(
          Bundle.BundleEntryComponent().setResource(Observation().apply { value = Quantity(bmi) })
        )
      }
    coEvery { questionnaireViewModel.performExtraction(any(), any(), any()) } returns bundle
    every { bmiQuestionnaireActivitySpy.getString(any()) } answers
      {
        (ApplicationProvider.getApplicationContext<HiltTestApplication>().getString(firstArg()))
      }

    val qr = getInvalidQuestionnaireResponse()

    // Run the method under test
    bmiQuestionnaireActivitySpy.handleQuestionnaireResponse(qr)

    verify {
      bmiQuestionnaireActivitySpy invoke
        "showBmiDataAlert" withArguments
        listOf<Any?>("Patient/1", qr, bmi)
    }
  }

  @Test
  fun handleQuestionnaireResponseShouldInvokeShowErrorAlertWhenGeneratedBmiIsLessThan0() =
      runBlocking {
    val questionnaireViewModel = mockk<BmiQuestionnaireViewModel>()
    val questionnaire = Questionnaire()

    ReflectionHelpers.setField(bmiQuestionnaireActivitySpy, "questionnaire", questionnaire)
    every { bmiQuestionnaireActivitySpy.questionnaireViewModel } returns questionnaireViewModel

    val bundle =
      Bundle().apply {
        addEntry(Bundle.BundleEntryComponent().setResource(Encounter()))
        addEntry(Bundle.BundleEntryComponent().setResource(Observation()))
        addEntry(Bundle.BundleEntryComponent().setResource(Observation()))
        addEntry(
          Bundle.BundleEntryComponent().setResource(Observation().apply { value = Quantity(-1) })
        )
      }
    coEvery { questionnaireViewModel.performExtraction(any(), any(), any()) } returns bundle
    every { bmiQuestionnaireActivitySpy.getString(any()) } answers
      {
        (ApplicationProvider.getApplicationContext<HiltTestApplication>().getString(firstArg()))
      }

    // Run the method under test
    bmiQuestionnaireActivitySpy.handleQuestionnaireResponse(getInvalidQuestionnaireResponse())

    verify {
      bmiQuestionnaireActivitySpy invoke
        "showErrorAlert" withArguments
        listOf("Try again", "Error encountered cannot save form")
    }
  }

  @Test
  fun resumeFormShouldCallDismissSaveProcessing() {
    ReflectionHelpers.callInstanceMethod<Void>(bmiQuestionnaireActivitySpy, "resumeForm")

    verify { bmiQuestionnaireActivitySpy invoke "dismissSaveProcessing" withArguments listOf() }
  }

  @Test
  fun showErrorAlertShouldShowDialogWithErrorTitleAndMessage() {
    mockkObject(AlertDialogue)

    every {
      AlertDialogue.showAlert(
        context = any(),
        alertIntent = any(),
        message = any(),
        title = any(),
        confirmButtonListener = any(),
        confirmButtonText = any(),
        neutralButtonText = any()
      )
    } returns mockk()

    ReflectionHelpers.callInstanceMethod<Void>(
      bmiQuestionnaireActivitySpy,
      "showErrorAlert",
      ReflectionHelpers.ClassParameter.from(String::class.java, "Error title"),
      ReflectionHelpers.ClassParameter.from(String::class.java, "Error message")
    )

    verify {
      AlertDialogue.showAlert(
        context = bmiQuestionnaireActivitySpy,
        alertIntent = AlertIntent.ERROR,
        message = "Error message",
        title = "Error title",
        confirmButtonListener = any(),
        confirmButtonText = android.R.string.ok,
        neutralButtonText = android.R.string.cancel
      )
    }

    unmockkObject(AlertDialogue)
  }

  @Test
  fun showBmiDataAlertShouldShowBmiDialog() {
    mockkObject(AlertDialogue)
    val qr = QuestionnaireResponse()

    every { bmiQuestionnaireActivitySpy.getString(R.string.your_bmi) } returns "Your BMI ="

    every {
      AlertDialogue.showAlert(
        context = any(),
        alertIntent = any(),
        message = any(),
        title = any(),
        confirmButtonListener = any(),
        confirmButtonText = any(),
        neutralButtonListener = any(),
        neutralButtonText = any()
      )
    } returns mockk()

    every { bmiQuestionnaireViewModel.getBmiResult(any(), any()) } returns
      SpannableString("Bmi result")

    ReflectionHelpers.callInstanceMethod<Void>(
      bmiQuestionnaireActivitySpy,
      "showBmiDataAlert",
      ReflectionHelpers.ClassParameter.from(String::class.java, "patient-id"),
      ReflectionHelpers.ClassParameter.from(QuestionnaireResponse::class.java, qr),
      ReflectionHelpers.ClassParameter.from(Double::class.java, 20.0)
    )

    verify {
      AlertDialogue.showAlert(
        context = bmiQuestionnaireActivitySpy,
        alertIntent = AlertIntent.INFO,
        message = any(),
        title = "Your BMI = 20.0",
        confirmButtonListener = any(),
        confirmButtonText = R.string.str_save,
        neutralButtonText = R.string.re_compute,
        neutralButtonListener = any()
      )
    }

    unmockkObject(AlertDialogue)
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
