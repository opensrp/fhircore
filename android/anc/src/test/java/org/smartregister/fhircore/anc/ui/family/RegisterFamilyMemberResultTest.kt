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

package org.smartregister.fhircore.anc.ui.family

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConfig
import org.smartregister.fhircore.anc.ui.family.form.RegisterFamilyMemberInput
import org.smartregister.fhircore.anc.ui.family.form.RegisterFamilyMemberResult
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RESPONSE_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_TITLE_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.parser
import org.smartregister.fhircore.engine.util.FormConfigUtil

@Config(shadows = [AncApplicationShadow::class])
class RegisterFamilyMemberResultTest : RobolectricTest() {
  private lateinit var context: Context
  private lateinit var activityResultContract: RegisterFamilyMemberResult
  private lateinit var familyFormConfig: FamilyFormConfig

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    activityResultContract = RegisterFamilyMemberResult()
    familyFormConfig =
      FormConfigUtil.loadConfig(FamilyFormConfig.FAMILY_REGISTER_CONFIG_ID, context)
  }

  @Test
  fun testCreateIntentShouldReturnIntentWithRightValues() {
    val result =
      activityResultContract.createIntent(
        context,
        RegisterFamilyMemberInput("123", familyFormConfig)
      )

    assertEquals(
      familyFormConfig.memberRegistrationQuestionnaireTitle,
      result.getStringExtra(QUESTIONNAIRE_TITLE_KEY)
    )
    assertEquals(
      familyFormConfig.memberRegistrationQuestionnaireId,
      result.getStringExtra(QUESTIONNAIRE_PATH_KEY)
    )
    assertEquals("123", result.getStringExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID))
    assertNull(result.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY))
  }

  @Test
  fun testParseResultShouldReturnQuestionnaireResponse() {
    val qrJson =
      context
        .assets
        .open("sample_family_member_registration_questionnaireresponse.json")
        .bufferedReader()
        .use { it.readText() }

    val intent = Intent().apply { putExtra(QUESTIONNAIRE_ARG_RESPONSE_KEY, qrJson) }

    val result = activityResultContract.parseResult(Activity.RESULT_OK, intent)!!

    assertEquals(qrJson, parser.encodeResourceToString(result.questionnaireResponse))
  }
}
