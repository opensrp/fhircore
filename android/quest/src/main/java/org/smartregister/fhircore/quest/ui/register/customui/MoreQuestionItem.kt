package org.smartregister.fhircore.quest.ui.register.customui

import android.content.Context
import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.fhir.datacapture.validation.ValidationResult
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import org.smartregister.fhircore.quest.R

fun getValidationErrorMessage(
  context: Context,
  questionnaireViewItem: QuestionnaireViewItem,
  validationResult: ValidationResult,
): String? {
  return when (validationResult) {
    is NotValidated,
    Valid,
    -> null

    is Invalid -> {
      val validationMessage = questionnaireViewItem
        .questionnaireItem
        .getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/validationtext")?.value?.asStringValue()
        ?: validationResult.getSingleStringValidationMessage()

      if (
        questionnaireViewItem.questionnaireItem.required &&
        questionnaireViewItem.answers.isEmpty()
      ) {
        questionnaireViewItem
          .questionnaireItem
          .getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/requiredtext")?.value?.asStringValue()
          ?: context.getString(R.string.required_text_and_new_line)

      } else {
        validationMessage
      }
    }
  }
}