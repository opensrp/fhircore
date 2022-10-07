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

package org.smartregister.fhircore.engine.ui.questionnaire

import kotlinx.serialization.Serializable

/**
 * Data class to represent a form configuration. Questionnaires are synced from the server
 * @property appId Application id for the questionnaire
 * @property form A unique name for the form as declared in the `form_configurations.json file`
 * @property title The title of the form
 * @property identifier Represents the identifier as synced from the server
 */
@Serializable
data class QuestionnaireConfig(
  val form: String,
  val title: String,
  val identifier: String,
  val saveButtonText: String? = null,
  val setPractitionerDetails: Boolean = true,
  val setOrganizationDetails: Boolean = true,
  val planDefinitions: List<String> = listOf()
)
