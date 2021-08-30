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
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig.Companion.COVAX_ARG_ITEM_ID
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig.Companion.COVAX_DETAIL_VIEW_CONFIG_ID
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.shadow.TestUtils
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientRepository
import org.smartregister.fhircore.engine.data.local.repository.patient.model.PatientVaccineSummary
import org.smartregister.fhircore.engine.util.DateUtils

@Config(shadows = [EirApplicationShadow::class])
@ExperimentalCoroutinesApi
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
        putExtra(COVAX_DETAIL_VIEW_CONFIG_ID, "covax_client_register_config.json")
        putExtra(COVAX_ARG_ITEM_ID, "test_patient_id")
      }

    recordVaccineActivity =
      Robolectric.buildActivity(RecordVaccineActivity::class.java, intent).create().resume().get()
  }

  @Test
  @Ignore("TO DO: Fix")
  fun testVerifyRecordedVaccineSavedDialogProperty() {
    coroutinesTestRule.runBlockingTest {
      val patientRepository = mockk<PatientRepository>()
      val immunization = spyk<Immunization>()
      every { immunization.protocolApplied } returns
        listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(0)))
      every { immunization.vaccineCode.coding } returns listOf(Coding("sys", "vaccine", "disp"))
      coEvery { patientRepository.getPatientImmunizations(any()) } returns listOf(immunization)

      ReflectionHelpers.setField(
        recordVaccineActivity.recordVaccineViewModel,
        "patientRepository",
        patientRepository
      )

      mockkObject(ResourceMapper)

      val entryComponent = mockk<Bundle.BundleEntryComponent>()
      val bundle = mockk<Bundle>()
      val questionnaireResponse = mockk<QuestionnaireResponse>()
      val item = mockk<QuestionnaireResponse.QuestionnaireResponseItemComponent>()
      val answer = mockk<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>()
      val items = listOf(item)
      val answerItems = listOf(answer)
      val coding = mockk<Coding>()

      every { entryComponent.resource } returns Immunization()
      every { bundle.entry } returns listOf(entryComponent)
      coEvery { ResourceMapper.extract(any(), any()) } returns bundle
      every { questionnaireResponse.item } returns items
      every { item.answer } throws IndexOutOfBoundsException()
      every { answer.valueCoding } returns coding
      every { coding.code } answers { "vaccine" }
      every { item.answer } returns answerItems

      Assert.assertNull(ShadowAlertDialog.getLatestAlertDialog())

      ReflectionHelpers.callInstanceMethod<Any>(
        recordVaccineActivity,
        "handleImmunizationResult",
        ReflectionHelpers.ClassParameter.from(
          QuestionnaireResponse::class.java,
          questionnaireResponse
        ),
      )

      val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

      val vaccineDate = DateTimeType.today().toHumanDisplay()
      val nextVaccineDate = DateUtils.addDays(vaccineDate, 28)

      Assert.assertNotNull(dialog)
      Assert.assertEquals("vaccine 1st dose recorded", dialog.title)
      Assert.assertEquals("Dose 2 due $nextVaccineDate", dialog.message)

      unmockkObject(ResourceMapper)
    }
  }

  @Test
  fun testShowVaccineRecordDialogVerifyAllOptions() {
    coroutinesTestRule.runBlockingTest {
      var vaccineSummary = patientVaccineSummaryOf(1, "vaccineA")

      val immunization =
        Immunization().apply {
          recorded = Date()
          vaccineCode =
            CodeableConcept().apply {
              this.text = "vaccineA"
              this.coding = listOf(Coding("", "vaccineA", "vaccineA"))
            }
          occurrence = DateTimeType.today()

          protocolApplied =
            listOf(
              Immunization.ImmunizationProtocolAppliedComponent().apply {
                doseNumber = PositiveIntType(1)
              }
            )
        }

      val vaccineDate = immunization.occurrenceDateTimeType.toHumanDisplay()
      val nextVaccineDate = DateUtils.addDays(vaccineDate, 28)

      ReflectionHelpers.callInstanceMethod<Any>(
        recordVaccineActivity,
        "showVaccineRecordDialog",
        ReflectionHelpers.ClassParameter.from(Immunization::class.java, immunization),
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
        ReflectionHelpers.ClassParameter.from(Immunization::class.java, immunization),
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
        ReflectionHelpers.ClassParameter.from(Immunization::class.java, immunization),
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

  override fun getActivity(): Activity {
    return recordVaccineActivity
  }
}
