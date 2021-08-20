package org.smartregister.fhircore.util

import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

fun Questionnaire.find(linkId: String): Questionnaire.QuestionnaireItemComponent? {
    return item.find(linkId, null)
}

private fun List<Questionnaire.QuestionnaireItemComponent>.find(
    linkId: String,
    default: Questionnaire.QuestionnaireItemComponent?
): Questionnaire.QuestionnaireItemComponent? {
    var result = default
    run loop@{
        forEach {
            if (it.linkId == linkId) {
                result = it
                return@loop
            } else if (it.item.isNotEmpty()) {
                result = it.item.find(linkId, result)
            }
        }
    }

    return result
}

fun QuestionnaireResponse.find(linkId: String): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
    return item.find(linkId, null)
}

private fun List<QuestionnaireResponse.QuestionnaireResponseItemComponent>.find(
    linkId: String,
    default: QuestionnaireResponse.QuestionnaireResponseItemComponent?
): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
    var result = default
    run loop@{
        forEach {
            if (it.linkId == linkId) {
                result = it
                return@loop
            } else if (it.item.isNotEmpty()) {
                result = it.item.find(linkId, result)
            }
        }
    }

    return result
}