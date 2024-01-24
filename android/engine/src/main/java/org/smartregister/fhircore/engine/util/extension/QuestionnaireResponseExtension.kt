package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent

/** Clears the item text in the [QuestionnaireResponse]. */
fun QuestionnaireResponse.clearText() {
    this.item.clearText()
}

/** Clears the text of items in the current list. */
private fun List<QuestionnaireResponseItemComponent>.clearText() {
    this.forEach { itemToClear ->
        itemToClear.text = null
        if (itemToClear.hasItem()) {
            itemToClear.item.clearText()
        }
    }
}