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

package org.smartregister.fhircore.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_ACTIVITY_RESULT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RESPONSE_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_TITLE_KEY
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.Utils

@Config(shadows = [FhirApplicationShadow::class])
class RegisterFamilyMemberResultTest : RobolectricTest() {
  private lateinit var context: Context
  private lateinit var activityResultContract: RegisterFamilyMemberResult
  private lateinit var detailView: FamilyDetailView

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    activityResultContract = RegisterFamilyMemberResult()
    detailView =
      Utils.loadConfig("family_client_register_config.json", FamilyDetailView::class.java, context)
  }

  @Test
  fun testCreateIntentShouldReturnIntentWithRightValues() {
    val result =
      activityResultContract.createIntent(context, RegisterFamilyMemberData("123", detailView))

    assertEquals(
      detailView.memberRegistrationQuestionnaireTitle,
      result.getStringExtra(QUESTIONNAIRE_TITLE_KEY)
    )
    assertEquals(
      detailView.memberRegistrationQuestionnaireIdentifier,
      result.getStringExtra(QUESTIONNAIRE_PATH_KEY)
    )
    assertEquals("123", result.getStringExtra(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY))
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

    val intent =
      Intent().apply {
        putExtra(
          QUESTIONNAIRE_ARG_ACTIVITY_RESULT_KEY,
          bundleOf(
            QUESTIONNAIRE_ARG_RESPONSE_KEY to qrJson,
            QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY to "123"
          )
        )
      }

    val result = activityResultContract.parseResult(Activity.RESULT_OK, intent)!!

    assertEquals("123", result)
  }
}
