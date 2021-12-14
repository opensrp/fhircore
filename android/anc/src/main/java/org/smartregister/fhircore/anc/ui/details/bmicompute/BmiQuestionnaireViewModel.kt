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

package org.smartregister.fhircore.anc.ui.details.bmicompute

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.anc.util.computeBMIViaMetricUnits
import org.smartregister.fhircore.anc.util.computeBMIViaUSCUnits
import org.smartregister.fhircore.engine.util.extension.find

@HiltViewModel
class BmiQuestionnaireViewModel @Inject constructor(val patientRepository: PatientRepository) :
  ViewModel() {

  init {
    patientRepository.setAncItemMapperType(AncItemMapper.AncItemMapperType.DETAILS)
  }

  companion object {

    const val KEY_UNIT_SELECTION = "select-mode"
    const val KEY_WEIGHT_LB = "vital-signs-body-wight_lb"
    const val KEY_HEIGHT_FT = "vital-signs-height_ft"
    const val KEY_HEIGHT_INCH = "vital-signs-height_in"
    const val KEY_WEIGHT_KG = "vital-signs-body-wight_kg"
    const val KEY_HEIGHT_CM = "vital-signs-height_cm"

    const val HEIGHT_FEET_INCHES_MULTIPLIER = 12
    const val HEIGHT_METER_CENTIMETER_MULTIPLIER = 100
    const val HEIGHT_INCH_METER_MULTIPLIER = 39.3701
    const val WEIGHT_POUND_KG_MULTIPLIER = 2.20462

    const val BMI_CATEGORY_UNDERWEIGHT_MAX_THRESHOLD = 18.5
    const val BMI_CATEGORY_NORMAL_MAX_THRESHOLD = 25
    const val BMI_CATEGORY_OVERWEIGHT_MAX_THRESHOLD = 30

    fun getHeightAsPerMetricUnit(inputHeight: Double, unitModeMetric: Boolean): Double {
      return if (unitModeMetric) {
        inputHeight
      } else {
        inputHeight / HEIGHT_INCH_METER_MULTIPLIER
      }
    }

    fun getWeightAsPerMetricUnit(inputWeight: Double, unitModeMetric: Boolean): Double {
      return if (unitModeMetric) {
        inputWeight
      } else {
        inputWeight / WEIGHT_POUND_KG_MULTIPLIER
      }
    }
  }

  enum class BmiCategory(val value: Int) {
    UNDERWEIGHT(R.string.bmi_category_underweight),
    NORMAL(R.string.bmi_category_normal),
    OVERWEIGHT(R.string.bmi_category_overweight),
    OBESITY(R.string.bmi_category_obesity)
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
      val heightCms = questionnaireResponse.find(KEY_HEIGHT_CM)
      val height = heightCms?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
      val heightInMeters = height.div(HEIGHT_METER_CENTIMETER_MULTIPLIER)
      heightInMeters
    } else {
      val heightFeets = questionnaireResponse.find(KEY_HEIGHT_FT)
      val heightInches = questionnaireResponse.find(KEY_HEIGHT_INCH)
      val heightInFeets = heightFeets?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
      val heightInInches = heightInches?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
      val height =
        (heightInFeets.toDouble() * HEIGHT_FEET_INCHES_MULTIPLIER) + heightInInches.toDouble()
      height
    }
  }

  fun getInputWeight(
    questionnaireResponse: QuestionnaireResponse,
    isUnitModeMetric: Boolean
  ): Double {
    return if (isUnitModeMetric) {
      val weightKgs = questionnaireResponse.find(KEY_WEIGHT_KG)
      weightKgs?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
    } else {
      val weightPounds = questionnaireResponse.find(KEY_WEIGHT_LB)
      weightPounds?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
    }
  }

  fun calculateBmi(height: Double, weight: Double, isUnitModeMetric: Boolean): Double {
    return if (height <= 0 || weight <= 0) -1.0
    else if (isUnitModeMetric)
      computeBMIViaMetricUnits(heightInMeters = height, weightInKgs = weight)
    else computeBMIViaUSCUnits(heightInInches = height, weightInPounds = weight)
  }

  fun getBmiResult(computedBMI: Double, context: Context): SpannableString {
    val message = getBmiCategories(context)
    val matchedCategoryIndex = getBmiResultCategoryIndex(computedBMI)
    val mSpannableString = SpannableString(message)
    val mGreenSpannedText = ForegroundColorSpan(getBmiResultHighlightColor(matchedCategoryIndex))
    mSpannableString.setSpan(
      mGreenSpannedText,
      getStartingIndexInCategories(bmiCategory = matchedCategoryIndex, context = context),
      getEndingIndexInCategories(bmiCategory = matchedCategoryIndex, context = context),
      Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return mSpannableString
  }

  fun getBmiResultHighlightColor(matchedCategoryIndex: BmiCategory): Int {
    return when (matchedCategoryIndex) {
      BmiCategory.NORMAL -> Color.parseColor("#5AAB61")
      else -> Color.RED
    }
  }

  private fun getBmiCategories(context: Context): String {
    return context.getString(
      R.string.bmi_categories_text,
      context.getString(R.string.bmi_categories_label),
      context.getString(R.string.bmi_category_underweight),
      context.getString(R.string.bmi_category_normal),
      context.getString(R.string.bmi_category_overweight),
      context.getString(R.string.bmi_category_obesity)
    )
  }

  fun getBmiResultCategoryIndex(computedBmi: Double): BmiCategory {
    return when {
      computedBmi < BMI_CATEGORY_UNDERWEIGHT_MAX_THRESHOLD -> BmiCategory.UNDERWEIGHT
      computedBmi < BMI_CATEGORY_NORMAL_MAX_THRESHOLD -> BmiCategory.NORMAL
      computedBmi < BMI_CATEGORY_OVERWEIGHT_MAX_THRESHOLD -> BmiCategory.OVERWEIGHT
      else -> BmiCategory.OBESITY
    }
  }

  fun getStartingIndexInCategories(bmiCategory: BmiCategory, context: Context): Int {
    return getBmiCategories(context).indexOf(context.getString(bmiCategory.value))
  }

  fun getEndingIndexInCategories(bmiCategory: BmiCategory, context: Context): Int {
    return getStartingIndexInCategories(bmiCategory = bmiCategory, context = context) +
      context.getString(bmiCategory.value).length
  }

  suspend fun saveComputedBmi(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    patientId: String,
    encounterID: String,
    height: Double,
    weight: Double,
    computedBMI: Double,
    heightUnit: String,
    weightUnit: String,
    bmiUnit: String
  ): Boolean {
    return patientRepository.recordComputedBmi(
      questionnaire,
      questionnaireResponse,
      patientId,
      encounterID,
      height,
      weight,
      computedBMI,
      heightUnit,
      weightUnit,
      bmiUnit
    )
  }
}
