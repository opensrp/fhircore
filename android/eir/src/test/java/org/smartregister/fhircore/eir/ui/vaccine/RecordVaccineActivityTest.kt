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
import android.content.DialogInterface
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import java.text.SimpleDateFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
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
import org.smartregister.fhircore.eir.util.RECORD_VACCINE_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.DateUtils

@ExperimentalCoroutinesApi
@Ignore("Fix tests failing when run with others")
class RecordVaccineActivityTest : ActivityRobolectricTest() {

  private lateinit var recordVaccineActivity: RecordVaccineActivity

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.load(Patient::class.java, "test_patient_id") } returns
      TestUtils.TEST_PATIENT_1

    coEvery { fhirEngine.search<Immunization>(any()) } returns listOf()
    coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns Questionnaire()
    ReflectionHelpers.setField(
      ApplicationProvider.getApplicationContext(),
      "fhirEngine\$delegate",
      lazy { fhirEngine }
    )

    val intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "test_patient_id")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, RECORD_VACCINE_FORM)
      }

    recordVaccineActivity =
      Robolectric.buildActivity(RecordVaccineActivity::class.java, intent).create().resume().get()
  }

  @Test
  fun testVerifyRecordedVaccineSavedDialogProperty() = runBlockingTest {
    val spyViewModel =
      spyk((recordVaccineActivity.recordVaccineViewModel as RecordVaccineViewModel))
    recordVaccineActivity.recordVaccineViewModel = spyViewModel

    val callback = slot<Observer<PatientVaccineSummary>>()

    coEvery { spyViewModel.performExtraction(any(), any(), any()) } returns
      Bundle().apply { addEntry().apply { resource = getImmunization() } }

    every { spyViewModel.getVaccineSummary(any()) } returns
      mockk<LiveData<PatientVaccineSummary>>().apply {
        every { observe(any(), capture(callback)) } answers
          {
            callback.captured.onChanged(PatientVaccineSummary(1, "vaccine"))
          }
      }

    recordVaccineActivity.handleQuestionnaireResponse(mockk())

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("Initially received vaccine", dialog.title)
    Assert.assertEquals("Second vaccine dose should be same as first", dialog.message)
  }

  @Test
  fun testShowVaccineRecordDialogVerifyAllOptions() {
    runBlocking {
      var vaccineSummary = patientVaccineSummaryOf(1, "vaccineA")

      val immunization = getImmunization()

      val vaccineDate = immunization.occurrenceDateTimeType.toHumanDisplay()
      val nextVaccineDate =
        DateUtils.addDays(vaccineDate, 28, dateTimeFormat = "MMM d, yyyy h:mm:ss a")
      val bundle = Bundle().apply { addEntry().apply { resource = immunization } }

      ReflectionHelpers.callInstanceMethod<Any>(
        recordVaccineActivity,
        "showVaccineRecordDialog",
        ReflectionHelpers.ClassParameter.from(Bundle::class.java, bundle),
        ReflectionHelpers.ClassParameter.from(PatientVaccineSummary::class.java, vaccineSummary)
      )

      val shadowAlertDialog = ShadowAlertDialog.getLatestAlertDialog()
      var dialog = shadowOf(shadowAlertDialog)

      Assert.assertNotNull(dialog)
      Assert.assertEquals("vaccineA 1st dose recorded", dialog.title)
      Assert.assertEquals("Dose 2 due $nextVaccineDate", dialog.message)

      shadowAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
      immunization.protocolApplied[0].doseNumber = PositiveIntType(2)

      ReflectionHelpers.callInstanceMethod<Any>(
        recordVaccineActivity,
        "showVaccineRecordDialog",
        ReflectionHelpers.ClassParameter.from(Bundle::class.java, bundle),
        ReflectionHelpers.ClassParameter.from(PatientVaccineSummary::class.java, vaccineSummary)
      )
      dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

      Assert.assertNotNull(dialog)
      Assert.assertEquals("vaccineA 1st dose recorded", dialog.title)
      Assert.assertEquals("Fully vaccinated", dialog.message)

      immunization.vaccineCode.coding[0].code = "another_dose"
      vaccineSummary = patientVaccineSummaryOf(1, "someother_vaccine")

      ReflectionHelpers.callInstanceMethod<Any>(
        recordVaccineActivity,
        "showVaccineRecordDialog",
        ReflectionHelpers.ClassParameter.from(Bundle::class.java, bundle),
        ReflectionHelpers.ClassParameter.from(PatientVaccineSummary::class.java, vaccineSummary)
      )
      dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

      Assert.assertNotNull(dialog)
      Assert.assertEquals("Initially received someother_vaccine", dialog.title)
      Assert.assertEquals("Second vaccine dose should be same as first", dialog.message)
    }
  }

  private fun patientVaccineSummaryOf(
    doseNumber: Int = 1,
    initialDose: String,
  ): PatientVaccineSummary {
    return PatientVaccineSummary(doseNumber, initialDose)
  }

  private fun getImmunization(): Immunization {
    return Immunization().apply {
      recorded = SimpleDateFormat("yyyy-MM-dd").parse("2021-09-16")
      vaccineCode =
        CodeableConcept().apply {
          this.text = "vaccineA"
          this.coding = listOf(Coding("", "vaccineA", "vaccineA"))
        }
      occurrence =
        mockk<DateTimeType>().apply { every { toHumanDisplay() } returns "Sep 16, 2021 6:13:22 PM" }

      protocolApplied =
        listOf(
          Immunization.ImmunizationProtocolAppliedComponent().apply {
            doseNumber = PositiveIntType(1)
          }
        )
    }
  }

  override fun getActivity(): Activity {
    return recordVaccineActivity
  }
}
