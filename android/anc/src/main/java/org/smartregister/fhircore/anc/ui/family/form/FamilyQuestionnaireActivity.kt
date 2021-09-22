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

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

class FamilyQuestionnaireActivity : QuestionnaireActivity() {
  internal lateinit var familyRepository: FamilyRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val saveBtn = findViewById<Button>(org.smartregister.fhircore.engine.R.id.btn_save_client_info)

    when(intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)!!){
      FamilyFormConstants.ANC_ENROLLMENT_FORM -> saveBtn.setText(R.string.mark_as_ANC_client)
      FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM -> saveBtn.setText(R.string.family_member_save_label)
      FamilyFormConstants.FAMILY_REGISTER_FORM -> saveBtn.setText(R.string.family_save_label)
    }

    familyRepository = FamilyRepository(AncApplication.getContext().fhirEngine, FamilyItemMapper)
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      when (questionnaireConfig.form) {
        FamilyFormConstants.ANC_ENROLLMENT_FORM -> {
          val patientId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!
          familyRepository.enrollIntoAnc(questionnaire!!, questionnaireResponse, patientId)
          reloadList()
        }
        FamilyFormConstants.FAMILY_REGISTER_FORM -> {
          val patientId =
            familyRepository.postProcessFamilyMember(questionnaire!!, questionnaireResponse, null)
          showFamilyMemberRegistrationConfirm(patientId)
        }
        FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM -> {
          val relatedTo = intent.getStringExtra(QUESTIONNAIRE_RELATED_TO_KEY)
          familyRepository.postProcessFamilyMember(
            questionnaire!!,
            questionnaireResponse,
            relatedTo
          )
          showFamilyMemberRegistrationConfirm(relatedTo!!)
        }
        else -> throw IllegalStateException("Invalid flow of app")
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

  private fun showFamilyMemberRegistrationConfirm(headId: String) {
    AlertDialog.Builder(this)
      .setMessage(R.string.family_register_message_alert)
      .setCancelable(false)
      .setNegativeButton(R.string.family_register_cancel_title) { dialogInterface, _ ->
        dialogInterface.dismiss()
        reloadList()
      }
      .setPositiveButton(R.string.family_register_ok_title) { dialogInterface, _ ->
        dialogInterface.dismiss()
        registerMember(headId!!)
      }
      .show()
  }

  fun reloadList() {
    startActivity(Intent(this, FamilyRegisterActivity::class.java))
  }

  fun registerMember(headId: String) {
    val bundle =
      requiredIntentArgs(
        clientIdentifier = null,
        form = FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM
      )
    bundle.putString(QUESTIONNAIRE_RELATED_TO_KEY, headId)
    startActivity(Intent(this, FamilyQuestionnaireActivity::class.java).putExtras(bundle))
  }

  companion object {
    const val QUESTIONNAIRE_RELATED_TO_KEY = "questionnaire-related-to"
  }
}
