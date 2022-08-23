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

package org.smartregister.fhircore.quest.util.extensions

import com.google.android.fhir.logicalId
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.shared.models.ViewComponentEvent

fun List<ActionConfig>.handleClickEvent(
  onViewComponentClick: (ViewComponentEvent) -> Unit,
  resourceData: ResourceData
) {
  val onClickAction = this.find { it.trigger == ActionTrigger.ON_CLICK }
  onClickAction?.let { actionConfig ->
    when (onClickAction.workflow) {
      ApplicationWorkflow.LAUNCH_QUESTIONNAIRE ->
        ViewComponentEvent.LaunchQuestionnaire(actionConfig, resourceData)
      ApplicationWorkflow.LAUNCH_PROFILE -> {
        actionConfig.id?.let {
          ViewComponentEvent.OpenProfile(
            profileId = it,
            resourceId = resourceData.baseResource.logicalId
          )
        }
      }
      else -> null
    }?.run { onViewComponentClick(this) }
  }
}
