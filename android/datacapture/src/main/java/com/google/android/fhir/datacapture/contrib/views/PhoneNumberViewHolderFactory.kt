/*
 * Copyright 2022-2023 Google LLC
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

package com.google.android.fhir.datacapture.contrib.views

import android.text.InputType
import com.google.android.fhir.datacapture.R
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemEditTextViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType

object PhoneNumberViewHolderFactory :
  QuestionnaireItemViewHolderFactory(R.layout.edit_text_single_line_view) {
  override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
    object : QuestionnaireItemEditTextViewHolderDelegate(InputType.TYPE_CLASS_PHONE) {

      override suspend fun handleInputText(
        input: String?,
        questionnaireViewItem: QuestionnaireViewItem,
      ) {
        input?.let { getValue(input) }?.let { questionnaireViewItem.setAnswer(it) }
          ?: questionnaireViewItem.clearAnswer()
      }

      private fun getValue(
        text: String,
      ): QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent? {
        return text.let {
          if (it.isEmpty()) {
            null
          } else {
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
              .setValue(StringType(it))
          }
        }
      }

      override fun updateUI(
        questionnaireViewItem: QuestionnaireViewItem,
        textInputEditText: TextInputEditText,
        textInputLayout: TextInputLayout,
      ) {
        val text =
          questionnaireViewItem.answers.singleOrNull()?.valueStringType?.value?.toString() ?: ""
        if (text != textInputEditText.text.toString()) {
          textInputEditText.setText(text)
        }
      }
    }
}