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

package org.smartregister.fhircore.quest.ui.patient.details

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertIntent
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.R

class BmiQuestionnaireActivity : QuestionnaireActivity() {

  override val questionnaireViewModel by viewModels<BmiQuestionnaireViewModel>()
//  private lateinit var saveBtn: Button
//
//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    saveBtn = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)
//    saveBtn.text = getString(R.string.compute_bmi)
//  }

  override fun onClick(view: View) {
    if (view.id == org.smartregister.fhircore.engine.R.id.btn_save_client_info) {
      handleQuestionnaireSubmit()
    }
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      val bundle =
        questionnaireViewModel.performExtraction(
          this@BmiQuestionnaireActivity,
          questionnaire,
          questionnaireResponse
        )

      if (bundle.entry.size > 3 && bundle.entry[3].resource is Observation) {
        val computedBMI =
          ((bundle.entry[3].resource as Observation).value as Quantity).value.toDouble()
        if (computedBMI < 0) {
          showErrorAlert(getString(R.string.try_again), getString(R.string.error_saving_form))
        } else {
          val patientId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!
          showBmiDataAlert(patientId, questionnaireResponse, computedBMI)
        }
      } else {
        showErrorAlert(getString(R.string.try_again), getString(R.string.error_saving_form))
      }
    }
  }

  private fun showErrorAlert(title: String, message: String) {
    AlertDialogue.showAlert(
      this,
      AlertIntent.ERROR,
      message,
      title,
      { dialog ->
        dialog.dismiss()
        resumeForm()
      },
      android.R.string.ok,
      neutralButtonText = android.R.string.cancel
    )
  }

  private fun resumeForm() {
    dismissSaveProcessing()
  }

  private fun exitForm() {
    this.finish()
  }

  private fun showBmiDataAlert(
    patientId: String,
    questionnaireResponse: QuestionnaireResponse,
    computedBMI: Double
  ) {
    val message = questionnaireViewModel.getBmiResult(computedBMI, this)

    AlertDialogue.showAlert(
      this,
      AlertIntent.INFO,
      message,
      getString(R.string.your_bmi) + " $computedBMI",
      { dialogInterface ->
        dialogInterface.dismiss()

        lifecycleScope.launch {
          questionnaireViewModel.extractAndSaveResources(
            this@BmiQuestionnaireActivity,
            patientId,
            questionnaire = questionnaire,
            questionnaireResponse = questionnaireResponse
          )
          exitForm()
        }
      },
      R.string.str_save,
      { dialogInterface ->
        dialogInterface.dismiss()
        resumeForm()
      },
      R.string.re_compute,
    )
  }
}
