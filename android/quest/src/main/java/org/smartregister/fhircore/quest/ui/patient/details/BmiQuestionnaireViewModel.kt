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

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.R

@HiltViewModel
class BmiQuestionnaireViewModel
@Inject
constructor(
  fhirEngine: FhirEngine,
  defaultRepository: DefaultRepository,
  configurationRegistry: ConfigurationRegistry,
  transformSupportServices: TransformSupportServices,
  dispatcherProvider: DispatcherProvider,
  sharedPreferencesHelper: SharedPreferencesHelper,
  libraryEvaluator: LibraryEvaluator
) :
  QuestionnaireViewModel(
    fhirEngine,
    defaultRepository,
    configurationRegistry,
    transformSupportServices,
    dispatcherProvider,
    sharedPreferencesHelper,
    libraryEvaluator
  ) {

  companion object {

    const val KEY_UNIT_SELECTION = "select-mode"
    const val KEY_WEIGHT_KG = "vital-signs-body-wight_kg"
    const val KEY_HEIGHT_CM = "vital-signs-height_cm"

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
}
