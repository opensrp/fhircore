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
import android.content.Intent
import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_TITLE_KEY

object QuestionnaireUtils {
  private val parser = FhirContext.forR4().newJsonParser()

  fun buildQuestionnaireIntent(
    context: Context,
    questionnaireTitle: String,
    questionnaireId: String,
    patientId: String?,
    isNewPatient: Boolean
  ): Intent {
    return Intent(context, QuestionnaireActivity::class.java).apply {
      putExtra(QUESTIONNAIRE_TITLE_KEY, questionnaireTitle)
      putExtra(QUESTIONNAIRE_PATH_KEY, questionnaireId)

      patientId?.let {
        if (isNewPatient) putExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID, patientId)
        else putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)
      }
    }
  }

  fun asQuestionnaireResponse(questionnaireResponse: String): QuestionnaireResponse {
    return parser.parseResource(questionnaireResponse) as QuestionnaireResponse
  }
}
