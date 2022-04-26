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

package org.smartregister.fhircore.anc.ui.family.details

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.family.removefamily.RemoveFamilyQuestionnaireActivity
import org.smartregister.fhircore.anc.util.getCallerActivity
import org.smartregister.fhircore.anc.util.startFamilyMemberRegistration
import org.smartregister.fhircore.engine.ui.base.AlertDialogListItem
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.getSingleChoiceSelectedKey
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.extractFamilyName
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class FamilyDetailsActivity : BaseMultiLanguageActivity() {

  val familyDetailViewModel by viewModels<FamilyDetailViewModel>()

  private lateinit var familyId: String
  lateinit var familyName: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    familyId = intent.extras?.getString(QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    familyDetailViewModel.apply {
      val familyDetailsActivity = this@FamilyDetailsActivity
      backClicked.observe(familyDetailsActivity, { if (it) finish() })
      memberItemClicked.observe(
        familyDetailsActivity,
        familyDetailsActivity::onFamilyMemberItemClicked
      )
      addNewMember.observe(
        familyDetailsActivity,
        { addNewMember ->
          if (addNewMember) {
            familyDetailsActivity.startFamilyMemberRegistration(familyId)
          }
        }
      )

      changeHead.observe(
        familyDetailsActivity,
        { changeHead ->
          if (changeHead) {
            val eligibleMembers = familyDetailViewModel.familyMembers.othersEligibleForHead()

            if (eligibleMembers.isNullOrEmpty())
              this@FamilyDetailsActivity.showToast(getString(R.string.no_eligible_family_head))
            else
              AlertDialogue.showConfirmAlert(
                context = this@FamilyDetailsActivity,
                message = R.string.change_head_confirm_message,
                title = R.string.change_head_confirm_title,
                confirmButtonListener = familyDetailsActivity::onFamilyHeadChangeRequested,
                confirmButtonText = R.string.change_head_button_title,
                options = eligibleMembers.map { AlertDialogListItem(it.id, it.name) }
              )
          }
        }
      )

      familyDetailViewModel.apply {
        isRemoveFamily.observe(familyDetailsActivity, { if (it) finish() })
        demographics.observe(familyDetailsActivity) {
          it?.let { familyName = it.extractFamilyName() }
        }
      }

      familyDetailViewModel.apply {
        isRemoveFamilyMenuItemClicked.observe(
          familyDetailsActivity,
          {
            if (it) {
              removeFamilyMenuItemClicked(familyId = familyId)
            }
          }
        )
      }
    }

    loadData()

    setContent { AppTheme { FamilyDetailScreen(familyDetailViewModel) } }
  }

  private fun loadData() {
    familyDetailViewModel.run {
      fetchDemographics(familyId)
      fetchFamilyMembers(familyId)
      fetchCarePlans(familyId)
      fetchEncounters(familyId)
    }
  }

  override fun onResume() {
    super.onResume()

    loadData()
  }

  fun getSelectedKey(dialog: DialogInterface): String? {
    return (dialog as AlertDialog).getSingleChoiceSelectedKey()
  }

  private fun onFamilyHeadChangeRequested(dialog: DialogInterface) {
    val selection = getSelectedKey(dialog)
    if (selection?.isNotBlank() == true) {
      familyDetailViewModel
        .changeFamilyHead(familyId, selection)
        .observe(
          this@FamilyDetailsActivity,
          {
            if (it) {
              dialog.dismiss()
              finish()
            }
          }
        )
    } else this.showToast(getString(R.string.invalid_selection))
  }

  private fun onFamilyMemberItemClicked(familyMemberItem: FamilyMemberItem?) {
    if (familyMemberItem != null)
      startActivity(
        Intent(this, PatientDetailsActivity::class.java).apply {
          putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, familyMemberItem.id)
        }
      )
  }

  private fun removeFamilyMenuItemClicked(familyId: String) {
    startActivity(
      Intent(this, RemoveFamilyQuestionnaireActivity::class.java).apply {
        putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = familyId,
            formName = FamilyFormConstants.REMOVE_FAMILY
          )
        )
        putExtra(FamilyQuestionnaireActivity.QUESTIONNAIRE_CALLING_ACTIVITY, getCallerActivity())
        putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, familyId)
      }
    )
  }
}
