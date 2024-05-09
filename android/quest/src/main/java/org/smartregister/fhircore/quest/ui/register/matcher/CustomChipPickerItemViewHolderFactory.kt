package org.smartregister.fhircore.quest.ui.register.matcher

import android.view.View
import android.widget.TextView
import com.google.android.fhir.datacapture.extensions.displayString
import com.google.android.fhir.datacapture.extensions.itemControlCode
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.smartregister.fhircore.quest.R

object CustomChipPickerItemViewHolderFactory : QuestionnaireItemViewHolderFactory(R.layout.custom_chip_item) {

    private val CONTROL_CODE = "-button"

    override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
        object : QuestionnaireItemViewHolderDelegate {
            private lateinit var current: Chip
            private lateinit var past: Chip
            private lateinit var never: Chip
            private lateinit var title: TextView
            override lateinit var questionnaireViewItem: QuestionnaireViewItem

            override fun init(itemView: View) {
                title = itemView.findViewById(R.id.group_question_tile)
                current = itemView.findViewById(R.id.current)
                past = itemView.findViewById(R.id.past)
                never = itemView.findViewById(R.id.never)
            }

            override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
                title.text = questionnaireViewItem.questionText
                if (questionnaireViewItem.enabledAnswerOptions.size == 3){
                    //current.text = questionnaireViewItem.enabledAnswerOptions[0].value
                    current.text = questionnaireViewItem.enabledAnswerOptions[0].value.displayString(current.context)
                    past.text = questionnaireViewItem.enabledAnswerOptions[1].value.displayString(current.context)
                    never.text = questionnaireViewItem.enabledAnswerOptions[2].value.displayString(current.context)

                    current.setOnClickListener {
                        selectChip(current, past, never, questionnaireViewItem)
                    }

                    past.setOnClickListener {
                        selectChip(past, current, never, questionnaireViewItem)
                    }

                    never.setOnClickListener {
                        selectChip(never, current, past, questionnaireViewItem)
                    }
                }
            }

            override fun setReadOnly(isReadOnly: Boolean) {
                current.isEnabled = !isReadOnly
                past.isEnabled = !isReadOnly
                never.isEnabled = !isReadOnly
            }
        }

    fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
        return questionnaireItem.itemControlCode == CONTROL_CODE
    }

    private fun selectChip(
        selectedChip: Chip,
        unselectedChip: Chip,
        unselectedChip2: Chip,
        questionnaireViewItem: QuestionnaireViewItem
    ) {
        selectedChip.isChecked = true
        unselectedChip.isChecked = false
        unselectedChip2.isChecked = false

        CoroutineScope(Dispatchers.IO).launch {
            /*questionnaireViewItem.setAnswer(
                QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = "DecimalType(altitude)"
                },)*/
        }

    }

}