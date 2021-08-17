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

package org.smartregister.fhircore.eir.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.util.Date
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
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.model.PatientItem
import org.smartregister.fhircore.eir.model.PatientStatus
import org.smartregister.fhircore.eir.model.PatientVaccineSummary
import org.smartregister.fhircore.eir.shadow.FhirApplicationShadow
import org.smartregister.fhircore.eir.shadow.TestUtils
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsFormConfig
import org.smartregister.fhircore.eir.ui.vaccine.RecordVaccineActivity
import org.smartregister.fhircore.eir.util.Utils
import org.smartregister.fhircore.engine.data.local.repository.model.VaccineStatus

@Config(shadows = [FhirApplicationShadow::class])
class RecordVaccineActivityTest : ActivityRobolectricTest() {

  private lateinit var recordVaccineActivity: RecordVaccineActivity

  @Before
  fun setUp() {
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.load(Patient::class.java, "test_patient_id") } returns
      TestUtils.TEST_PATIENT_1
    coEvery { fhirEngine.search<Immunization>(any()) } returns listOf()
    coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns Questionnaire()

    mockkObject(EirApplication)
    every { EirApplication.fhirEngine(any()) } returns fhirEngine

    val intent =
      Intent().apply {
        putExtra(
          PatientDetailsFormConfig.COVAX_DETAIL_VIEW_CONFIG_ID,
          "covax_client_register_config.json"
        )
        putExtra(PatientDetailsFormConfig.COVAX_ARG_ITEM_ID, "test_patient_id")
      }

    recordVaccineActivity =
      Robolectric.buildActivity(RecordVaccineActivity::class.java, intent).create().resume().get()
  }

  @Test
  fun testVerifyRecordedVaccineSavedDialogProperty() {

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
    every { ResourceMapper.extract(any(), any()) } returns bundle

    every { questionnaireResponse.item } returns items
    every { item.answer } throws IndexOutOfBoundsException()
    every { answer.valueCoding } returns coding
    every { coding.code } answers { "dummy" }
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
    val nextVaccineDate = Utils.addDays(vaccineDate, 28)

    Assert.assertNotNull(dialog)
    Assert.assertEquals("dummy 1st dose recorded", dialog.title)
    Assert.assertEquals("Dose 2 due $nextVaccineDate", dialog.message)

    unmockkObject(ResourceMapper)
  }

  @Test
  fun testShowVaccineRecordDialogVerifyAllOptions() {
    var patientItem = patientItemOf(1, "vaccineA", VaccineStatus.DUE)

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
    val nextVaccineDate = Utils.addDays(vaccineDate, 28)

    ReflectionHelpers.callInstanceMethod<Any>(
      recordVaccineActivity,
      "showVaccineRecordDialog",
      ReflectionHelpers.ClassParameter.from(Immunization::class.java, immunization),
      ReflectionHelpers.ClassParameter.from(PatientItem::class.java, patientItem)
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
      ReflectionHelpers.ClassParameter.from(PatientItem::class.java, patientItem)
    )
    dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("vaccineA 1st dose recorded", dialog.title)
    Assert.assertEquals("Fully vaccinated", dialog.message)

    immunization.vaccineCode.coding[0].code = "another_dose"
    patientItem = patientItemOf(1, "someother_vaccine", VaccineStatus.DUE)

    ReflectionHelpers.callInstanceMethod<Any>(
      recordVaccineActivity,
      "showVaccineRecordDialog",
      ReflectionHelpers.ClassParameter.from(Immunization::class.java, immunization),
      ReflectionHelpers.ClassParameter.from(PatientItem::class.java, patientItem)
    )
    dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("Initially received someother_vaccine", dialog.title)
    Assert.assertEquals("Second vaccine dose should be same as first", dialog.message)
  }

  private fun patientItemOf(
    doseNumber: Int,
    initialDose: String,
    status: VaccineStatus
  ): PatientItem {
    val patientVaccineSummary = PatientVaccineSummary(doseNumber, initialDose)
    val patientStatus = PatientStatus(status, "none")
    return PatientItem(
      "",
      "",
      "",
      "2000-01-01",
      "",
      "",
      "",
      "HR",
      patientStatus,
      patientVaccineSummary,
      lastSeen = "07-26-2021"
    )
  }

  override fun getActivity(): Activity {
    return recordVaccineActivity
  }
}
