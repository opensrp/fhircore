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
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.buildQuestionnaireIntent

class RegisterFamilyResult : ActivityResultContract<FamilyFormConfig, String?>() {

  override fun createIntent(context: Context, input: FamilyFormConfig): Intent {
    val questionnaireId = input.registrationQuestionnaireIdentifier
    val questionnaireTitle = input.registrationQuestionnaireTitle

    return buildQuestionnaireIntent(
      context = context,
      questionnaireTitle = questionnaireTitle,
      questionnaireId = questionnaireId,
      patientId = null,
      isNewPatient = true
    )
  }

  override fun parseResult(resultCode: Int, intent: Intent?): String? {
    val data = intent?.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)

    if (resultCode == Activity.RESULT_OK && data != null) {
      return data
    }

    return null
  }
}
