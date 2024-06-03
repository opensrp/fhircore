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

package org.smartregister.fhircore.engine.ui.questionnaire.items

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.extensions.getRequiredOrOptionalText
import com.google.android.fhir.datacapture.extensions.getValidationErrorMessage
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.questionnaire.items.location.LocationPickerView

class LocationPickerViewHolderFactory(
  val customQuestItemDataProvider: CustomQuestItemDataProvider,
) : QuestionnaireItemViewHolderFactory(R.layout.custom_quest_location_picker_item) {

  private lateinit var context: AppCompatActivity

  override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
    object : QuestionnaireItemViewHolderDelegate {

      private lateinit var locationPickerView: LocationPickerView
      override lateinit var questionnaireViewItem: QuestionnaireViewItem

      override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
        locationPickerView.headerView?.bind(questionnaireViewItem)
        locationPickerView.setRequiredOrOptionalText(
          getRequiredOrOptionalText(questionnaireViewItem, context),
        )
        locationPickerView.setType(
          questionnaireViewItem.questionnaireItem
            .getExtensionByUrl(WIDGET_EXTENSION)
            .value
            .asStringValue(),
        )
        locationPickerView.setOnLocationChanged { value ->
          context.lifecycleScope.launch {
            if (value != null) {
              questionnaireViewItem.setAnswer(
                QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(value),
              )
            } else {
              questionnaireViewItem.clearAnswer()
            }
          }
        }
        val initialAnswer = questionnaireViewItem.answers.singleOrNull()?.valueStringType
        locationPickerView.initLocation(initialAnswer?.value)
        if (questionnaireViewItem.draftAnswer == null) {
          locationPickerView.showError(
            getValidationErrorMessage(
              context,
              questionnaireViewItem,
              questionnaireViewItem.validationResult,
            ),
          )
        }
      }

      override fun init(itemView: View) {
        context = itemView.context.tryUnwrapContext()!!
        locationPickerView = LocationPickerView(itemView.context, itemView, context.lifecycleScope)
        locationPickerView.setCustomDataProvider(customQuestItemDataProvider)
      }

      override fun setReadOnly(isReadOnly: Boolean) {
        locationPickerView.setEnabled(!isReadOnly)
      }
    }

  companion object {
    const val WIDGET_EXTENSION = "https://d-tree.org/fhir/extensions/location-widget"
    const val WIDGET_TYPE = "location-widget"
    const val WIDGET_TYPE_ALL = "location-widget-all"
  }
}
