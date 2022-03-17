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

package org.smartregister.fhircore.anc.ui.details.removefamilymember

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

class RemoveFamilyQuestionnaireActivity : QuestionnaireActivity() {

  val removeFamilyViewModel by viewModels<RemoveFamilyViewModel>()

  private lateinit var saveBtn: Button
  private lateinit var familyId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    familyId = intent.extras?.getString(QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    saveBtn = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)
    saveBtn.text = getString(R.string.remove_family)

    removeFamilyViewModel.apply {
      isRemoveFamily.observe(this@RemoveFamilyQuestionnaireActivity) {
        if (it) {
          Log.e("aw=test", "finishing now")
          finish()
        }
      }
    }
  }

  override fun onClick(view: View) {
    if (view.id == org.smartregister.fhircore.engine.R.id.btn_save_client_info) {
      handleQuestionnaireSubmit()
    }
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch { removeFamilyMember(familyId = familyId) }
  }

  private fun removeFamilyMember(familyId: String) {
    AlertDialogue.showConfirmAlert(
      this,
      R.string.remove_family_warning,
      R.string.confirm_remove_family_title,
      { removeFamilyViewModel.removeFamily(familyId = familyId) },
      R.string.family_register_ok_title
    )
  }
}
