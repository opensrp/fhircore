/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.event.EventWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
@Parcelize
data class QuestionnaireConfig(
  val id: String,
  val title: String? = null,
  val saveButtonText: String? = null,
  val planDefinitions: List<String>? = null,
  var type: QuestionnaireType = QuestionnaireType.DEFAULT,
  val resourceIdentifier: String? = null,
  val resourceType: ResourceType? = null,
  val removeResource: Boolean? = null,
  val confirmationDialog: ConfirmationDialog? = null,
  val groupResource: GroupResourceConfig? = null,
  val taskId: String? = null,
  val saveDraft: Boolean = false,
  val snackBarMessage: SnackBarMessageConfig? = null,
  val eventWorkflows: List<EventWorkflow> = emptyList(),
  val readOnlyLinkIds: List<String>? = emptyList(),
  val configRules: List<RuleConfig>? = null,
  val extraParams: List<ActionParameter>? = null,
  val onSubmitActions: List<ActionConfig>? = null,
  val barcodeLinkId: String = "patient-barcode",
) : java.io.Serializable, Parcelable {

  fun interpolate(computedValuesMap: Map<String, Any>) =
    this.copy(
      id = id.interpolate(computedValuesMap).extractLogicalIdUuid(),
      taskId = taskId?.interpolate(computedValuesMap),
      title = title?.interpolate(computedValuesMap),
      resourceIdentifier =
        resourceIdentifier?.interpolate(computedValuesMap)?.extractLogicalIdUuid(),
      groupResource =
        groupResource?.copy(
          groupIdentifier =
            groupResource.groupIdentifier.interpolate(computedValuesMap).extractLogicalIdUuid(),
        ),
      confirmationDialog =
        confirmationDialog?.copy(
          title = confirmationDialog.title.interpolate(computedValuesMap),
          message = confirmationDialog.message.interpolate(computedValuesMap),
          actionButtonText = confirmationDialog.actionButtonText.interpolate(computedValuesMap),
        ),
      planDefinitions = planDefinitions?.map { it.interpolate(computedValuesMap) },
      readOnlyLinkIds = readOnlyLinkIds?.map { it.interpolate(computedValuesMap) },
    )
}

@Serializable
@Parcelize
data class ConfirmationDialog(
  val title: String = "",
  val message: String = "",
  val actionButtonText: String = "",
) : java.io.Serializable, Parcelable

@Serializable
@Parcelize
data class GroupResourceConfig(
  val groupIdentifier: String,
  val memberResourceType: ResourceType,
  val removeMember: Boolean = false,
  val removeGroup: Boolean = false,
  val deactivateMembers: Boolean = true,
) : java.io.Serializable, Parcelable
