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

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.fhir.datacapture.contrib.views.barcode.mlkit.md.LiveBarcodeScanningFragment
import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.extensions.getValidationErrorMessage
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemEditTextViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.quest.R

object EditTextQrCodeViewHolderFactory :
  QuestionnaireItemViewHolderFactory(R.layout.edit_text_single_line_qr_code_view) {
  override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
    object :
      QuestionnaireItemEditTextViewHolderDelegate(
        InputType.TYPE_NULL,
      ) {
      override fun init(itemView: View) {
        super.init(itemView)

        val onQrCodeIconClickListener: (Context) -> Unit = {
          it.tryUnwrapContext()?.let { context ->
            context.supportFragmentManager.apply {
              setFragmentResultListener(
                CameraPermissionDialogFragment.RESULT_REQUEST_KEY,
                context,
              ) { _, result ->
                val permissionGranted =
                  result.getBoolean(CameraPermissionDialogFragment.RESULT_REQUEST_KEY, false)
                if (permissionGranted) {
                  showBarcodeScanner(context) { barcode ->
                    itemView
                      .findViewById<TextInputEditText>(R.id.text_input_edit_text)
                      .setText(barcode)
                  }
                } else {
                  Toast.makeText(
                      context,
                      context.getString(R.string.barcode_camera_permission_denied),
                      Toast.LENGTH_SHORT,
                    )
                    .show()
                }
              }

              CameraPermissionDialogFragment()
                .show(this@apply, EditTextQrCodeViewHolderFactory::class.java.simpleName)
            }
          }
        }

        itemView.findViewById<TextInputLayout>(R.id.text_input_layout).apply {
          setEndIconOnClickListener { onQrCodeIconClickListener.invoke(it.context) }
          findViewById<TextInputEditText>(R.id.text_input_edit_text).setOnFocusChangeListener {
            view,
            hasFocus,
            ->
            if (hasFocus) onQrCodeIconClickListener.invoke(view.context)
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

      private fun showBarcodeScanner(
        activity: AppCompatActivity,
        onBarcodeReceived: (String) -> Unit,
      ) {
        activity.supportFragmentManager.apply {
          setFragmentResultListener(
            LiveBarcodeScanningFragment.RESULT_REQUEST_KEY,
            activity,
          ) { _, result ->
            val barcode = result.getString(LiveBarcodeScanningFragment.RESULT_REQUEST_KEY)?.trim()
            if (!barcode.isNullOrBlank()) {
              onBarcodeReceived.invoke(barcode)
            }
          }
          LiveBarcodeScanningFragment()
            .show(
              this@apply,
              EditTextQrCodeViewHolderFactory::class.java.simpleName,
            )
        }
      }
    }

  fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
    return questionnaireItem.getExtensionByUrl(EXTENSION_URL)?.value?.asStringValue() ==
      EXTENSION_VALUE
  }

  private const val EXTENSION_URL =
    "https://github.com/google/android-fhir/StructureDefinition/questionnaire-itemControl"
  private const val EXTENSION_VALUE = "qr_code-widget"
}
