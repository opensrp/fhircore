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

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.util.othersEligibleForHead
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.base.AlertDialogListItem
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.getSingleChoiceSelectedKey
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class RemoveFamilyQuestionnaireActivity : QuestionnaireActivity() {
  private lateinit var saveBtn: Button
  private lateinit var familyId: String

  override val questionnaireViewModel by viewModels<RemoveFamilyQuestionnaireViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupUI()
    loadData()
  }

  private fun loadData() {
    familyId = intent.extras?.getString(QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    questionnaireViewModel.fetchFamilyMembers(familyId)
  }

  private fun setupUI() {
    saveBtn = findViewById(R.id.btn_save_client_info)
    saveBtn.text = getString(R.string.questionnaire_remove_family_member_btn_save_client_info)
    showFamilyHeadDialog()
    didFamilyMemberRemoved()
  }

  private fun didFamilyMemberRemoved() {
    questionnaireViewModel.shouldRemoveFamilyMember.observe(
      this@RemoveFamilyQuestionnaireActivity
    ) { deletePatient ->
      if (deletePatient) {
        switchToPatientScreen(familyId)
      }
    }
  }

  private fun showFamilyHeadDialog() {
    questionnaireViewModel.shouldOpenHeadDialog.observe(this@RemoveFamilyQuestionnaireActivity) {
      val eligibleMembers = questionnaireViewModel.familyMembers.othersEligibleForHead()

      if (eligibleMembers.isNullOrEmpty()) {
        showToast(getString(org.smartregister.fhircore.anc.R.string.no_eligible_family_head))
      } else {
        AlertDialogue.showConfirmAlert(
          context = this@RemoveFamilyQuestionnaireActivity,
          message = org.smartregister.fhircore.anc.R.string.change_head_confirm_message,
          title = org.smartregister.fhircore.anc.R.string.change_head_confirm_title,
          confirmButtonListener =
            this@RemoveFamilyQuestionnaireActivity::onFamilyHeadChangeRequested,
          confirmButtonText = org.smartregister.fhircore.anc.R.string.change_head_button_title,
          options = eligibleMembers.map { AlertDialogListItem(it.id, it.name) }
        )
      }
    }
  }

  private fun onFamilyHeadChangeRequested(dialog: DialogInterface) {
    val selection = getSelectedKey(dialog)
    if (selection?.isNotBlank() == true) {
      questionnaireViewModel.changeFamilyHead(familyId, selection).observe(
          this@RemoveFamilyQuestionnaireActivity
        ) { changeHead ->
        if (changeHead) {
          dialog.dismiss()
          questionnaireViewModel.deleteFamilyMember(familyId)
        }
      }
    } else this.showToast(getString(org.smartregister.fhircore.anc.R.string.invalid_selection))
  }

  private fun getSelectedKey(dialog: DialogInterface): String? {
    return (dialog as AlertDialog).getSingleChoiceSelectedKey()
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    questionnaireViewModel.extractionProgress.postValue(true)
  }

  override fun postSaveSuccessful(questionnaireResponse: QuestionnaireResponse) {
    // remove the family member data from resourceEntity
    questionnaireViewModel.process(
      intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY),
      questionnaire,
      questionnaireResponse
    )
  }

  private fun switchToPatientScreen(uniqueIdentifier: String) {
    val intent =
      Intent(this, FamilyDetailsActivity::class.java).apply {
        putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, uniqueIdentifier)
      }
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    startActivity(intent)
  }

  override fun showFormSubmissionConfirmAlert() {
    AlertDialogue.showConfirmAlert(
      context = this,
      message = R.string.questionnaire_alert_submit_message,
      title = R.string.questionnaire_alert_submit_title,
      confirmButtonListener = { handleQuestionnaireSubmit() },
      confirmButtonText = R.string.questionnaire_remove_family_member_alert_submit_button_title
    )
  }
}
