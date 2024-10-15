/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.ResourceFilterExpression
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.domain.model.SortConfig
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
@Parcelize
data class QuestionnaireConfig(
  val id: String,
  val title: String? = null,
  val saveButtonText: String? = null,
  val planDefinitions: List<String>? = null,
  var type: String = QuestionnaireType.DEFAULT.name,
  val resourceIdentifier: String? = null,
  val resourceType: ResourceType? = null,
  val removeResource: Boolean? = null,
  val confirmationDialog: ConfirmationDialog? = null,
  val groupResource: GroupResourceConfig? = null,
  val taskId: String? = null,
  val encounterId: String? = null,
  val saveDraft: Boolean = false,
  val snackBarMessage: SnackBarMessageConfig? = null,
  val eventWorkflows: List<EventWorkflow> = emptyList(),
  val readOnlyLinkIds: List<String>? = emptyList(),
  val configRules: List<RuleConfig>? = null,
  val extraParams: List<ActionParameter>? = null,
  val onSubmitActions: List<ActionConfig>? = null,
  val barcodeLinkId: String? = "patient-barcode",
  val extractedResourceUniquePropertyExpressions: List<ExtractedResourceUniquePropertyExpression>? =
    null,
  val saveQuestionnaireResponse: Boolean = true,
  val generateCarePlanWithWorkflowApi: Boolean = false,
  val cqlInputResources: List<String>? = emptyList(),
  val showClearAll: Boolean = false,
  val showRequiredTextAsterisk: Boolean = true,
  val showRequiredText: Boolean = false,
  val managingEntityRelationshipCode: String? = null,
  val uniqueIdAssignment: UniqueIdAssignmentConfig? = null,
  val linkIds: List<LinkIdConfig>? = null,
  val showSubmitAnywayButton: String = "false",
) : java.io.Serializable, Parcelable {

  fun interpolate(computedValuesMap: Map<String, Any>) =
    this.copy(
      id = id.interpolate(computedValuesMap).extractLogicalIdUuid(),
      taskId = taskId?.interpolate(computedValuesMap),
      encounterId = encounterId?.interpolate(computedValuesMap),
      title = title?.interpolate(computedValuesMap),
      type = type.interpolate(computedValuesMap),
      managingEntityRelationshipCode =
        managingEntityRelationshipCode?.interpolate(computedValuesMap),
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
      extraParams = extraParams?.map { it.interpolate(computedValuesMap) },
      onSubmitActions = onSubmitActions?.map { it.interpolate(computedValuesMap) },
      barcodeLinkId = barcodeLinkId?.interpolate(computedValuesMap),
      cqlInputResources = cqlInputResources?.map { it.interpolate(computedValuesMap) },
      uniqueIdAssignment =
        uniqueIdAssignment?.copy(linkId = uniqueIdAssignment.linkId.interpolate(computedValuesMap)),
      linkIds = linkIds?.onEach { it.linkId.interpolate(computedValuesMap) },
      saveButtonText = saveButtonText?.interpolate(computedValuesMap),
      showSubmitAnywayButton = showSubmitAnywayButton.interpolate(computedValuesMap),
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

@Serializable
@Parcelize
data class ExtractedResourceUniquePropertyExpression(
  val resourceType: ResourceType,
  val fhirPathExpression: String,
) : java.io.Serializable, Parcelable

@Serializable
@Parcelize
/**
 * @property linkId The linkId used to capture the OpenSRP unique ID in the Questionnaire. Typically
 *   a GUID.
 * @property idFhirPathExpression The FHIR Path expression for extracting ID from the configured
 *   [resource]
 * @property resource The type of resource used to store generated IDs
 * @property readOnly Whether to disable/enable editing of link ID.
 * @property dataQueries The queries used to filter resources used for representing OpenSRP unique
 *   IDs.
 * @property sortConfigs Configuration for sorting resources
 * @property resourceFilterExpression Expression used to filter the returned resources
 */
data class UniqueIdAssignmentConfig(
  val linkId: String,
  val idFhirPathExpression: String,
  val readOnly: Boolean = true,
  val resource: ResourceType,
  val dataQueries: List<DataQuery> = emptyList(),
  val sortConfigs: List<SortConfig>? = null,
  val resourceFilterExpression: ResourceFilterExpression? = null,
) : java.io.Serializable, Parcelable

@Serializable
@Parcelize
data class LinkIdConfig(
  val linkId: String,
  val type: LinkIdType,
) : java.io.Serializable, Parcelable

@Serializable
@Parcelize
enum class LinkIdType : Parcelable {
  READ_ONLY,
  BARCODE,
  LOCATION,
  PREPOPULATION_EXCLUSION,
}
