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

package org.smartregister.fhircore.quest.util.extensions

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import org.smartregister.fhircore.engine.configuration.interpolate
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.p2p.utils.startP2PScreen

fun List<ActionConfig>.handleClickEvent(
  navController: NavController,
  resourceData: ResourceData? = null,
  navMenu: NavigationMenuConfig? = null
) {
  val onClickAction = this.find { it.trigger == ActionTrigger.ON_CLICK }
  onClickAction?.let { actionConfig ->
    when (onClickAction.workflow) {
      ApplicationWorkflow.LAUNCH_QUESTIONNAIRE -> {
        actionConfig.questionnaire?.let { questionnaireConfig ->
          val questionnaireConfigInterpolated =
            questionnaireConfig.interpolate(resourceData?.computedValuesMap ?: emptyMap())
          val actionParams =
            actionConfig.params.map {
              ActionParameter(
                key = it.key,
                paramType = it.paramType,
                dataType = it.dataType,
                linkId = it.linkId,
                value = it.value.interpolate(resourceData?.computedValuesMap ?: emptyMap())
              )
            }

          if (navController.context is QuestionnaireHandler) {
            (navController.context as QuestionnaireHandler).launchQuestionnaire<Any>(
              context = navController.context,
              questionnaireConfig = questionnaireConfigInterpolated,
              actionParams = actionParams
            )
          }
        }
      }
      ApplicationWorkflow.LAUNCH_PROFILE -> {
        actionConfig.id?.let { id ->
          val args =
            bundleOf(
              NavigationArg.PROFILE_ID to id,
              NavigationArg.RESOURCE_ID to resourceData?.baseResourceId,
              NavigationArg.RESOURCE_CONFIG to actionConfig.resourceConfig
            )
          navController.navigate(MainNavigationScreen.Profile.route, args)
        }
      }
      ApplicationWorkflow.LAUNCH_REGISTER -> {
        val args =
          bundleOf(
            Pair(NavigationArg.REGISTER_ID, actionConfig.id ?: navMenu?.id),
            Pair(
              NavigationArg.SCREEN_TITLE,
              resourceData?.let { actionConfig.display(it.computedValuesMap) } ?: navMenu?.display
            ),
            Pair(NavigationArg.TOOL_BAR_HOME_NAVIGATION, actionConfig.toolBarHomeNavigation),
          )

        // Register is the entry point destination, clear back stack with every register switch
        navController.navigate(
          resId = MainNavigationScreen.Home.route,
          args = args,
          navOptions = navOptions(MainNavigationScreen.Home.route),
        )
      }
      ApplicationWorkflow.LAUNCH_REPORT -> {
        val args = bundleOf(Pair(NavigationArg.REPORT_ID, actionConfig.id))
        navController.navigate(MainNavigationScreen.Reports.route, args)
      }
      ApplicationWorkflow.LAUNCH_SETTINGS ->
        navController.navigate(MainNavigationScreen.Settings.route)
      ApplicationWorkflow.DEVICE_TO_DEVICE_SYNC -> startP2PScreen(navController.context)
      ApplicationWorkflow.LAUNCH_MAP ->
        navController.navigate(
          MainNavigationScreen.GeoWidget.route,
          bundleOf(NavigationArg.CONFIG_ID to actionConfig.id)
        )
      else -> return
    }
  }
}

/**
 * Apply navigation options. Restrict destination to only use a single instance in the back stack.
 */
fun navOptions(resId: Int, inclusive: Boolean = false, singleOnTop: Boolean = true) =
  NavOptions.Builder().setPopUpTo(resId, true, inclusive).setLaunchSingleTop(singleOnTop).build()

fun ViewProperties.clickable(ResourceData: ResourceData) =
  this.clickable.interpolate(ResourceData.computedValuesMap).toBoolean()
