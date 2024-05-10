package org.smartregister.fhircore.quest.ui.register.customui

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.view.View
import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.fhir.datacapture.validation.ValidationResult
import com.google.android.fhir.datacapture.views.HeaderView
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemEditTextViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.quest.R

object CustomEditTextStringViewHolderFactory : QuestionnaireItemViewHolderFactory(R.layout.custom_text_question_item) {

  private val CONTROL_CODE = "STRING"

  override fun getQuestionnaireItemViewHolderDelegate() = EditTextStringViewHolderDelegate()

  fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
    return questionnaireItem.type == Questionnaire.QuestionnaireItemType.STRING
  }

}


class EditTextStringViewHolderDelegate :
  QuestionnaireItemEditTextViewHolderDelegate(
    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
  ) {
  private lateinit var header: HeaderView
  private lateinit var textInputEditText: TextInputEditText


  override fun init(itemView: View) {
    super.init(itemView)
    header = itemView.findViewById(R.id.header)
    header.visibility = View.GONE
    textInputEditText = itemView.findViewById(R.id.text_input_edit_text)
  }

  override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
    super.bind(questionnaireViewItem)
    header.visibility = View.GONE
    textInputLayout.hint = questionnaireViewItem.questionText
    displayValidationResult(questionnaireViewItem.validationResult)
  }


  override suspend fun handleInput(
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

  override fun updateUI(
    questionnaireViewItem: QuestionnaireViewItem,
    textInputEditText: TextInputEditText,
    textInputLayout: TextInputLayout,
  ) {
    val text = questionnaireViewItem.answers.singleOrNull()?.valueStringType?.value ?: ""
    if ((text != textInputEditText.text.toString())) {
      textInputEditText.setText(text)
    }
  }

  private fun displayValidationResult(validationResult: ValidationResult) {
    textInputLayout.error = getValidationErrorMessage(textInputLayout.context, questionnaireViewItem, validationResult)
  }
}

