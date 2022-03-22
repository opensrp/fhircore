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

package org.smartregister.fhircore.eir.ui.vaccine

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.data.model.PatientVaccineSummary
import org.smartregister.fhircore.eir.shadow.TestUtils
import org.smartregister.fhircore.eir.ui.patient.details.nextDueDateFmt
import org.smartregister.fhircore.eir.util.RECORD_VACCINE_FORM
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy

@ExperimentalCoroutinesApi
@HiltAndroidTest
class RecordVaccineActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) var coroutineTestRule = CoroutineTestRule()

  val fhirEngine: FhirEngine = mockk()

  private lateinit var recordVaccineActivity: RecordVaccineActivity

  @BindValue lateinit var recordVaccineViewModel: RecordVaccineViewModel

  @Before
  fun setUp() {
    recordVaccineViewModel =
      spyk(
        RecordVaccineViewModel(
          fhirEngine = fhirEngine,
          defaultRepository = mockk(),
          configurationRegistry = mockk(),
          transformSupportServices = mockk(),
          patientRepository = mockk(),
          dispatcherProvider = DefaultDispatcherProvider(),
          sharedPreferencesHelper = mockk(),
          libraryEvaluator = mockk()
        )
      )

    hiltRule.inject()

    coEvery { fhirEngine.load(Patient::class.java, "test_patient_id") } returns
      TestUtils.TEST_PATIENT_1
    coEvery { fhirEngine.search<Immunization>(any()) } returns listOf()
    coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns Questionnaire()
    coEvery { fhirEngine.load(Immunization::class.java, any()) } returns Immunization()
    coEvery { fhirEngine.save(any()) } answers {}

    val intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "test_patient_id")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, RECORD_VACCINE_FORM)
      }

    coEvery { recordVaccineViewModel.loadQuestionnaire(any(), any()) } returns mockk()
    val questionnaireConfig = QuestionnaireConfig("appId", "form", "title", "form-id")
    coEvery { recordVaccineViewModel.getQuestionnaireConfig(any(), any()) } returns
      questionnaireConfig
    recordVaccineActivity =
      Robolectric.buildActivity(RecordVaccineActivity::class.java, intent).create().resume().get()
  }

  @Test
  fun testHandleQuestionnaireResponseWithDose2ShouldShowAlert() = runBlockingTest {
    coEvery { recordVaccineViewModel.performExtraction(any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { resource = getImmunization() } }

    coEvery { recordVaccineViewModel.loadLatestVaccine(any()) } returns
      PatientVaccineSummary(2, "vaccine")

    recordVaccineActivity.handleQuestionnaireResponse(mockk())

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals(
      getString(R.string.already_fully_vaccinated),
      dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  @Test
  fun testHandleQuestionnaireResponseShouldCallSaveBundleResources() = runBlockingTest {
    coEvery { recordVaccineViewModel.performExtraction(any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { resource = getImmunization() } }

    coEvery { recordVaccineViewModel.loadLatestVaccine(any()) } returns
      PatientVaccineSummary(1, "vaccineA")

    recordVaccineActivity.handleQuestionnaireResponse(mockk())

    coVerify { recordVaccineViewModel.saveBundleResources(any()) }
  }

  @Test
  fun testPostSaveSuccessfulWithDose2ShouldShowAlertWithFullyImmunized() = runBlockingTest {
    val savedImmunization =
      getImmunization().apply { protocolAppliedFirstRep.doseNumber = PositiveIntType(2) }

    ReflectionHelpers.setField(recordVaccineActivity, "savedImmunization", savedImmunization)

    recordVaccineActivity.postSaveSuccessful(QuestionnaireResponse())

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("vaccineA 2nd dose recorded", dialog.title)
    Assert.assertEquals(
      getString(org.smartregister.fhircore.eir.R.string.fully_vaccinated),
      dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  @Test
  fun testPostSaveSuccessfulWithDose1ShouldShowAlertWithFullyImmunized() = runBlockingTest {
    val savedImmunization =
      getImmunization().apply { protocolAppliedFirstRep.doseNumber = PositiveIntType(1) }

    ReflectionHelpers.setField(recordVaccineActivity, "savedImmunization", savedImmunization)

    recordVaccineActivity.postSaveSuccessful(QuestionnaireResponse())

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("vaccineA 1st dose recorded", dialog.title)
    Assert.assertEquals(
      "Dose 2 due ${savedImmunization.nextDueDateFmt()}",
      dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  @Test
  fun testVerifyRecordedVaccineSavedDialogProperty() = runBlockingTest {
    coEvery { recordVaccineViewModel.performExtraction(any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { resource = getImmunization() } }

    coEvery { recordVaccineViewModel.loadLatestVaccine(any()) } returns
      PatientVaccineSummary(1, "vaccine")

    recordVaccineActivity.handleQuestionnaireResponse(mockk())

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("Initially received vaccine", dialog.title)
    Assert.assertEquals(
      "Second vaccine dose should be same as first",
      dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  @Test
  fun testSanitizeExtractedDataWithFirstDoseShouldSetRelevantProperties() {
    val immunization = Immunization()
    recordVaccineActivity.sanitizeExtractedData(immunization, null)

    Assert.assertEquals(1, immunization.protocolAppliedFirstRep.doseNumberPositiveIntType.value)
    Assert.assertEquals(
      Date().asDdMmmYyyy(),
      immunization.occurrenceDateTimeType.value.asDdMmmYyyy()
    )
    Assert.assertEquals(Immunization.ImmunizationStatus.COMPLETED, immunization.status)
    Assert.assertEquals("Patient/test_patient_id", immunization.patient.reference)
  }

  @Test
  fun testShowVaccineRecordDialogShouldShowNextDue() {
    coEvery { recordVaccineViewModel.performExtraction(any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { resource = getImmunization() } }

    coEvery { recordVaccineViewModel.loadLatestVaccine(any()) } returns
      PatientVaccineSummary(1, "vaccineA")

    val immunization = getImmunization()

    recordVaccineActivity.handleQuestionnaireResponse(mockk())

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    // TODO Perform assertions for both progress and the vaccination dialog.
    //    Assert.assertEquals("vaccineA 1st dose recorded", dialog.title)
    //    Assert.assertEquals(
    //      "Dose 2 due ${immunization.nextDueDateFmt()}",
    //      dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text
    //    )
  }

  @Test
  fun testShowVaccineRecordDialogShouldShowFullyVaccinated() {

    coEvery { recordVaccineViewModel.performExtraction(any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { resource = getImmunization() } }

    coEvery { recordVaccineViewModel.loadLatestVaccine(any()) } returns
      PatientVaccineSummary(2, "vaccineA")

    recordVaccineActivity.handleQuestionnaireResponse(mockk())

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("", dialog.title)
    Assert.assertEquals(
      "Can not receive another dose. Already fully vaccinated",
      dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  private fun getImmunization(): Immunization {
    return Immunization().apply {
      recorded = SimpleDateFormat("yyyy-MM-dd").parse("2021-09-16")
      vaccineCode =
        CodeableConcept().apply {
          this.text = "vaccineA"
          this.coding = listOf(Coding("", "vaccineA", "vaccineA"))
        }
      occurrence = DateTimeType.now()

      addProtocolApplied().doseNumber = PositiveIntType(1)
    }
  }

  override fun getActivity(): Activity {
    return recordVaccineActivity
  }
}
