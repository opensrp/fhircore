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

package org.smartregister.fhircore.anc.ui.madx.bmicompute

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.madx.BmiPatientRepository
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

class BmiComputeActivity : QuestionnaireActivity() {

  lateinit var bmiComputeViewModel: BmiComputeViewModel
  internal lateinit var patientBmiRepository: BmiPatientRepository
  private var encounterID = QuestionnaireUtils.getUniqueId()
  private lateinit var saveBtn: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    saveBtn = findViewById(org.smartregister.fhircore.engine.R.id.btn_save_client_info)
    saveBtn.text = getString(R.string.compute_bmi)
    patientBmiRepository =
      BmiPatientRepository(AncApplication.getContext().fhirEngine, BmiPatientItemMapper)
    bmiComputeViewModel =
      BmiComputeViewModel.get(this, application as AncApplication, patientBmiRepository)
    encounterID = QuestionnaireUtils.getUniqueId()
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      val isUnitModeMetric = bmiComputeViewModel.isUnitModeMetric(questionnaireResponse)
      val inputHeight = bmiComputeViewModel.getInputHeight(questionnaireResponse, isUnitModeMetric)
      val inputWeight = bmiComputeViewModel.getInputWeight(questionnaireResponse, isUnitModeMetric)
      val computedBMI = bmiComputeViewModel.calculateBMI(inputHeight, inputWeight, isUnitModeMetric)
      if (computedBMI < 0)
        showErrorAlert(getString(R.string.try_again), getString(R.string.error_saving_form))
      else {
        val patientId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!
        val height = bmiComputeViewModel.getHeightAsPerSIUnit(inputHeight, isUnitModeMetric)
        val weight = bmiComputeViewModel.getWeightAsPerSIUnit(inputWeight, isUnitModeMetric)
        showBmiDataAlert(questionnaireResponse, patientId, height, weight, computedBMI)
      }
    }
  }

  private fun showErrorAlert(title: String, message: String) {
    AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setCancelable(true)
      .setPositiveButton("OK") { dialogInterface, _ -> dialogInterface.dismiss() }
      .show()
  }

  private fun exitForm() {
    this.finish()
  }

  private fun showBmiDataAlert(
    questionnaireResponse: QuestionnaireResponse,
    patientId: String,
    height: Double,
    weight: Double,
    computedBMI: Double
  ) {
    val message = bmiComputeViewModel.getBMIResult(computedBMI, this)
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.your_bmi) + " $computedBMI")
      .setMessage(message)
      .setCancelable(false)
      .setNegativeButton(R.string.re_compute) { dialogInterface, _ -> dialogInterface.dismiss() }
      .setPositiveButton(R.string.str_save) { dialogInterface, _ ->
        dialogInterface.dismiss()
        proceedRecordBMI(questionnaireResponse, patientId, height, weight, computedBMI)
      }
      .show()
  }

  private fun proceedRecordBMI(
    questionnaireResponse: QuestionnaireResponse,
    patientId: String,
    height: Double,
    weight: Double,
    computedBMI: Double
  ) {
    lifecycleScope.launch {
      val success =
        bmiComputeViewModel.saveComputedBMI(
          questionnaire!!,
          questionnaireResponse,
          patientId,
          encounterID,
          height,
          weight,
          computedBMI
        )

      if (success) {
        exitForm()
      } else {
        showErrorAlert(getString(R.string.try_again), getString(R.string.error_saving_form))
      }
    }
  }
}
