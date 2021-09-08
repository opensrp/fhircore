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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_TITLE_KEY

@Config(shadows = [EirApplicationShadow::class])
class QuestionnaireUtilsTest : RobolectricTest() {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private lateinit var questionnaire: Questionnaire
  private lateinit var questionnaireResponse: QuestionnaireResponse

  @Before
  fun setup() {
    val iParser: IParser = FhirContext.forR4().newJsonParser()

    val qJson =
      context.assets.open("sample_patient_registration.json").bufferedReader().use { it.readText() }

    val qrJson =
      context.assets.open("sample_registration_questionnaireresponse.json").bufferedReader().use {
        it.readText()
      }

    questionnaire = iParser.parseResource(qJson) as Questionnaire
    questionnaireResponse = iParser.parseResource(qrJson) as QuestionnaireResponse
  }

  @Test
  fun testBuildQuestionnaireIntent_shouldReturnIntentWithExtrasWithPatientId() {
    val result =
      QuestionnaireUtils.buildQuestionnaireIntent(context, "My Q title", "my-q-id", "12345", false)

    assertEquals(QuestionnaireActivity::class.java.name, result.component!!.className)

    assertEquals("My Q title", result.getStringExtra(QUESTIONNAIRE_TITLE_KEY))
    assertEquals("my-q-id", result.getStringExtra(QUESTIONNAIRE_PATH_KEY))

    // should set patient-arg key and not the pre-assigned-key which is for new patients
    assertEquals("12345", result.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY))
    assertNull(result.getStringExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID))
  }

  @Test
  fun testBuildQuestionnaireIntent_shouldReturnIntentWithExtrasWithNewPatient() {
    val result =
      QuestionnaireUtils.buildQuestionnaireIntent(context, "My Q title", "my-q-id", "12345", true)

    assertEquals(QuestionnaireActivity::class.java.name, result.component!!.className)

    assertEquals("My Q title", result.getStringExtra(QUESTIONNAIRE_TITLE_KEY))
    assertEquals("my-q-id", result.getStringExtra(QUESTIONNAIRE_PATH_KEY))

    // should set patient-arg key and not the pre-assigned-key which is for new patients
    assertEquals("12345", result.getStringExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID))
    assertNull(result.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY))
  }

  private fun getQuestionnaireResponseItem(
    linkId: String
  ): QuestionnaireResponse.QuestionnaireResponseItemComponent {
    // comorbidities section is on 2 index
    return questionnaireResponse.item[2].item.single { it.linkId!!.contentEquals(linkId) }
  }

  private fun setResponsesToTrue(vararg linkIds: String) {
    linkIds.forEach { getQuestionnaireResponseItem(it).addAnswer().value = BooleanType(true) }
  }

  private fun setResponsesToFalse(vararg linkIds: String) {
    linkIds.forEach { getQuestionnaireResponseItem(it).addAnswer().value = BooleanType(false) }
  }
}
