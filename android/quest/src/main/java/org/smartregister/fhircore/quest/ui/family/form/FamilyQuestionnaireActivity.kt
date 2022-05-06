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

package org.smartregister.fhircore.quest.ui.family.form

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.quest.R

@AndroidEntryPoint
class FamilyQuestionnaireActivity : QuestionnaireActivity() {

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    questionnaireViewModel.extractAndSaveResources(
      this,
      intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY),
      questionnaire,
      questionnaireResponse,
      questionnaireType
    )
  }

  override fun postSaveSuccessful(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      val patientId = questionnaireResponse.subject.extractId()

      if (questionnaireConfig.form == ANC_ENROLLMENT_FORM) {
        finish()
      } else {
        handlePregnancy(patientId, questionnaireResponse)
      }
    }
  }

  override fun getDismissDialogMessage(): Int {
    return R.string.unsaved_changes_message_alert
  }

  private fun handlePregnancy(patientId: String, questionnaireResponse: QuestionnaireResponse) {
    val pregnancy =
      questionnaireResponse
        .find(IS_PREGNANT_KEY)
        ?.answer
        ?.firstOrNull()
        ?.valueBooleanType
        ?.booleanValue()
    if (pregnancy == true) {
      startAncEnrollment(patientId)
    } else {
      finish()
    }
  }
  // todo - will be removed
  private fun startAncEnrollment(patientId: String) {
    startActivity(
      Intent(this, FamilyQuestionnaireActivity::class.java)
        .putExtras(intentArgs(clientIdentifier = patientId, formName = ANC_ENROLLMENT_FORM))
        .putExtra(
          QUESTIONNAIRE_CALLING_ACTIVITY,
          intent.getStringExtra(QUESTIONNAIRE_CALLING_ACTIVITY) ?: this::class.java.name
        )
    )
  }

  companion object {
    const val QUESTIONNAIRE_RELATED_TO_KEY = "questionnaire-related-to"
    const val QUESTIONNAIRE_CALLING_ACTIVITY = "questionnaire-calling-activity"
    const val IS_PREGNANT_KEY = "is_pregnant"
    const val HEAD_RECORD_ID_KEY = "head_record_id"
    const val FAMILY_MEMBER_REGISTER_FORM = "family-member-registration"
    const val ANC_ENROLLMENT_FORM = "anc-patient-registration"
  }
}
