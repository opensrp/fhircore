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
import org.smartregister.fhircore.anc.data.madx.BmiPatientRepository
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils
import org.smartregister.fhircore.anc.ui.madx.details.PatientBmiItemMapper
import org.smartregister.fhircore.anc.util.computeBMIViaMetricUnits
import org.smartregister.fhircore.anc.util.computeBMIViaStandardUnits
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.find

class BMIQuestionnaireActivity : QuestionnaireActivity() {

  private lateinit var patientBmiRepository: BmiPatientRepository
  private var encounterID = QuestionnaireUtils.getUniqueId()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    patientBmiRepository =
      BmiPatientRepository(AncApplication.getContext().fhirEngine, PatientBmiItemMapper)
    encounterID = QuestionnaireUtils.getUniqueId()
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      val isUnitModeMetric = isUnitModeMetric(questionnaireResponse)
      val inputHeight = getInputHeight(questionnaireResponse, isUnitModeMetric)
      val inputWeight = getInputWeight(questionnaireResponse, isUnitModeMetric)
      val computedBMI = calculateBMI(inputHeight, inputWeight, isUnitModeMetric)
      if (computedBMI < 0) showErrorAlert(getString(R.string.error_saving_form))
      else {
        val patientId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)!!
        val height = getHeightAsPerSIUnit(inputHeight, isUnitModeMetric)
        val weight = getWeightAsPerSIUnit(inputWeight, isUnitModeMetric)
        showBmiDataAlert(questionnaireResponse, patientId, height, weight, computedBMI)
      }
    }
  }

  private fun showErrorAlert(message: String) {
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.try_again))
      .setMessage(message)
      .setCancelable(true)
      .setPositiveButton("OK") { dialogInterface, _ -> dialogInterface.dismiss() }
      .show()
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
        patientBmiRepository.recordComputedBMI(
          questionnaire!!,
          questionnaireResponse,
          patientId,
          encounterID,
          height,
          weight,
          computedBMI
        )
      showErrorAlert("saving bmi record success = $success")
    }
  }

  private fun isUnitModeMetric(questionnaireResponse: QuestionnaireResponse): Boolean {
    val unitMode = questionnaireResponse.find(KEY_UNIT_SELECTION)
    return (unitMode?.answer?.get(0)?.valueCoding?.code ?: "none" == "metric")
  }

  private fun getInputHeight(
    questionnaireResponse: QuestionnaireResponse,
    isUnitModeMetric: Boolean
  ): Double {
    return if (isUnitModeMetric) {
      val heightCms = questionnaireResponse.find(KEY_HEIGHT_MU)
      val height = heightCms?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
      val heightInMeters = height.div(100)
      heightInMeters
    } else {
      val heightFeets = questionnaireResponse.find(KEY_HEIGHT_SI)
      val heightInches = questionnaireResponse.find(KEY_HEIGHT_DP_SI)
      val heightInFeets = heightFeets?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
      val heightInInches = heightInches?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
      val height = (heightInFeets.toDouble() * 12) + heightInInches.toDouble()
      height
    }
  }

  private fun getInputWeight(
    questionnaireResponse: QuestionnaireResponse,
    isUnitModeMetric: Boolean
  ): Double {
    return if (isUnitModeMetric) {
      val weightKgs = questionnaireResponse.find(KEY_WEIGHT_MU)
      weightKgs?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
    } else {
      val weightPounds = questionnaireResponse.find(KEY_WEIGHT_SI)
      weightPounds?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
    }
  }

  private fun getHeightAsPerSIUnit(inputHeight: Double, unitModeMetric: Boolean): Double {
    return if (unitModeMetric) {
      inputHeight * 39.3701
    } else {
      inputHeight
    }
  }

  private fun getWeightAsPerSIUnit(inputWeight: Double, unitModeMetric: Boolean): Double {
    return if (unitModeMetric) {
      inputWeight * 2.20462
    } else {
      inputWeight
    }
  }

  private fun calculateBMI(height: Double, weight: Double, isUnitModeMetric: Boolean): Double {
    return try {
      if (isUnitModeMetric) computeBMIViaMetricUnits(heightInMeters = height, weightInKgs = weight)
      else computeBMIViaStandardUnits(heightInInches = height, weightInPounds = weight)
    } catch (e: Exception) {
      e.printStackTrace()
      -1.0
    }
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
