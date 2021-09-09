package org.smartregister.fhircore.anc.ui.family.form

import kotlinx.serialization.Serializable

@Serializable
data class FamilyFormConfig(
    val memberRegistrationQuestionnaireId: String,
    val memberRegistrationQuestionnaireTitle: String
) {
    companion object {
        const val FAMILY_REGISTER_CONFIG_ID = "family_register_config.json"
    }
}