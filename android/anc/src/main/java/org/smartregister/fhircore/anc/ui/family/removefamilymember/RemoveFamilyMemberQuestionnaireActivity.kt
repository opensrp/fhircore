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

package org.smartregister.fhircore.anc.ui.family.removefamilymember

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetDataModel
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetHolder
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetListDialog
import org.smartregister.fhircore.anc.util.othersEligibleForHead
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

@AndroidEntryPoint
class RemoveFamilyMemberQuestionnaireActivity :
  QuestionnaireActivity(), BottomSheetListDialog.OnClickedListItems {
  private lateinit var saveBtn: Button
  private lateinit var familyId: String

  override val questionnaireViewModel by viewModels<RemoveFamilyMemberQuestionnaireViewModel>()

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
      this@RemoveFamilyMemberQuestionnaireActivity
    ) { deletePatient ->
      if (deletePatient) {
        switchToPatientScreen(familyId)
      }
    }
  }

  private fun showFamilyHeadDialog() {
    questionnaireViewModel.shouldOpenHeadDialog.observe(
      this@RemoveFamilyMemberQuestionnaireActivity
    ) {
      if (it) {
        val eligibleMembers = questionnaireViewModel.familyMembers.othersEligibleForHead()

        if (eligibleMembers.isNullOrEmpty()) {
          showToast(getString(R.string.no_eligible_family_head))
        } else {
          openBottomSheetDialog(eligibleMembers)
        }
      }
    }
  }

  private fun openBottomSheetDialog(eligibleMembers: List<FamilyMemberItem>) {
    val options =
      eligibleMembers.map {
        BottomSheetDataModel(it.name + "," + it.birthdate.toAgeDisplay(), it.gender, it.id)
      }
    val title = getString(R.string.label_assign_new_family_head)
    val listTitle = getString(R.string.label_select_new_head)
    val warning = getString(R.string.label_remove_family_warning)
    BottomSheetListDialog(this, BottomSheetHolder(title, listTitle, warning, options), this).show()
  }

  private fun onFamilyHeadChangeRequested(newFamilyHeadId: String) {
    questionnaireViewModel.changeFamilyHead(familyId, newFamilyHeadId).observe(
        this@RemoveFamilyMemberQuestionnaireActivity
      ) { changeHead ->
      if (changeHead) {
        questionnaireViewModel.deleteFamilyMember(familyId)
      }
    }
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
    val alertDialog =
      androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
        .setTitle(R.string.confirm_remove_family_alert_title)
        .setMessage(R.string.remove_family_member_warning)
        .setCancelable(false)
        .setNegativeButton(R.string.cancel) { dialogInterface, _ -> dialogInterface.dismiss() }
        .setPositiveButton(R.string.questionnaire_remove_family_member_alert_submit_button_title) {
          dialogInterface,
          _ ->
          dialogInterface.dismiss()
          handleQuestionnaireSubmit()
        }
        .create()
    alertDialog.show()
    alertDialog
      .getButton(AlertDialog.BUTTON_NEGATIVE)
      .setTextColor(ContextCompat.getColor(this, R.color.transparent_blue))
    alertDialog
      .getButton(AlertDialog.BUTTON_POSITIVE)
      .setTextColor(ContextCompat.getColor(this, R.color.status_red))
  }

  override fun onSave(bottomSheetDataModel: BottomSheetDataModel) {
    onFamilyHeadChangeRequested(bottomSheetDataModel.id)
  }

  override fun onCancel() {
    androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
      .setTitle(R.string.alert_title_abort)
      .setMessage(R.string.alert_message_abort_operation)
      .setCancelable(false)
      .setNegativeButton(R.string.no) { dialogInterface, _ -> dialogInterface.dismiss() }
      .setPositiveButton(R.string.yes) { dialogInterface, _ ->
        dialogInterface.dismiss()
        finish()
      }
      .show()
  }
}
