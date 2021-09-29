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

package org.smartregister.fhircore.anc.ui.madx.details.form

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.madx.PatientBmiRepository
import org.smartregister.fhircore.anc.ui.madx.details.PatientBmiItemMapper
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

class BMIQuestionnaireActivity : QuestionnaireActivity() {

  internal lateinit var patientBmiRepository: PatientBmiRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    patientBmiRepository =
      PatientBmiRepository(AncApplication.getContext().fhirEngine, PatientBmiItemMapper)
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      val isUnitModeMetric = patientBmiRepository.isUnitModeMetric(questionnaireResponse)
      val inputHeight = patientBmiRepository.getInputHeight(questionnaireResponse, isUnitModeMetric)
      val inputWeight = patientBmiRepository.getInputWeight(questionnaireResponse, isUnitModeMetric)
      val computedBMI = patientBmiRepository.calculateBMI(inputHeight, inputWeight, isUnitModeMetric)
//      val computedBMI = patientBmiRepository.computeBMI(questionnaireResponse) // first approach to delete
      if (computedBMI < 0)
        showErrorAlert(getString(R.string.error_saving_form))
      else {
        val patientId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!
        showBmiDataAlert(questionnaireResponse, patientId, inputHeight, inputWeight, computedBMI)
      }
    }
  }

  private fun showBmiDataAlert(
    questionnaireResponse: QuestionnaireResponse,
    patientId: String,
    height: Double,
    weight: Double,
    computedBMI: Double
  ) {
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.your_bmi))
      .setMessage("$computedBMI" + getBMICategories())
      .setCancelable(false)
      .setNegativeButton(R.string.re_compute) { dialogInterface, _ ->
        dialogInterface.dismiss()
      }
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
      patientBmiRepository.recordComputedBMI(
        questionnaire!!,
        questionnaireResponse,
        patientId,
        height,
        weight,
        computedBMI
      )
    }
  }

  private fun showErrorAlert(message: String) {
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.try_again))
      .setMessage(message)
      .setCancelable(true)
      .setPositiveButton("OK") { dialogInterface, _ ->
        dialogInterface.dismiss()
      }
      .show()
  }

  companion object {
    const val KEY_UNIT_SELECTION = "select-mode"
    const val KEY_WEIGHT_SI = "vital-signs-body-wight-s"
    const val KEY_HEIGHT_SI = "vital-signs-height-s"
    const val KEY_HEIGHT_DP_SI = "vital-signs-height-double-prime"
    const val KEY_WEIGHT_MU = "vital-signs-body-wight-m"
    const val KEY_HEIGHT_MU = "vital-signs-height-m"
  }

  // Below BMI Categories information isn't required, it can be omit out
  private fun getBMICategories(): String {
    return "\n\n\nBMI Categories:\n\nUnderweight = <18.5\nNormal weight = 18.5–24.9" +
            "\nOverweight = 25–29.9\nObesity = BMI of 30 or greater"
  }
}
