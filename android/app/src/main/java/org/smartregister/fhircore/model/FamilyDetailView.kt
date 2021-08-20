/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.model

import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.UriClientParam
import java.io.Serializable

data class FamilyDetailView(
  val registerTitle: String,
  val registrationQuestionnaireIdentifier: String,
  val registrationQuestionnaireTitle: String,
  val memberRegistrationQuestionnaireIdentifier: String,
  val memberRegistrationQuestionnaireTitle: String,
  val registerGroupCountField: String
) : Serializable {

  companion object {
    const val FAMILY_DETAIL_VIEW_CONFIG_ID = "family_client_register_config.json"
    const val FAMILY_ARG_ITEM_ID = "family_client_item_id"
  }
}
