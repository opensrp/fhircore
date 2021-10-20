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
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.util.computeBMIViaMetricUnits
import org.smartregister.fhircore.anc.util.computeBMIViaStandardUnits
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.find

class BmiQuestionnaireViewModel(
  application: Application,
  private val bmiPatientRepository: PatientRepository
) : ViewModel() {

  companion object {
    fun get(
      owner: ViewModelStoreOwner,
      application: AncApplication,
      repository: PatientRepository
    ): BmiQuestionnaireViewModel {
      return ViewModelProvider(
        owner,
        BmiQuestionnaireViewModel(application, repository).createFactory()
      )[BmiQuestionnaireViewModel::class.java]
    }

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

  fun getHeightAsPerSiUnit(inputHeight: Double, unitModeMetric: Boolean): Double {
    return if (unitModeMetric) {
      inputHeight * HEIGHT_INCH_METER_MULTIPLIER
    } else {
      inputHeight
    }
  }

  fun getWeightAsPerSiUnit(inputWeight: Double, unitModeMetric: Boolean): Double {
    return if (unitModeMetric) {
      inputWeight * WEIGHT_POUND_KG_MULTIPLIER
    } else {
      inputWeight
    }
  }

  fun calculateBmi(height: Double, weight: Double, isUnitModeMetric: Boolean): Double {
    return try {
      if (isUnitModeMetric) computeBMIViaMetricUnits(heightInMeters = height, weightInKgs = weight)
      else computeBMIViaStandardUnits(heightInInches = height, weightInPounds = weight)
    } catch (e: Exception) {
      e.printStackTrace()
      -1.0
    }
  }

  fun getBmiResult(computedBMI: Double, activityContext: Context): SpannableString {
    val message = getBmiCategories(activityContext)
    val matchedCategoryIndex = getBmiResultCategoryIndex(computedBMI)
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

  private fun getBmiResultHighlightColor(matchedCategoryIndex: BmiCategory): Int {
    return when (matchedCategoryIndex) {
      BmiCategory.NORMAL -> Color.parseColor("#5AAB61")
      else -> Color.RED
    }
  }

  private fun getBmiCategories(activityContext: Context): String {
    return "\n\n" +
      activityContext.getString(R.string.bmi_categories_label) +
      "\n" +
      activityContext.getString(R.string.bmi_category_underweight) +
      "\n" +
      activityContext.getString(R.string.bmi_category_normal) +
      "\n" +
      activityContext.getString(R.string.bmi_category_overweight) +
      "\n" +
      activityContext.getString(R.string.bmi_category_obesity)
  }

  private fun getBmiResultCategoryIndex(computedBmi: Double): BmiCategory {
    return when {
      computedBmi < BMI_CATEGORY_UNDERWEIGHT_MAX_THRESHOLD -> BmiCategory.UNDERWEIGHT
      computedBmi < BMI_CATEGORY_NORMAL_MAX_THRESHOLD -> BmiCategory.NORMAL
      computedBmi < BMI_CATEGORY_OVERWEIGHT_MAX_THRESHOLD -> BmiCategory.OVERWEIGHT
      else -> BmiCategory.OBESITY
    }
  }

  private fun getActivityContextStringForBmiCategory(
    bmiCategory: BmiCategory,
    activityContext: Context
  ): String {
    return activityContext.getString(bmiCategory.value)
  }

  private fun getStartingIndexInCategories(
    bmiCategory: BmiCategory,
    activityContext: Context
  ): Int {
    return getBmiCategories(activityContext)
      .indexOf(getActivityContextStringForBmiCategory(bmiCategory, activityContext))
  }

  private fun getEndingIndexInCategories(bmiCategory: BmiCategory, activityContext: Context): Int {
    return getStartingIndexInCategories(bmiCategory, activityContext) +
      getBmiCategories(activityContext)
        .indexOf(getActivityContextStringForBmiCategory(bmiCategory, activityContext))
  }

  suspend fun saveComputedBmi(
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
