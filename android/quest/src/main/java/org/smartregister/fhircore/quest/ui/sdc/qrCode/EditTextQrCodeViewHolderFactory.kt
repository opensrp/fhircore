/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.sdc.qrCode

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import com.google.android.fhir.datacapture.extensions.getValidationErrorMessage
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemEditTextViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.util.QrCodeScanUtils

object EditTextQrCodeViewHolderFactory :
  QuestionnaireItemViewHolderFactory(R.layout.edit_text_single_line_qr_code_view) {
  override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
    object :
      QuestionnaireItemEditTextViewHolderDelegate(
        InputType.TYPE_NULL,
      ) {
      @SuppressLint("ClickableViewAccessibility")
      override fun init(itemView: View) {
        super.init(itemView)

        val onQrCodeIconClickListener: (Context) -> Unit = {
          it.tryUnwrapContext()?.let { appCompatActivity ->
            QrCodeScanUtils.scanQrCode(appCompatActivity) { code ->
              itemView.findViewById<TextInputEditText>(R.id.text_input_edit_text).setText(code)
            }
          }
        }

        itemView.findViewById<TextInputLayout>(R.id.text_input_layout).apply {
          setEndIconOnClickListener { onQrCodeIconClickListener.invoke(it.context) }
          findViewById<TextInputEditText>(R.id.text_input_edit_text).setOnTouchListener { v, event,
            ->
            if (event.action == MotionEvent.ACTION_UP) {
              onQrCodeIconClickListener(v.context)
            }
            return@setOnTouchListener false
          }
        }
      }

      override suspend fun handleInput(
        editable: Editable,
        questionnaireViewItem: QuestionnaireViewItem,
      ) {
        val answer =
          editable.toString().let {
            if (it.isBlank()) {
              null
            } else
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                .setValue(StringType(it))
          }

        if (answer != null) {
          questionnaireViewItem.setAnswer(answer)
        } else questionnaireViewItem.clearAnswer()
      }

      override fun updateInputTextUI(
        questionnaireViewItem: QuestionnaireViewItem,
        textInputEditText: TextInputEditText,
      ) {
        val text = questionnaireViewItem.answers.singleOrNull()?.valueStringType?.value ?: ""
        if ((text != textInputEditText.text.toString())) {
          textInputEditText.text?.clear()
          textInputEditText.append(text)
        }
      }

      override fun updateValidationTextUI(
        questionnaireViewItem: QuestionnaireViewItem,
        textInputLayout: TextInputLayout,
      ) {
        textInputLayout.error =
          getValidationErrorMessage(
            textInputLayout.context,
            questionnaireViewItem,
            questionnaireViewItem.validationResult,
          )
      }
    }

  fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
    val codeableConcept =
      questionnaireItem.getExtensionByUrl(EXTENSION_URL)?.value as? CodeableConcept
    return codeableConcept?.coding?.firstOrNull { it.system == EXTENSION_CONTROL_SYSTEM }?.code ==
      EXTENSION_VALUE
  }

  private const val EXTENSION_URL =
    "https://github.com/opensrp/android-fhir/StructureDefinition/questionnaire-itemControl"
  private const val EXTENSION_CONTROL_SYSTEM =
    "https://github.com/opensrp/android-fhir/questionnaire-item-control"
  private const val EXTENSION_VALUE = "qr_code-widget"
}
