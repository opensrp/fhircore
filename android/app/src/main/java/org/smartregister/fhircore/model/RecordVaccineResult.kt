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
import androidx.activity.result.contract.ActivityResultContract
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_ACTIVITY_RESULT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RESPONSE_KEY
import org.smartregister.fhircore.util.QuestionnaireUtils

class RecordVaccineResult(private val patientId: String) :
  ActivityResultContract<CovaxDetailView, QuestionnaireResponse?>() {
  override fun createIntent(context: Context, input: CovaxDetailView): Intent {
    return QuestionnaireUtils.buildQuestionnaireIntent(
      context,
      input.vaccineQuestionnaireTitle,
      input.vaccineQuestionnaireIdentifier,
      patientId,
      false
    )
  }

  override fun parseResult(resultCode: Int, intent: Intent?): QuestionnaireResponse? {
    val data =
      intent
        ?.getBundleExtra(QUESTIONNAIRE_ARG_ACTIVITY_RESULT_KEY)
        ?.getString(QUESTIONNAIRE_ARG_RESPONSE_KEY)

    return if (resultCode == Activity.RESULT_OK && data != null) {
      QuestionnaireUtils.asQuestionnaireResponse(data)
    } else null
  }
}
