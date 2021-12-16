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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.util.startAncEnrollment
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.hide

@AndroidEntryPoint
class FamilyQuestionnaireActivity : QuestionnaireActivity() {

  @Inject lateinit var familyRepository: FamilyRepository

  lateinit var saveBtn: Button
  private var isEditFamily: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    isEditFamily = intent.extras?.getBoolean(FamilyFormConstants.FAMILY_EDIT_INFO) ?: false

    saveBtn = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)
    if (isEditFamily) {
      when (intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)!!) {
        FamilyFormConstants.ANC_ENROLLMENT_FORM -> saveBtn.setText(R.string.mark_as_ANC_client)
        FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM ->
          saveBtn.setText(R.string.family_member_update_label)
        FamilyFormConstants.FAMILY_REGISTER_FORM -> saveBtn.setText(R.string.family_update_label)
      }
    } else {
      when (intent.getStringExtra(QUESTIONNAIRE_ARG_FORM)!!) {
        FamilyFormConstants.ANC_ENROLLMENT_FORM -> saveBtn.setText(R.string.mark_as_ANC_client)
        FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM ->
          saveBtn.setText(R.string.family_member_save_label)
        FamilyFormConstants.FAMILY_REGISTER_FORM -> saveBtn.setText(R.string.family_save_label)
      }
    }
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      saveBtn.hide(false)
      if (isEditFamily) {
        when (questionnaireConfig.form) {
          FamilyFormConstants.FAMILY_REGISTER_FORM -> {
            familyRepository.updateProcessFamilyHead(
              intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!,
              questionnaire!!,
              questionnaireResponse
            )
            handlePregnancy(
              intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!,
              questionnaireResponse,
              FamilyFormConstants.FAMILY_REGISTER_FORM
            )
          }
          FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM -> {
            val relatedTo = intent.getStringExtra(QUESTIONNAIRE_RELATED_TO_KEY)
            familyRepository.updateProcessFamilyMember(
              intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!,
              questionnaire!!,
              questionnaireResponse,
              relatedTo
            )
            handlePregnancy(
              intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!,
              questionnaireResponse,
              FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM
            )
          }
        }
      } else {
        when (questionnaireConfig.form) {
          FamilyFormConstants.ANC_ENROLLMENT_FORM -> {
            val patientId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!
            familyRepository.enrollIntoAnc(questionnaire, questionnaireResponse, patientId)
            endActivity()
          }
          FamilyFormConstants.FAMILY_REGISTER_FORM -> {
            val patientId =
              familyRepository.postProcessFamilyHead(questionnaire, questionnaireResponse)
            familyRepository.fhirEngine.load(Patient::class.java, patientId).run {
              questionnaireViewModel.appendOrganizationInfo(this)

              familyRepository.fhirEngine.save(this)
            }
            addOrganizationToPatient(patientId)

            handlePregnancy(
              patientId = patientId,
              questionnaireResponse = questionnaireResponse,
              ancEnrollmentForm = FamilyFormConstants.FAMILY_REGISTER_FORM
            )
          }
          FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM -> {
            val relatedTo = intent.getStringExtra(QUESTIONNAIRE_RELATED_TO_KEY)
            val patientId =
              familyRepository.postProcessFamilyMember(
                questionnaire = questionnaire,
                questionnaireResponse = questionnaireResponse,
                relatedTo = relatedTo
              )
            addOrganizationToPatient(patientId)

            handlePregnancy(
              patientId = patientId,
              questionnaireResponse = questionnaireResponse,
              ancEnrollmentForm = FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM
            )
          }
        }
      }
    }
  }

  suspend fun addOrganizationToPatient(patientId: String) {
    familyRepository.fhirEngine.load(Patient::class.java, patientId).run {
      questionnaireViewModel.appendOrganizationInfo(this)

      familyRepository.fhirEngine.save(this)
    }
  }

  override fun onBackPressed() {
    AlertDialog.Builder(this, R.style.AlertDialogTheme)
      .setMessage(R.string.unsaved_changes_message_alert)
      .setCancelable(false)
      .setNegativeButton(R.string.unsaved_changes_neg) { dialogInterface, _ ->
        dialogInterface.dismiss()
        if (isEditFamily) {
          finish()
        } else {
          if (questionnaireConfig.form == FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM) {
            startActivity(
              Intent(this, FamilyDetailsActivity::class.java).apply {
                putExtra(
                  QUESTIONNAIRE_ARG_PATIENT_KEY,
                  intent.getStringExtra(QUESTIONNAIRE_RELATED_TO_KEY)!!
                )
              }
            )
          }
          finish()
        }
      }
      .setPositiveButton(R.string.unsaved_changes_pos) { dialogInterface, _ ->
        dialogInterface.dismiss()
      }
      .show()
  }

  private fun handlePregnancy(
    patientId: String,
    questionnaireResponse: QuestionnaireResponse,
    ancEnrollmentForm: String
  ) {
    val pregnantItem = questionnaireResponse.find(IS_PREGNANT_KEY)
    val pregnancy = pregnantItem?.answer?.firstOrNull()?.valueBooleanType?.booleanValue()
    if (pregnancy == true) {
      this.startAncEnrollment(patientId)
    } else {
      if (isEditFamily) {
        finish()
      } else {
        if (ancEnrollmentForm == FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM) {
          startActivity(
            Intent(this, FamilyDetailsActivity::class.java).apply {
              putExtra(
                QUESTIONNAIRE_ARG_PATIENT_KEY,
                intent.getStringExtra(QUESTIONNAIRE_RELATED_TO_KEY)!!
              )
            }
          )
          endActivity()
        } else endActivity()
      }
    }
  }

  private fun endActivity() {
    when (intent.getStringExtra(QUESTIONNAIRE_CALLING_ACTIVITY) ?: "") {
      FamilyRegisterActivity::class.java.name ->
        startActivity(Intent(this, FamilyRegisterActivity::class.java))
    }
    finish()
  }

  companion object {
    const val QUESTIONNAIRE_RELATED_TO_KEY = "questionnaire-related-to"
    const val QUESTIONNAIRE_CALLING_ACTIVITY = "questionnaire-calling-activity"
    const val IS_PREGNANT_KEY = "is_pregnant"
  }
}
