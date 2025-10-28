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
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.View.FOCUS_DOWN
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.extensions.getRequiredOrOptionalText
import com.google.android.fhir.datacapture.extensions.getValidationErrorMessage
import com.google.android.fhir.datacapture.extensions.localizedFlyoverSpanned
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import com.google.android.fhir.datacapture.views.HeaderView
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemAndroidViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemAndroidViewHolderFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.util.QrCodeScanUtils

internal class EditTextQrCodeItemViewHolderFactory(
  private val qrCodeAnswerChangeListener: QrCodeChangeListener,
) : QuestionnaireItemAndroidViewHolderFactory(R.layout.edit_text_single_line_qr_code_item_view) {
  override fun getQuestionnaireItemViewHolderDelegate() =
    object :
      QuestionnaireItemAndroidViewHolderDelegate {

        override lateinit var questionnaireViewItem: QuestionnaireViewItem

        private lateinit var context: AppCompatActivity
        private lateinit var header: HeaderView
        private lateinit var textInputLayout: TextInputLayout
        private lateinit var textInputEditText: TextInputEditText
        private var textWatcher: TextWatcher? = null

        private fun superInit(itemView: View) {
            context = itemView.context.tryUnwrapContext()!!
            header = itemView.findViewById(R.id.header)
            textInputLayout = itemView.findViewById(R.id.text_input_layout)
            textInputEditText = itemView.findViewById(R.id.text_input_edit_text)

            textInputEditText.setRawInputType(InputType.TYPE_NULL)
            // Override `setOnEditorActionListener` to avoid crash with `IllegalStateException` if it's not
            // possible to move focus forward.
            // See
            // https://stackoverflow.com/questions/13614101/fatal-crash-focus-search-returned-a-view-that-wasnt-able-to-take-focus/47991577
            textInputEditText.setOnEditorActionListener { view, actionId, _ ->
                if (actionId != EditorInfo.IME_ACTION_NEXT) {
                    return@setOnEditorActionListener false
                }
                view.focusSearch(FOCUS_DOWN)?.requestFocus(FOCUS_DOWN) ?: false
            }
            textInputEditText.setOnFocusChangeListener { view, focused ->
                if (!focused) {
                    (view.context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as InputMethodManager)
                        .hideSoftInputFromWindow(view.windowToken, 0)

                    context.lifecycleScope.launch {
                        // Update answer even if the text box loses focus without any change. This will mark the
                        // questionnaire response item as being modified in the view model and trigger validation.
                        handleInput(textInputEditText.editableText, questionnaireViewItem)
                    }
                }
            }
        }

      @SuppressLint("ClickableViewAccessibility")
      override fun init(itemView: View) {
        superInit(itemView)

        val onQrCodeIconClickListener: (Context) -> Unit = {
          it.tryUnwrapContext()?.let { appCompatActivity ->
            QrCodeScanUtils.scanQrCode(appCompatActivity) { code ->
              itemView.findViewById<TextInputEditText>(R.id.text_input_edit_text).setText(code)
            }
          }
        }

        itemView.findViewById<TextInputLayout>(R.id.text_input_layout).apply {
          setEndIconOnClickListener { onQrCodeIconClickListener.invoke(it.context) }
          findViewById<TextInputEditText>(R.id.text_input_edit_text).apply {
            setOnFocusChangeListener { v, hasFocus ->
              if (!hasFocus) {
                (v.context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager)
                  .hideSoftInputFromWindow(v.windowToken, 0)
              }
            }
            setOnTouchListener { v, event,
              ->
              if (event.action == MotionEvent.ACTION_UP) {
                onQrCodeIconClickListener(v.context)
              }
              return@setOnTouchListener false
            }
          }
        }
      }

        override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
            header.bind(questionnaireViewItem)
            with(textInputLayout) {
                hint = questionnaireViewItem.enabledDisplayItems.localizedFlyoverSpanned
                helperText = getRequiredOrOptionalText(questionnaireViewItem, context)
            }

            /**
             * Ensures that any validation errors or warnings are immediately reflected in the UI whenever
             * the view is bound to a new or updated item.
             */
            updateValidationTextUI(questionnaireViewItem, textInputLayout)

            /**
             * Updates the EditText *only* if the EditText is not currently focused.
             *
             * This is done to avoid disrupting the user's typing experience and prevent conflicts if they
             * are actively editing the field. Updating the text programmatically is safe in the following
             * scenarios:
             * 1. **ViewHolder Reuse:** When the same ViewHolder is being used to display a different
             *    QuestionnaireViewItem, the EditText needs to be updated with the new item's content.
             * 2. **Read-Only Items:** When the item is read-only, its value may change dynamically due to
             *    expressions, and the EditText needs to reflect this updated value.
             *
             * The following actions are performed if the EditText is not focused:
             * - Removes any existing text change listener.
             * - Updates the input text UI based on the QuestionnaireViewItem.
             * - Updates the unit text view (if applicable).
             * - Attaches a new text change listener to handle user input.
             */
            if (!textInputEditText.isFocused) {
                textInputEditText.removeTextChangedListener(textWatcher)
                updateInputTextUI(questionnaireViewItem, textInputEditText)

                // TextWatcher is set only once for each question item in scenario 1
                textWatcher =
                    textInputEditText.doAfterTextChanged { editable: Editable? ->
                        context.lifecycleScope.launch { handleInput(editable!!, questionnaireViewItem) }
                    }
            }
        }

        override fun setReadOnly(isReadOnly: Boolean) {
        val questionnaireItemHasAnswer = questionnaireViewItem.answers.any { !it.value.isEmpty }
        val readOnly =
          questionnaireItemHasAnswer && (isReadOnly || questionnaireViewItem.isSetOnceReadOnly)

            textInputLayout.isEnabled = !readOnly
            textInputEditText.isEnabled = !readOnly
      }

      suspend fun handleInput(
        editable: Editable,
        questionnaireViewItem: QuestionnaireViewItem,
      ) {
        val answer =
          editable.toString().let {
            if (it.isBlank()) {
              null
            } else {
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                .setValue(StringType(it))
            }
          }

        qrCodeAnswerChangeListener.onQrCodeChanged(
          questionnaireViewItem.answers.singleOrNull(),
          answer,
        )
      }

      fun updateInputTextUI(
        questionnaireViewItem: QuestionnaireViewItem,
        textInputEditText: TextInputEditText,
      ) {
        val text = questionnaireViewItem.answers.singleOrNull()?.valueStringType?.value ?: ""
        if ((text != textInputEditText.text.toString())) {
          textInputEditText.text?.clear()
          textInputEditText.append(text)
        }
      }

      fun updateValidationTextUI(
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
}
