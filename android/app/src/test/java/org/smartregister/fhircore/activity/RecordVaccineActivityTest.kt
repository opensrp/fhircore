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

package org.smartregister.fhircore.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.widget.Button
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.lang.IndexOutOfBoundsException
import java.util.Date
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.Utils

@Config(shadows = [FhirApplicationShadow::class])
class RecordVaccineActivityTest : ActivityRobolectricTest() {

  private lateinit var recordVaccineActivity: RecordVaccineActivity

  @Before
  fun setUp() {

    val intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Record Vaccine")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY, "sample_record_vaccine.json")
        putExtra(PATIENT_ID, "1")
        putExtra(INITIAL_DOSE, "dummy")
      }

    recordVaccineActivity =
      Robolectric.buildActivity(RecordVaccineActivity::class.java, intent).create().resume().get()
  }

  @Test
  fun testVerifyRecordedVaccineSavedDialogProperty() {

    mockkObject(ResourceMapper)

    val entryComponent = mockk<Bundle.BundleEntryComponent>()
    val bundle = mockk<Bundle>()
    val questionnaireFragment = mockk<QuestionnaireFragment>()
    val questionnaireResponse = mockk<QuestionnaireResponse>()
    val item = mockk<QuestionnaireResponse.QuestionnaireResponseItemComponent>()
    val answer = mockk<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>()
    val items = listOf(item)
    val answerItems = listOf(answer)
    val coding = mockk<Coding>()

    every { entryComponent.resource } returns Immunization()
    every { bundle.entry } returns listOf(entryComponent)
    every { ResourceMapper.extract(any(), any()) } returns bundle

    every { questionnaireFragment.getQuestionnaireResponse() } returns questionnaireResponse
    every { questionnaireResponse.item } returns items
    every { item.answer } throws IndexOutOfBoundsException()
    every { answer.valueCoding } returns coding
    every { coding.code } returns "dummy"

    val fragmentField = recordVaccineActivity.javaClass.getDeclaredField("fragment")
    fragmentField.isAccessible = true
    fragmentField.set(recordVaccineActivity, questionnaireFragment)

    recordVaccineActivity.findViewById<Button>(R.id.btn_record_vaccine).performClick()
    Assert.assertNull(ShadowAlertDialog.getLatestAlertDialog())

    every { item.answer } returns answerItems
    recordVaccineActivity.findViewById<Button>(R.id.btn_record_vaccine).performClick()
    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    val vaccineDate = DateTimeType.today().toHumanDisplay()
    val nextVaccineDate = Utils.addDays(vaccineDate, 28)

    Assert.assertNotNull(dialog)
    Assert.assertEquals("dummy 1st dose recorded", dialog.title)
    Assert.assertEquals("Dose 2 due $nextVaccineDate", dialog.message)
  }

  @Test
  fun testShowVaccineRecordDialogVerifyAllOptions() {

    val immunization =
      Immunization().apply {
        recorded = Date()
        vaccineCode =
          CodeableConcept().apply {
            this.text = "dummy"
            this.coding = listOf(Coding("", "dummy", "dummy"))
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
      ReflectionHelpers.ClassParameter.from(Immunization::class.java, immunization)
    )

    val shadowAlertDialog = ShadowAlertDialog.getLatestAlertDialog()
    var dialog = shadowOf(shadowAlertDialog)

    Assert.assertNotNull(dialog)
    Assert.assertEquals("dummy 1st dose recorded", dialog.title)
    Assert.assertEquals("Dose 2 due $nextVaccineDate", dialog.message)

    shadowAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
    immunization.protocolApplied[0].doseNumber = PositiveIntType(2)

    ReflectionHelpers.callInstanceMethod<Any>(
      recordVaccineActivity,
      "showVaccineRecordDialog",
      ReflectionHelpers.ClassParameter.from(Immunization::class.java, immunization)
    )
    dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("dummy 1st dose recorded", dialog.title)
    Assert.assertEquals("Fully vaccinated", dialog.message)

    immunization.vaccineCode.coding[0].code = "another_dose"

    ReflectionHelpers.callInstanceMethod<Any>(
      recordVaccineActivity,
      "showVaccineRecordDialog",
      ReflectionHelpers.ClassParameter.from(Immunization::class.java, immunization)
    )
    dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("Initially  received dummy", dialog.title)
    Assert.assertEquals("Second vaccine dose should be same as first", dialog.message)
  }

  @Test
  fun testOnOptionsItemSelectedShouldReturnExpectedBoolean() {
    Assert.assertTrue(recordVaccineActivity.onOptionsItemSelected(RoboMenuItem(android.R.id.home)))
    Assert.assertFalse(recordVaccineActivity.onOptionsItemSelected(RoboMenuItem()))
  }

  override fun getActivity(): Activity {
    return recordVaccineActivity
  }
}
