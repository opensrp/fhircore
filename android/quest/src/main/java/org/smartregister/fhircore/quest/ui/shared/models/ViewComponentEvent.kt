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

package org.smartregister.fhircore.quest.ui.shared.models

import androidx.navigation.NavController
import com.google.android.fhir.logicalId
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg

/**
 * This sealed class is used to represent various click events of the configurable view components
 */
sealed class ViewComponentEvent {

  /**
   * Event triggered when user clicks a service card to open a profile. Uses [profileId] to fetch
   * the profile configurations and [resourceId] to fetch the data for the current profile.
   */
  data class OpenProfile(val profileId: String, val resourceId: String) : ViewComponentEvent()

  data class LaunchQuestionnaire(val actionConfig: ActionConfig, val resourceData: ResourceData) :
    ViewComponentEvent()

  fun handleEvent(navController: NavController) {
    when (this) {
      is OpenProfile -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(
            NavigationArg.PROFILE_ID to this.profileId,
            NavigationArg.RESOURCE_ID to this.resourceId
          )
        navController.navigate(MainNavigationScreen.Profile.route + urlParams)
      }
      is LaunchQuestionnaire -> {
        actionConfig.questionnaire?.let { questionnaireConfig ->
          val questionnaireType = questionnaireConfig.type
          navController.context.launchQuestionnaire<QuestionnaireActivity>(
            questionnaireId = questionnaireConfig.id,
            clientIdentifier =
              if (questionnaireType == QuestionnaireType.DEFAULT) null
              else resourceData.baseResource.logicalId,
            questionnaireType = questionnaireType,
            intentBundle = actionConfig.paramsBundle(resourceData.computedValuesMap)
          )
        }
      }
    }
  }
}
