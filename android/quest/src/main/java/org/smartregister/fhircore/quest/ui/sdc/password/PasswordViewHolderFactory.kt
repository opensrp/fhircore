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

package org.smartregister.fhircore.quest.ui.sdc.password

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.R
import com.google.android.fhir.datacapture.extensions.itemControlCode
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import com.google.android.fhir.datacapture.views.HeaderView
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemAndroidViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemAndroidViewHolderFactory
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType

object PasswordViewHolderFactory :
  QuestionnaireItemAndroidViewHolderFactory(
    org.smartregister.fhircore.quest.R.layout.password_view,
  ) {
  override fun getQuestionnaireItemViewHolderDelegate() =
    object : QuestionnaireItemAndroidViewHolderDelegate {
      override lateinit var questionnaireViewItem: QuestionnaireViewItem

      private lateinit var header: HeaderView
      private lateinit var passwordEditText: TextInputEditText
      private var passwordTextWatcher: TextWatcher? = null
      private lateinit var appContext: AppCompatActivity

      override fun init(itemView: View) {
        appContext = itemView.context.tryUnwrapContext()!!
        header = itemView.findViewById(R.id.header)

        passwordEditText =
          itemView
            .findViewById<TextInputEditText>(
              org.smartregister.fhircore.quest.R.id.password_edit_text,
            )
            .apply {
              setRawInputType(InputType.TYPE_CLASS_TEXT)

              setOnEditorActionListener { view, actionId, _ ->
                if (actionId != EditorInfo.IME_ACTION_NEXT) {
                  false
                }
                view.focusSearch(View.FOCUS_DOWN)?.requestFocus(View.FOCUS_DOWN) ?: false
              }
              setOnFocusChangeListener { view, focused ->
                if (!focused) {
                  (view.context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE)
                      as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
                }
              }
            }
      }

      override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
        header.bind(questionnaireViewItem)

        passwordEditText.removeTextChangedListener(passwordTextWatcher)
        updateUI()
        passwordTextWatcher =
          passwordEditText.doAfterTextChanged { editable: Editable? ->
            appContext.lifecycleScope.launch { handleInput(editable!!, questionnaireViewItem) }
          }
      }

      override fun setReadOnly(isReadOnly: Boolean) {
        passwordEditText.isEnabled = !isReadOnly
      }

      private suspend fun handleInput(
        editable: Editable,
        questionnaireViewItem: QuestionnaireViewItem,
      ) {
        val input = getValue(editable.toString())
        if (input != null) {
          questionnaireViewItem.setAnswer(input)
        } else {
          questionnaireViewItem.clearAnswer()
        }
      }

      private fun updateUI() {
        val text = questionnaireViewItem.answers.singleOrNull()?.value?.toString() ?: ""
        if ((text != passwordEditText.text.toString())) {
          passwordEditText.setText(text)
        }
      }

      private fun isTextUpdatesRequired(answerText: String, inputText: String): Boolean {
        if (answerText.isEmpty() && inputText.isEmpty()) {
          return false
        }
        if (answerText.isEmpty() || inputText.isEmpty()) {
          return true
        }
        // Avoid shifting focus by updating text field if the values are the same
        return answerText != inputText
      }
    }

  private fun getValue(
    text: String,
  ): QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent? {
    return text.let {
      if (it.isEmpty()) {
        null
      } else {
        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(StringType(it))
      }
    }
  }

  fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
    return questionnaireItem.itemControlCode == PASSWORD_WIDGET_UI_CONTROL_CODE
  }

  private const val PASSWORD_WIDGET_UI_CONTROL_CODE = "password-widget"
}
