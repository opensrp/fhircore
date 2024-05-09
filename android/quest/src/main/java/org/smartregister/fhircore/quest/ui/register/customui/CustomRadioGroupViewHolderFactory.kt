package org.smartregister.fhircore.quest.ui.register.customui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.extensions.ChoiceOrientationTypes
import com.google.android.fhir.datacapture.extensions.choiceOrientation
import com.google.android.fhir.datacapture.extensions.displayString
import com.google.android.fhir.datacapture.extensions.itemAnswerOptionImage
import com.google.android.fhir.datacapture.extensions.itemControlCode
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.fhir.datacapture.validation.ValidationResult
import com.google.android.fhir.datacapture.views.HeaderView
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.quest.R

object CustomRadioGroupViewHolderFactory : QuestionnaireItemViewHolderFactory(R.layout.custom_chip_group_view) {

    override fun getQuestionnaireItemViewHolderDelegate() =
        object : QuestionnaireItemViewHolderDelegate {
            private lateinit var appContext: AppCompatActivity
            private lateinit var header: HeaderView
            private lateinit var radioGroup: ConstraintLayout
            private lateinit var flow: Flow

            override lateinit var questionnaireViewItem: QuestionnaireViewItem

            override fun init(itemView: View) {
                appContext = itemView.context.tryUnwrapContext()!!
                header = itemView.findViewById(R.id.header)
                radioGroup = itemView.findViewById(R.id.radio_group)
                flow = itemView.findViewById(R.id.flow)
            }

            override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
                header.bind(questionnaireViewItem)
                header.showRequiredOrOptionalTextInHeaderView(questionnaireViewItem)
                // Keep the Flow layout which is the first child
                radioGroup.removeViews(1, radioGroup.childCount - 1)
                val choiceOrientation =
                    questionnaireViewItem.questionnaireItem.choiceOrientation
                        ?: ChoiceOrientationTypes.VERTICAL
                when (choiceOrientation) {
                    ChoiceOrientationTypes.HORIZONTAL -> {
                        flow.setOrientation(Flow.HORIZONTAL)
                        flow.setWrapMode(Flow.WRAP_CHAIN)
                    }
                    ChoiceOrientationTypes.VERTICAL -> {
                        flow.setOrientation(Flow.VERTICAL)
                        flow.setWrapMode(Flow.WRAP_NONE)
                    }
                }
                questionnaireViewItem.enabledAnswerOptions
                    .map { answerOption -> View.generateViewId() to answerOption }
                    .onEach { populateViewWithAnswerOption(it.first, it.second, choiceOrientation) }
                    .map { it.first }
                    .let { flow.referencedIds = it.toIntArray() }
                displayValidationResult(questionnaireViewItem.validationResult)
            }

            private fun displayValidationResult(validationResult: ValidationResult) {
                when (validationResult) {
                    is NotValidated,
                    Valid, -> header.showErrorText(isErrorTextVisible = false)
                    is Invalid -> {
                        header.showErrorText(errorText = validationResult.getSingleStringValidationMessage())
                    }
                }
            }

            override fun setReadOnly(isReadOnly: Boolean) {
                // The Flow layout has index 0. The radio button indices start from 1.
                for (i in 1 until radioGroup.childCount) {
                    val view = radioGroup.getChildAt(i)
                    view.isEnabled = !isReadOnly
                }
            }

            private fun populateViewWithAnswerOption(
                viewId: Int,
                answerOption: Questionnaire.QuestionnaireItemAnswerOptionComponent,
                choiceOrientation: ChoiceOrientationTypes,
            ) {
                val radioButtonItem =
                        LayoutInflater.from(radioGroup.context).inflate(R.layout.custom_radio_button, null)
                var isCurrentlySelected = questionnaireViewItem.isAnswerOptionSelected(answerOption)
                val radioButton =
                    radioButtonItem.findViewById<Chip>(R.id.radio_button).apply {
                        id = viewId
                        text = answerOption.value.displayString(header.context)
                        setCompoundDrawablesRelative(
                            answerOption.itemAnswerOptionImage(radioGroup.context),
                            null,
                            null,
                            null,
                        )
                        layoutParams =
                            ViewGroup.LayoutParams(
                                when (choiceOrientation) {
                                    ChoiceOrientationTypes.HORIZONTAL -> ViewGroup.LayoutParams.WRAP_CONTENT
                                    ChoiceOrientationTypes.VERTICAL -> ViewGroup.LayoutParams.MATCH_PARENT
                                },
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        isChecked = isCurrentlySelected
                        setOnClickListener { radioButton ->
                            appContext.lifecycleScope.launch {
                                isCurrentlySelected = !isCurrentlySelected
                                when (isCurrentlySelected) {
                                    true -> {
                                        updateAnswer(answerOption)
                                        val buttons = radioGroup.children.asIterable().filterIsInstance<Chip>()
                                        buttons.forEach { button -> uncheckIfNotButtonId(radioButton.id, button) }
                                    }
                                    false -> {
                                        questionnaireViewItem.clearAnswer()
                                        (radioButton as Chip).isChecked = false
                                    }
                                }
                            }
                        }
                    }
                radioGroup.addView(radioButton)
                flow.addView(radioButton)
            }

            private fun uncheckIfNotButtonId(checkedId: Int, button: Chip) {
                if (button.id != checkedId) button.isChecked = false
            }

            private suspend fun updateAnswer(
                answerOption: Questionnaire.QuestionnaireItemAnswerOptionComponent,
            ) {
                questionnaireViewItem.setAnswer(
                    QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                        value = answerOption.value
                    },
                )
            }
        }


    fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
        return questionnaireItem.itemControlCode == "chip"
    }

}


