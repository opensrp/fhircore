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

package org.smartregister.fhircore.engine.domain.model

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.PdfConfig
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
@Parcelize
data class ActionConfig(
  val trigger: ActionTrigger,
  val workflow: String? = null,
  val id: String? = null,
  val display: String? = null,
  val rules: List<RuleConfig> = emptyList(),
  val questionnaire: QuestionnaireConfig? = null,
  val managingEntity: ManagingEntityConfig? = null,
  val params: List<ActionParameter> = emptyList(),
  val resourceConfig: FhirResourceConfig? = null,
  val toolBarHomeNavigation: ToolBarHomeNavigation = ToolBarHomeNavigation.OPEN_DRAWER,
  val popNavigationBackStack: Boolean? = null,
  val multiSelectViewConfig: MultiSelectViewConfig? = null,
  val pdfConfig: PdfConfig? = null,
) : Parcelable, java.io.Serializable {
  fun paramsBundle(computedValuesMap: Map<String, Any> = emptyMap()): Bundle =
    Bundle().apply {
      params
        .filter { !it.paramType?.name.equals(PREPOPULATE_PARAM_TYPE) }
        .forEach { putString(it.key, it.value.interpolate(computedValuesMap)) }
    }

  fun interpolate(computedValuesMap: Map<String, Any>): ActionConfig =
    this.copy(
      id = id?.interpolate(computedValuesMap),
      workflow = workflow?.interpolate(computedValuesMap),
      display = display?.interpolate(computedValuesMap),
      managingEntity =
        managingEntity?.copy(
          dialogTitle = managingEntity.dialogTitle?.interpolate(computedValuesMap),
          dialogWarningMessage =
            managingEntity.dialogWarningMessage?.interpolate(computedValuesMap),
          dialogContentMessage =
            managingEntity.dialogContentMessage?.interpolate(computedValuesMap),
          noMembersErrorMessage =
            managingEntity.noMembersErrorMessage.interpolate(computedValuesMap),
          managingEntityReassignedMessage =
            managingEntity.managingEntityReassignedMessage.interpolate(computedValuesMap),
        ),
      params = params.map { it.interpolate(computedValuesMap) },
    )

  companion object {
    const val PREPOPULATE_PARAM_TYPE = "PREPOPULATE"
  }
}

@Parcelize
@Serializable
data class ActionParameter(
  val key: String,
  val paramType: ActionParameterType? = null,
  val dataType: Enumerations.DataType? = null,
  val value: String,
  val linkId: String? = null,
  val resourceType: ResourceType? = null,
) : Parcelable, java.io.Serializable {

  fun interpolate(computedValuesMap: Map<String, Any>) =
    this.copy(
      value = value.interpolate(computedValuesMap),
      key = key.interpolate(computedValuesMap),
      linkId = linkId?.interpolate(computedValuesMap),
    )
}
