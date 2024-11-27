/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler

@AndroidEntryPoint
class AlertDialogFragment() : DialogFragment() {

  private val alertDialogFragmentArgs by navArgs<AlertDialogFragmentArgs>()
  private val alertDialogViewModel by viewModels<AlertDialogViewModel>()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialogue.showThreeButtonAlert(
      context = requireContext(),
      message = org.smartregister.fhircore.engine.R.string.open_draft_changes_message,
      title = org.smartregister.fhircore.engine.R.string.open_draft_changes_title,
      confirmButtonListener = {
        if (requireContext().getActivity() is QuestionnaireHandler) {
          (requireContext().getActivity() as QuestionnaireHandler).launchQuestionnaire(
            context = requireContext().getActivity()!!.baseContext,
            questionnaireConfig = alertDialogFragmentArgs.questionnaireConfig,
            actionParams = listOf(),
          )
        }
      },
      confirmButtonText =
        org.smartregister.fhircore.engine.R.string.questionnaire_alert_open_draft_button_title,
      neutralButtonListener = {},
      neutralButtonText =
        org.smartregister.fhircore.engine.R.string.questionnaire_alert_neutral_button_title,
      negativeButtonListener = {
        runBlocking {
          alertDialogViewModel.deleteDraft(alertDialogFragmentArgs.questionnaireConfig)
        }
      },
      negativeButtonText =
        org.smartregister.fhircore.engine.R.string.questionnaire_alert_delete_draft_button_title,
    )
  }
}
