package org.smartregister.fhircore.quest.ui.register.customui

import android.view.View
import com.google.android.fhir.datacapture.extensions.itemControlCode
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.hl7.fhir.r4.model.Questionnaire
import org.smartregister.fhircore.quest.R

object CustomDatePickerItemViewHolderFactory : QuestionnaireItemViewHolderFactory(R.layout.custom_number_question_item) {

    private val CONTROL_CODE = "INTEGER"

    override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
        object : QuestionnaireItemViewHolderDelegate {
            private lateinit var textLayout: TextInputLayout
            private lateinit var editText: TextInputEditText
            override lateinit var questionnaireViewItem: QuestionnaireViewItem

            override fun init(itemView: View) {
                textLayout = itemView.findViewById(R.id.phoneNumberInputLayout)
                editText = itemView.findViewById(R.id.phoneNumberEditText)
            }

            override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
                editText.hint = questionnaireViewItem.questionText
            }

            override fun setReadOnly(isReadOnly: Boolean) {
                editText.isEnabled = true
            }
        }

    fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
        return questionnaireItem.type == Questionnaire.QuestionnaireItemType.DATE
    }

}