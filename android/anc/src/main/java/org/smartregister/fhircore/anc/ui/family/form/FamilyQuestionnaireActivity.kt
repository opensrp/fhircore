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

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.util.startAncEnrollment
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.hide

@AndroidEntryPoint
class FamilyQuestionnaireActivity : QuestionnaireActivity() {

  @Inject lateinit var familyRepository: FamilyRepository

  lateinit var saveBtn: Button
  private var isEditFamily: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    isEditFamily = questionnaireType.isEditMode()
    saveBtn = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)

    val action =
      if (isEditFamily) getString(R.string.form_action_edit)
      else getString(R.string.form_action_save)

    when (intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)!!) {
      FamilyFormConstants.ANC_ENROLLMENT_FORM -> saveBtn.setText(R.string.mark_as_anc_client)
      FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM ->
        saveBtn.text = getString(R.string.family_member_save_label, action)
      FamilyFormConstants.FAMILY_REGISTER_FORM ->
        saveBtn.text = getString(R.string.family_save_label, action)
    }
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    saveBtn.hide(false)

    questionnaireViewModel.extractAndSaveResources(
      context = this,
      resourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY),
      groupResourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_GROUP_KEY),
      questionnaireResponse = questionnaireResponse,
      questionnaireType = questionnaireType,
      questionnaire = questionnaire
    )
  }

  override fun populateInitialValues(questionnaire: Questionnaire) {
    if (questionnaireConfig.form == FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM) {
      questionnaire.find(HEAD_RECORD_ID_KEY)!!.initialFirstRep.value =
        StringType(intent.getStringExtra(QUESTIONNAIRE_RELATED_TO_KEY)!!)
    }
  }

  override fun postSaveSuccessful(questionnaireResponse: QuestionnaireResponse, extras: List<Resource>?) {
    lifecycleScope.launch {
      val patientId = questionnaireResponse.subject.extractId()

      if (questionnaireConfig.form == FamilyFormConstants.ANC_ENROLLMENT_FORM) {
        finish()
      } else {
        handlePregnancy(patientId, questionnaireResponse)
      }
    }
  }

  override fun onBackPressed() {
    AlertDialog.Builder(this, R.style.AlertDialogTheme)
      .setMessage(R.string.unsaved_changes_message_alert)
      .setCancelable(false)
      .setNegativeButton(R.string.unsaved_changes_neg) { dialogInterface, _ ->
        dialogInterface.dismiss()
        finish()
      }
      .setPositiveButton(R.string.unsaved_changes_pos) { dialogInterface, _ ->
        dialogInterface.dismiss()
      }
      .show()
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
      this.startAncEnrollment(patientId)
    } else {
      finish()
    }
  }

  companion object {
    const val QUESTIONNAIRE_RELATED_TO_KEY = "questionnaire-related-to"
    const val QUESTIONNAIRE_CALLING_ACTIVITY = "questionnaire-calling-activity"
    const val IS_PREGNANT_KEY = "is_pregnant"
    const val HEAD_RECORD_ID_KEY = "head_record_id"
  }
}
