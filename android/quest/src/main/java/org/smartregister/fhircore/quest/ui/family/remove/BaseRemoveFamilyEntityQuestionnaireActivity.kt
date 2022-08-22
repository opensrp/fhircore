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

package org.smartregister.fhircore.quest.ui.family.remove

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.compose.material.ExperimentalMaterialApi
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertIntent
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity

abstract class BaseRemoveFamilyEntityQuestionnaireActivity<T> : QuestionnaireActivity() {

  abstract val viewModel: BaseRemoveFamilyEntityViewModel<T>

  private lateinit var btnRemove: Button
  private lateinit var profileId: String
  lateinit var profileName: String
  lateinit var familyId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    profileId = intent.extras?.getString(QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    familyId = intent.extras?.getString(NavigationArg.FAMILY_ID) ?: ""
    btnRemove = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)
    btnRemove.text = setRemoveButtonText()

    viewModel.apply {
      isRemoved.observe(this@BaseRemoveFamilyEntityQuestionnaireActivity) { if (it) onRemove() }
      isDiscarded.observe(this@BaseRemoveFamilyEntityQuestionnaireActivity) { if (it) finish() }
      fetch(profileId)
      profile.observe(this@BaseRemoveFamilyEntityQuestionnaireActivity) { onReceive(it) }
    }
  }

  @OptIn(ExperimentalMaterialApi::class)
  override fun onRemove() {
    val intent =
      Intent(this@BaseRemoveFamilyEntityQuestionnaireActivity, AppMainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    this@BaseRemoveFamilyEntityQuestionnaireActivity.run {
      startActivity(intent)
      finish()
    }
  }

  abstract fun onReceive(profile: T)

  override fun onClick(view: View) {
    if (view.id == org.smartregister.fhircore.engine.R.id.btn_save_client_info) {
      handleQuestionnaireSubmit()
    }
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    dismissSaveProcessing()
    confirmationDialog(profileId = profileId, profileName = profileName)
  }

  private fun confirmationDialog(profileId: String, profileName: String) {
    AlertDialogue.showAlert(
      context = this,
      alertIntent = AlertIntent.CONFIRM,
      title = setRemoveDialogTitle(),
      message = setRemoveDialogMessage(profileName),
      confirmButtonListener = { dialog ->
        viewModel.remove(profileId)
        dialog.dismiss()
      },
      neutralButtonListener = { dialog ->
        viewModel.discard()
        dialog.dismiss()
      }
    )
  }

  abstract fun setRemoveButtonText(): String

  abstract fun setRemoveDialogTitle(): String

  abstract fun setRemoveDialogMessage(profileName: String): String
}
