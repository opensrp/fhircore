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

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.madx.BmiPatientRepository
import org.smartregister.fhircore.anc.util.computeBMIViaMetricUnits
import org.smartregister.fhircore.anc.util.computeBMIViaStandardUnits
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.find

class BmiComputeViewModel(
  application: Application,
  val bmiPatientRepository: BmiPatientRepository
) : ViewModel() {

  companion object {
    fun get(
      owner: ViewModelStoreOwner,
      application: AncApplication,
      repository: BmiPatientRepository
    ): BmiComputeViewModel {
      return ViewModelProvider(owner, BmiComputeViewModel(application, repository).createFactory())[
        BmiComputeViewModel::class.java]
    }

    const val KEY_UNIT_SELECTION = "select-mode"
    const val KEY_WEIGHT_SI = "vital-signs-body-wight_lb"
    const val KEY_HEIGHT_SI = "vital-signs-height_ft"
    const val KEY_HEIGHT_DP_SI = "vital-signs-height_in"
    const val KEY_WEIGHT_MU = "vital-signs-body-wight_kg"
    const val KEY_HEIGHT_MU = "vital-signs-height_cm"
  }

  fun isUnitModeMetric(questionnaireResponse: QuestionnaireResponse): Boolean {
    val unitMode = questionnaireResponse.find(KEY_UNIT_SELECTION)
    return (unitMode?.answer?.get(0)?.valueCoding?.code ?: "none" == "metric")
  }

  fun getInputHeight(
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

  fun getInputWeight(
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

  fun getHeightAsPerSIUnit(inputHeight: Double, unitModeMetric: Boolean): Double {
    return if (unitModeMetric) {
      inputHeight * 39.3701
    } else {
      inputHeight
    }
  }

  fun getWeightAsPerSIUnit(inputWeight: Double, unitModeMetric: Boolean): Double {
    return if (unitModeMetric) {
      inputWeight * 2.20462
    } else {
      inputWeight
    }
  }

  fun calculateBMI(height: Double, weight: Double, isUnitModeMetric: Boolean): Double {
    return try {
      if (isUnitModeMetric) computeBMIViaMetricUnits(heightInMeters = height, weightInKgs = weight)
      else computeBMIViaStandardUnits(heightInInches = height, weightInPounds = weight)
    } catch (e: Exception) {
      e.printStackTrace()
      -1.0
    }
  }

  fun getBMIResult(computedBMI: Double, activityContext: Context): SpannableString {
    val message = getBMICategories(activityContext)
    val matchedCategoryIndex = getBMIResultCategoryIndex(computedBMI)
    val mSpannableString = SpannableString(message)
    val mGreenSpannedText = ForegroundColorSpan(getBmiResultHighlightColor(matchedCategoryIndex))
    mSpannableString.setSpan(
      mGreenSpannedText,
      getStartingIndexInCategories(matchedCategoryIndex, activityContext),
      getEndingIndexInCategories(matchedCategoryIndex, activityContext),
      Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return mSpannableString
  }

  private fun getBmiResultHighlightColor(matchedCategoryIndex: Int): Int {
    return when (matchedCategoryIndex) {
      1 -> Color.RED
      2 -> Color.parseColor("#5AAB61")
      3 -> Color.RED
      else -> Color.RED
    }
  }

  private fun getBMICategories(activityContext: Context): String {
    return "\n\n" +
      activityContext.getString(R.string.bmi_categories_label) +
      "\n" +
      activityContext.getString(R.string.bmi_category_1) +
      "\n" +
      activityContext.getString(R.string.bmi_category_2) +
      "\n" +
      activityContext.getString(R.string.bmi_category_3) +
      "\n" +
      activityContext.getString(R.string.bmi_category_4)
  }

  private fun getBMIResultCategoryIndex(computedBMI: Double): Int {
    return when {
      computedBMI < 18.5 -> 1
      computedBMI < 25 -> 2
      computedBMI < 30 -> 3
      else -> 4
    }
  }

  private fun getStartingIndexInCategories(index: Int, activityContext: Context): Int {
    return when (index) {
      1 ->
        getBMICategories(activityContext)
          .indexOf(activityContext.getString(R.string.bmi_category_1))
      2 ->
        getBMICategories(activityContext)
          .indexOf(activityContext.getString(R.string.bmi_category_2))
      3 ->
        getBMICategories(activityContext)
          .indexOf(activityContext.getString(R.string.bmi_category_3))
      else ->
        getBMICategories(activityContext)
          .indexOf(activityContext.getString(R.string.bmi_category_4))
    }
  }

  private fun getEndingIndexInCategories(index: Int, activityContext: Context): Int {
    return when (index) {
      1 ->
        getStartingIndexInCategories(1, activityContext) +
          activityContext.getString(R.string.bmi_category_1).length
      2 ->
        getStartingIndexInCategories(2, activityContext) +
          activityContext.getString(R.string.bmi_category_2).length
      3 ->
        getStartingIndexInCategories(3, activityContext) +
          activityContext.getString(R.string.bmi_category_3).length
      else ->
        getStartingIndexInCategories(4, activityContext) +
          activityContext.getString(R.string.bmi_category_4).length
    }
  }

  suspend fun saveComputedBMI(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    patientId: String,
    encounterID: String,
    height: Double,
    weight: Double,
    computedBMI: Double
  ): Boolean {
    return bmiPatientRepository.recordComputedBMI(
      questionnaire,
      questionnaireResponse,
      patientId,
      encounterID,
      height,
      weight,
      computedBMI
    )
  }
}
