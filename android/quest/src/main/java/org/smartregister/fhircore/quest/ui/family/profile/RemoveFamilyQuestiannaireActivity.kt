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

package org.smartregister.fhircore.quest.ui.family.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertIntent
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.main.AppMainActivity

class RemoveFamilyQuestionnaireActivity : QuestionnaireActivity() {

    val removeFamilyViewModel by viewModels<RemoveFamilyViewModel>()

    private lateinit var saveBtn: Button
    private lateinit var familyId: String
    private lateinit var familyName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        familyId = intent.extras?.getString(QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
        saveBtn = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)
        saveBtn.text = getString(R.string.remove_family)

        removeFamilyViewModel.apply {
            isRemoveFamily.observe(this@RemoveFamilyQuestionnaireActivity) {
                if (it) {
                    moveToHomePage()
                }
            }
            discardRemoving.observe(this@RemoveFamilyQuestionnaireActivity) {
                if (it) {
                    discardRemovingAndBackToFamilyDetailPage()
                }
            }
            fetchFamily(familyId = familyId)
            family.observe(this@RemoveFamilyQuestionnaireActivity) {
                it.let { familyName = it.name }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    fun moveToHomePage() {
        val intent =
            Intent(this@RemoveFamilyQuestionnaireActivity, AppMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        this@RemoveFamilyQuestionnaireActivity.run {
            startActivity(intent)
            finish()
        }
    }

    fun discardRemovingAndBackToFamilyDetailPage() {
        finish()
    }


    override fun onClick(view: View) {
        if (view.id == org.smartregister.fhircore.engine.R.id.btn_save_client_info) {
            handleQuestionnaireSubmit()
        }
    }


    override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
        dismissSaveProcessing()
        lifecycleScope.launch { removeFamily(familyId = familyId, familyName = familyName) }
    }

    private fun removeFamily(familyId: String, familyName: String) {
        AlertDialogue.showAlert(
            context = this,
            alertIntent = AlertIntent.CONFIRM,
            message = getString(R.string.remove_family_warning, familyName),
            title = getString(R.string.confirm_remove_family_title),
            confirmButtonListener = { dialog ->
                dialog.dismiss()
                removeFamilyViewModel.removeFamily(familyId = familyId)
            },
            confirmButtonText = R.string.family_register_ok_title,
            neutralButtonListener = { dialog ->
                dialog.dismiss()
                removeFamilyViewModel.discardRemovingFamily()
            }
        )
    }
}
