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
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.datacapture.QuestionnaireFragment
import java.text.SimpleDateFormat
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientDetailFragment
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class QuestionnaireActivityTest : ActivityRobolectricTest() {

  private lateinit var questionnaireActivity: QuestionnaireActivity

  @Before
  fun setUp() {

    init()

    val intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY, "patient-registration.json")
        putExtra(PatientDetailFragment.ARG_ITEM_ID, TEST_PATIENT_1_ID)
      }
    questionnaireActivity =
      Robolectric.buildActivity(QuestionnaireActivity::class.java, intent).create().resume().get()
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(questionnaireActivity)
  }

  @Test
  fun testVerifyPrePopulatedQuestionnaire() {
    val fragment =
      questionnaireActivity.supportFragmentManager.findFragmentByTag(
        QuestionnaireActivity.QUESTIONNAIRE_FRAGMENT_TAG
      ) as
        QuestionnaireFragment

    Assert.assertNotNull(fragment)

    val response = fragment.getQuestionnaireResponse()
    Assert.assertEquals(
      TEST_PATIENT_1.name[0].given[0].toString(),
      response.find("PR-name-text")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.name[0].family,
      response.find("PR-name-family")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.birthDate.toString(),
      response.find("patient-0-birth-date")?.valueDateType?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.gender.toCode(),
      response.find("patient-0-gender")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.telecom[0].value,
      response.find("PR-telecom-value")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.address[0].city,
      response.find("PR-address-city")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.address[0].country,
      response.find("PR-address-country")?.value.toString()
    )
    Assert.assertEquals(
      TEST_PATIENT_1.active,
      response.find("PR-active")?.valueBooleanType?.booleanValue()
    )
  }

  @Test
  fun testVerifyPatientResourceSaved() {
    questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).performClick()

    val expectedIntent = Intent(questionnaireActivity, PatientListActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }

  private fun init() {
    runBlocking {
      FhirApplication.fhirEngine(ApplicationProvider.getApplicationContext()).save(TEST_PATIENT_1)
    }
  }

  private fun QuestionnaireResponse.find(
    linkId: String
  ): QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent? {
    return item.find(linkId, null)
  }

  private fun List<QuestionnaireResponse.QuestionnaireResponseItemComponent>.find(
    linkId: String,
    default: QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent?
  ): QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent? {
    var result = default
    run loop@{
      forEach {
        if (it.linkId == linkId) {
          result = if (it.answer.isNotEmpty()) it.answer[0] else default
          return@loop
        } else if (it.item.isNotEmpty()) {
          result = it.item.find(linkId, result)
        }
      }
    }

    return result
  }

  companion object {
    const val TEST_PATIENT_1_ID = "test_patient_1"
    var TEST_PATIENT_1 = Patient()

    init {
      TEST_PATIENT_1.id = TEST_PATIENT_1_ID
      TEST_PATIENT_1.gender = Enumerations.AdministrativeGender.MALE
      TEST_PATIENT_1.name =
        mutableListOf(
          HumanName().apply {
            addGiven("jane")
            setFamily("Mc")
          }
        )
      TEST_PATIENT_1.telecom = mutableListOf(ContactPoint().apply { value = "12345678" })
      TEST_PATIENT_1.address =
        mutableListOf(
          Address().apply {
            city = "Nairobi"
            country = "Kenya"
          }
        )
      TEST_PATIENT_1.active = true
      TEST_PATIENT_1.birthDate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25")
    }
  }
}
