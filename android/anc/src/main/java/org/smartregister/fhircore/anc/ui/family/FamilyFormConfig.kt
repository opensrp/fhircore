package org.smartregister.fhircore.anc.ui.family

import kotlinx.serialization.Serializable

@Serializable
data class FamilyFormConfig(
  val registerTitle: String,
  val registrationQuestionnaireIdentifier: String,
  val registrationQuestionnaireTitle: String,
  val memberRegistrationQuestionnaireIdentifier: String,
  val memberRegistrationQuestionnaireTitle: String
) {

  companion object {
    const val FAMILY_DETAIL_VIEW_CONFIG_ID = "family_client_register_config.json"
    const val FAMILY_ARG_ITEM_ID = "family_client_item_id"
  }
}
