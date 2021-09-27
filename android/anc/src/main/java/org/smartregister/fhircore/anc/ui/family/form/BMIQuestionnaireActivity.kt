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

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.util.computeBMIViaMetricUnits
import org.smartregister.fhircore.anc.util.computeBMIViaStandardUnits
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.find

class BMIQuestionnaireActivity : QuestionnaireActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {

      val computedBMI = computeBMI(questionnaireResponse)
      if (computedBMI < 0)
        showErrorAlert(getString(R.string.error_saving_form))
      else
        showBMIAlert(computedBMI)
    }
  }

  private fun computeBMI(questionnaireResponse: QuestionnaireResponse): Double {
    try {
      val unitMode = questionnaireResponse.find(KEY_UNIT_SELECTION)
      // for Standard Units
      if (unitMode?.answer?.get(0)?.valueCoding?.code == "standard") {
        val weightPounds = questionnaireResponse.find(KEY_WEIGHT_SI)
        val heightFeets = questionnaireResponse.find(KEY_HEIGHT_SI)
        val heightInches = questionnaireResponse.find(KEY_HEIGHT_DP_SI)

        val weight = weightPounds?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
        val heightInFeets = heightFeets?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
        val heightInInches = heightInches?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
        val height = (heightInFeets.toDouble() * 12) + heightInInches.toDouble()

        return computeBMIViaStandardUnits(height, weight.toDouble())
      } else {
        // for Metric Units
        val weightKgs = questionnaireResponse.find(KEY_WEIGHT_MU)
        val heightCms = questionnaireResponse.find(KEY_HEIGHT_MU)
        val weight = weightKgs?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
        val height = heightCms?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
        return computeBMIViaMetricUnits(height.toDouble() / 100, weight.toDouble())
      }
    } catch (e: Exception) {
      e.printStackTrace()
      return -1.0
    }
  }

  private fun showBMIAlert(computedBMI: Double) {
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.your_bmi))
      .setMessage("$computedBMI" + getBMICategories())
      .setCancelable(false)
      .setNegativeButton(R.string.re_compute) { dialogInterface, _ ->
        dialogInterface.dismiss()
      }
      .setPositiveButton(R.string.str_save) { dialogInterface, _ ->
        dialogInterface.dismiss()
        showErrorAlert("Save Observations")
      }
      .show()
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
