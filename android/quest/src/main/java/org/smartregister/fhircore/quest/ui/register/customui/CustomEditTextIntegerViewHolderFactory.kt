package org.smartregister.fhircore.quest.ui.register.customui

import android.content.Context
import android.icu.number.NumberFormatter
import android.icu.text.DecimalFormat
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.view.View
import androidx.annotation.RequiresApi
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
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.quest.R
import java.util.Locale

object CustomTextIntegerItemViewHolderFactory : QuestionnaireItemViewHolderFactory(R.layout.custom_text_question_item) {


    override fun getQuestionnaireItemViewHolderDelegate() = object :
        QuestionnaireItemEditTextViewHolderDelegate(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
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
            textInputLayout.hint = questionnaireViewItem.questionText
            header.visibility = View.GONE
            displayValidationResult(questionnaireViewItem.validationResult)
        }

        fun displayValidationResult(validationResult: ValidationResult) {
            textInputLayout.error = getValidationErrorMessage(textInputLayout.context, questionnaireViewItem, validationResult)
        }

        override suspend fun handleInput(
            editable: Editable,
            questionnaireViewItem: QuestionnaireViewItem,
        ) {
            val input = editable.toString()
            if (input.isEmpty()) {
                questionnaireViewItem.clearAnswer()
                return
            }

            val inputInteger = input.toIntOrNull()
            if (inputInteger != null) {
                questionnaireViewItem.setAnswer(
                    QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                        .setValue(IntegerType(input)),
                )
            } else {
                questionnaireViewItem.setDraftAnswer(input)
            }
        }

        override fun updateUI(
            questionnaireViewItem: QuestionnaireViewItem,
            textInputEditText: TextInputEditText,
            textInputLayout: TextInputLayout,
        ) {
            val answer =
                questionnaireViewItem.answers.singleOrNull()?.valueIntegerType?.value?.toString()
            val draftAnswer = questionnaireViewItem.draftAnswer?.toString()

            // Update the text on the UI only if the value of the saved answer or draft answer
            // is different from what the user is typing. We compare the two fields as integers to
            // avoid shifting focus if the text values are different, but their integer representation
            // is the same (e.g. "001" compared to "1")
            if (answer.isNullOrEmpty() && draftAnswer.isNullOrEmpty()) {
                textInputEditText.setText("")
            } else if (answer?.toIntOrNull() != textInputEditText.text.toString().toIntOrNull()) {
                textInputEditText.setText(answer)
            } else if (draftAnswer != null && draftAnswer != textInputEditText.text.toString()) {
                textInputEditText.setText(draftAnswer)
            }

            // Update error message if draft answer present
            if (draftAnswer != null) {
                textInputLayout.error =
                    textInputEditText.context.getString(
                        org.smartregister.fhircore.engine.R.string.integer_format_validation_error_msg,
                        formatInteger(Int.MIN_VALUE),
                        formatInteger(Int.MAX_VALUE),
                    )
            }
        }
    }

    private fun formatInteger(value: Int): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            numberFormatter.format(value).toString()
        } else {
            decimalFormat.format(value)
        }
    }

    private val numberFormatter
        @RequiresApi(Build.VERSION_CODES.R) get() = NumberFormatter.withLocale(Locale.getDefault())

    private val decimalFormat
        get() = DecimalFormat.getInstance(Locale.getDefault())


    fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
        return questionnaireItem.type == Questionnaire.QuestionnaireItemType.INTEGER
    }
}
