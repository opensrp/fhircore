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

package org.smartregister.fhircore.anc.ui.family.register.form

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.smartregister.fhircore.anc.ui.family.FamilyFormConfig
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.buildQuestionnaireIntent

data class RegisterFamilyMemberData(val headId: String, val familyFormConfig: FamilyFormConfig)

class RegisterFamilyMemberResult : ActivityResultContract<RegisterFamilyMemberData, String?>() {

  override fun createIntent(context: Context, input: RegisterFamilyMemberData): Intent {
    val intent =
      buildQuestionnaireIntent(
        context,
        input.familyFormConfig.memberRegistrationQuestionnaireTitle,
        input.familyFormConfig.memberRegistrationQuestionnaireIdentifier,
        null,
        true
      )
    intent.putExtra(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY, input.headId)
    return intent
  }

  override fun parseResult(resultCode: Int, intent: Intent?): String? {
    val data = intent?.getStringExtra(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY)

    return if (resultCode == Activity.RESULT_OK && data != null) {
      return data
    } else null
  }
}
