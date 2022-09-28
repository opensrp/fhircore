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

package org.smartregister.fhircore.engine.configuration

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
data class QuestionnaireConfig(
  val id: String,
  val title: String? = null,
  val saveButtonText: String? = null,
  val setPractitionerDetails: Boolean = true,
  val setOrganizationDetails: Boolean = true,
  val planDefinitions: List<String>? = null,
  var type: QuestionnaireType = QuestionnaireType.DEFAULT,
  val resourceIdentifier: String? = null,
  val resourceType: String? = null,
  val confirmationDialog: ConfirmationDialog? = null,
  val groupResource: GroupResourceConfig? = null,
  val taskId: String? = null
) : java.io.Serializable

@Serializable
data class ConfirmationDialog(
  val title: String = "",
  val message: String = "",
  val actionButtonText: String = ""
) : java.io.Serializable

@Serializable
data class GroupResourceConfig(
  val groupIdentifier: String,
  val memberResourceType: String,
  val removeMember: Boolean = false,
  val removeGroup: Boolean = false,
  val deactivateMembers: Boolean = true
) : java.io.Serializable

fun QuestionnaireConfig.interpolate(computedValuesMap: Map<String, Any>) =
  this.copy(
    title = title?.interpolate(computedValuesMap),
    resourceIdentifier = resourceIdentifier?.interpolate(computedValuesMap)?.extractLogicalIdUuid(),
    groupResource =
      groupResource?.copy(
        groupIdentifier =
          groupResource.groupIdentifier.interpolate(computedValuesMap).extractLogicalIdUuid()
      ),
    confirmationDialog =
      confirmationDialog?.copy(
        title = confirmationDialog.title.interpolate(computedValuesMap),
        message = confirmationDialog.message.interpolate(computedValuesMap),
        actionButtonText = confirmationDialog.actionButtonText.interpolate(computedValuesMap)
      )
  )
