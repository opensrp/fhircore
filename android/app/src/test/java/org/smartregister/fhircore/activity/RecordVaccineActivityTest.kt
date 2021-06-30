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
import android.content.Intent
import android.widget.Button
import com.google.android.fhir.datacapture.QuestionnaireFragment
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.smartregister.fhircore.R
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class RecordVaccineActivityTest : ActivityRobolectricTest() {

  private lateinit var recordVaccineActivity: RecordVaccineActivity

  @Before
  fun setUp() {

    val intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Record Vaccine")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY, "record-vaccine.json")
        putExtra(USER_ID, "1")
      }

    recordVaccineActivity =
      Robolectric.buildActivity(RecordVaccineActivity::class.java, intent).create().resume().get()
  }

  @Test
  fun testVerifyRecordedVaccineSavedDialogProperty() {

    val questionnaireFragment = mockk<QuestionnaireFragment>()
    val questionnaireResponse = mockk<QuestionnaireResponse>()
    val item = mockk<QuestionnaireResponse.QuestionnaireResponseItemComponent>()
    val answer = mockk<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>()
    val items = listOf(item)
    val answerItems = listOf(answer)
    val coding = mockk<Coding>()

    every { questionnaireFragment.getQuestionnaireResponse() } returns questionnaireResponse
    every { questionnaireResponse.item } returns items
    every { item.answer } returns answerItems
    every { answer.valueCoding } returns coding
    every { coding.code } returns "dummy"

    val fragmentField = recordVaccineActivity.javaClass.getDeclaredField("fragment")
    fragmentField.isAccessible = true
    fragmentField.set(recordVaccineActivity, questionnaireFragment)

    recordVaccineActivity.findViewById<Button>(R.id.btn_record_vaccine).performClick()
    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("dummy 1st dose recorded", dialog.title)
    Assert.assertEquals("Second dose due on 27-04-2021", dialog.message)
  }

  override fun getActivity(): Activity {
    return recordVaccineActivity
  }
}
