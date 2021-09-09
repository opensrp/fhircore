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

package org.smartregister.fhircore.anc.ui.family.form

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RESPONSE_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_BYPASS_EXTRACTOR_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.buildQuestionnaireIntent
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.parser

data class RegisterFamilyMemberInput(val preAssignedId: String, val familyFormConfig: FamilyFormConfig)
data class RegisterFamilyMemberOutput(val questionnaireResponse: QuestionnaireResponse)

class RegisterFamilyMemberResult : ActivityResultContract<RegisterFamilyMemberInput, RegisterFamilyMemberOutput?>() {

  override fun createIntent(context: Context, input: RegisterFamilyMemberInput): Intent {
    return buildQuestionnaireIntent(
      context,
      input.familyFormConfig.memberRegistrationQuestionnaireTitle,
      input.familyFormConfig.memberRegistrationQuestionnaireId,
      input.preAssignedId,
      true
    ).putExtra(QUESTIONNAIRE_BYPASS_EXTRACTOR_KEY, true)
  }

  override fun parseResult(resultCode: Int, intent: Intent?): RegisterFamilyMemberOutput? {
    val data = intent?.getStringExtra(QUESTIONNAIRE_ARG_RESPONSE_KEY)
    return if (resultCode == Activity.RESULT_OK && data != null) {
      return RegisterFamilyMemberOutput(parser.parseResource(data) as QuestionnaireResponse)
    } else null
  }
}
